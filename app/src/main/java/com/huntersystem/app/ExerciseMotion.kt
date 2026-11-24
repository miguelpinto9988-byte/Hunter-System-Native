package com.huntersystem.app

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.json.JSONObject
import kotlin.math.floor

internal enum class ExerciseView { FRONT, SIDE, OVERHEAD, REAR }

internal data class ExerciseProp(val kind: String, val bounds: Rect)

internal data class ExerciseMotion(
    val frames: List<List<Offset>>,
    val duration: Float,
    val times: List<Float>,
    val source: String,
    val facing: String,
    val staticHold: Boolean = false,
    val equipment: String = "",
    val props: List<ExerciseProp> = emptyList(),
    val view: ExerciseView = ExerciseView.SIDE,
    val prone: Boolean = false,
    val handSupport: Boolean = false,
    val posture: ExercisePosture = if (prone) ExercisePosture.PRONE else ExercisePosture.UPRIGHT,
    val authoredFloor: AuthoredFloorMovement? = null,
    val toeLift: ToeLiftMotion? = null,
    val handGesture: HandGesture? = null,
    val footGesture: FootGesture? = null,
    val apparatus: List<DemoApparatus> = emptyList(),
    val floorTransition: Boolean = false,
    val suspended: Boolean = false
) {
    val firstFootOnLeft: Boolean by lazy { frames.sumOf { (it[8].x-it[10].x).toDouble() } <= 0.0 }
    private val ankleFloor: List<Float> by lazy { listOf(8,10).map { index -> frames.maxOf { it[index].y } } }

    fun ankleTouchesFloor(ankle: Offset, first: Boolean, torsoLength: Float): Boolean =
        ankle.y >= ankleFloor[if(first) 0 else 1] - torsoLength * .035f

    val needsPlacementGuide: Boolean by lazy {
        val first = frames.first()
        val torso = (first[2] - first[1]).getDistance().coerceAtLeast(1f)
        val nearlyStill = !staticHold && frames.all { pose ->
            pose.indices.all { (pose[it] - first[it]).getDistance() < torso * .10f }
        }
        nearlyStill || frames.any { pose ->
            listOf(3 to 4,5 to 6,7 to 8,9 to 10).any { (joint,end) ->
                (pose[joint]-pose[end]).getDistance() < torso * .035f
            }
        }
    }

    fun at(phase: Float): List<Offset> {
        if (staticHold) return frames.first()
        val wrap = phase - floor(phase)
        val previous = times.indexOfLast { it <= wrap }.coerceAtLeast(0)
        val next = (previous + 1) % frames.size
        val interval = (times.getOrNull(previous + 1) ?: 1f) - times[previous]
        val fraction = if (interval > 0f) ((wrap - times[previous]) / interval).coerceIn(0f,1f) else 0f
        return ExercisePoseCommands.interpolate(frames[previous], frames[next], fraction)
    }
}

internal object ExerciseMotions {
    private val idPattern = Regex("[A-Za-z0-9_-]{1,100}")
    private var legacy: JSONObject? = null
    private var sports: JSONObject? = null

    fun load(context: Context, exerciseId: String): ExerciseMotion? =
        loadAsset(exerciseId) { read(context, it) }

    private fun loadAsset(exerciseId: String, readAsset: (String) -> JSONObject): ExerciseMotion? {
        if (!idPattern.matches(exerciseId)) return null
        return runCatching {
            when {
                exerciseId.startsWith("hx_") -> {
                    val records = legacy ?: readAsset("hunter2000-poses.json").also { legacy = it }
                    parse(records.getJSONObject(exerciseId),"historical",exerciseId)
                }
                exerciseId.startsWith("sport-") -> {
                    val records = sports ?: readAsset("exercise-actor/sports-poses.json").also { sports = it }
                    parse(records.getJSONObject(exerciseId),"sport",exerciseId)
                }
                else -> parse(readAsset("demos30/$exerciseId.json"),"original",exerciseId)
            }
        }.getOrNull()
    }

    fun load(context: Context, exercise: Exercise): ExerciseMotion? =
        resolve(exercise) { read(context, it) }

    fun resolve(exercise: Exercise, readAsset: (String) -> JSONObject): ExerciseMotion? {
        RingExerciseMotions.load(exercise.id)?.let { return it }
        if (CoreExerciseClips.hasInconsistentInstructions(exercise)) return null
        val motion = CoreExerciseClips.load(exercise) ?: LegacySingleExerciseMotions.load(exercise.id) { loadAsset(it,readAsset) }
            ?: loadAsset(exercise.raw.s("motionReference",exercise.id),readAsset)
        return motion?.withApparatus(exercise)
    }

    fun loadSequence(context: Context, exercise: Exercise, catalog: Catalog? = null): List<ExerciseMotion>? =
        loadClips(context, exercise, catalog)?.map(ExerciseClip::motion)

    fun loadClips(context: Context, exercise: Exercise, catalog: Catalog? = null): List<ExerciseClip>? =
        resolveClips(exercise, catalog) { read(context, it) }

    fun resolveClips(exercise: Exercise, catalog: Catalog?, readAsset: (String) -> JSONObject): List<ExerciseClip>? {
        val ids = exercise.raw.a("sequenceParts").strings()
        if (ids.isEmpty()) return LegacyExerciseClips.load(exercise) { loadAsset(it,readAsset) }
        if (ids.size != 2 || ids.distinct().size != ids.size) return null
        val labels = exercise.raw.a("sequenceLabels").strings()
        return ids.mapIndexed { index, id ->
            val motion = loadAsset(id,readAsset) ?: return null
            val component = catalog?.byId?.get(id)
            ExerciseClip(labels.getOrNull(index) ?: component?.name ?: "Gesto ${index + 1}", id,
                component?.let { motion.withApparatus(it) } ?: motion)
        }
    }

    private fun ExerciseMotion.withApparatus(exercise: Exercise) =
        copy(apparatus = ExerciseEquipmentPlan.forExercise(exercise, this))

    private fun read(context: Context, file: String) =
        context.assets.open(file).bufferedReader().use { JSONObject(it.readText()) }

    internal fun parse(json: JSONObject, source: String, exerciseId: String = ""): ExerciseMotion {
        val native = source == "original"
        val data = json.getJSONArray(if (native) "frames" else "poses")
        require(data.length() in 2..240)
        val decoded = List(data.length()) { i ->
            val frame = if (native) data.getJSONObject(i).getJSONArray("p") else data.getJSONArray(i)
            require(frame.length() == 11)
            List(11) { j ->
                val v = frame.getJSONArray(j)
                Offset(v.getDouble(0).toFloat(),v.getDouble(1).toFloat()).also {
                    require(it.x.isFinite() && it.y.isFinite() && it.x in -2000f..2000f && it.y in -2000f..2000f)
                }
            }
        }
        AuthoredFloorMovement.forId(exerciseId)?.let { return it.render(exerciseId,decoded.size) }
        val frames = ExercisePoseCommands.apply(exerciseId, decoded)
        val keyTimes = json.optJSONArray("times")
        val times = if (keyTimes == null) frames.indices.map { it.toFloat() / frames.size }
        else List(keyTimes.length()) { keyTimes.getDouble(it).toFloat() }
        require(times.size == frames.size && times.first() == 0f &&
            times.last() <= 1f && times.zipWithNext().all { (a,b) -> a < b })
        val duration = json.optDouble("duration", json.optDouble("duration46", 4.8)).toFloat()
        val equipment = if (native) data.getJSONObject(0).optJSONArray("equipment") else null
        val props = (0 until (equipment?.length() ?: 0)).mapNotNull { index ->
            val entry = equipment!!.getJSONArray(index)
            val kind = entry.getString(0)
            if (kind == "bounds23") return@mapNotNull null
            val bounds = entry.getJSONArray(1)
            ExerciseProp(kind, Rect(bounds.getDouble(0).toFloat(), bounds.getDouble(1).toFloat(),
                bounds.getDouble(2).toFloat(), bounds.getDouble(3).toFloat()))
        }
        require(duration in .5f..30f)
        val camera = json.optJSONObject("view21")
        val declaredView = when (camera?.optString("primary")) {
            "front" -> ExerciseView.FRONT
            "top", "topoblique" -> ExerciseView.OVERHEAD
            "rear" -> ExerciseView.REAR
            else -> ExerciseView.SIDE
        }
        val horizontal = frames.all {
            val torso = it[2]-it[1]
            kotlin.math.abs(torso.y) < kotlin.math.abs(torso.x)*.4f
        }
        // These legacy "front" tracks are stored on their sides, already projected in the floor plane.
        val view = if (horizontal && declaredView==ExerciseView.FRONT) ExerciseView.SIDE else declaredView
        val floorPosture = when(exerciseId) {
            "new46-pelvicclock","new46-foampec" -> "supine"
            "new46-copenhagen","new46-copenhagendyn","new46-sideadduct","new46-sidecrow",
            "new46-sleeper","new46-sidepallof" -> "side-lying"
            "new46-sitthrough","new46-adductorrock","new46-sliderfly" -> "prone"
            else -> json.optString("posture23")
        }
        val posture = ExercisePoseCommands.posture(floorPosture, view, frames)
        val prone = posture == ExercisePosture.PRONE
        val floorWithoutPalms = camera?.optString("family") in setOf("prone","reverse","contralateral","w-t19","y-t-w19") ||
            exerciseId in setOf("w-t19","y-t-w19")
        val handSupport = prone && !floorWithoutPalms && (view == ExerciseView.OVERHEAD || frames.any { frame ->
            val lower = maxOf(frame[7].y, frame[8].y, frame[9].y, frame[10].y)
            val torso = (frame[2] - frame[1]).getDistance()
            maxOf(frame[4].y, frame[6].y) >= lower - .12f * torso &&
                frame[0].y + .20f * torso < maxOf(frame[4].y, frame[6].y)
        })
        val toeLift = ToeLiftMotion.forId(exerciseId)
        val drawnEquipment = when (exerciseId) {
            "gym-front-bb23", "gym-front-bb23-tempo23", "gym-front-bb23-pause23" -> "barbell"
            "gym-tibialis30", "gym-tibialis30-ecc30", "gym-tibialis30-hold30" -> "tibialis-machine"
            "doublefront", "doublefront_slow", "doublefront_pause" -> "dumbbells"
            "goblet", "goblet_slow", "goblet_pause", "goblet-15-19", "goblet-control18", "goblethold" -> "goblet-weight"
            else -> json.optString("equipment")
        }
        return ExerciseMotion(
            frames, toeLift?.duration ?: duration, times, source, json.optString("facing23", "right"),
            json.optBoolean("hold") || StaticExercisePoses.contains(exerciseId), drawnEquipment, props, view, prone, handSupport, posture,
            toeLift = toeLift, handGesture = HandGesture.forId(exerciseId), footGesture = FootGesture.forId(exerciseId)
        )
    }
}

private object StaticExercisePoses {
    private val ids = setOf("hs379-palms", "hs379-isobiceps", "new46-lsit", "new46-tucklever",
        "new46-tricepsstretch", "new46-ringturnout")
    fun contains(id: String) = id in ids
}
