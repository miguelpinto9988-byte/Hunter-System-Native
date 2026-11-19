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
    val handSupport: Boolean = false
) {
    fun at(phase: Float): List<Offset> {
        if (staticHold) return frames.first()
        val wrap = phase - floor(phase)
        val previous = times.indexOfLast { it <= wrap }.coerceAtLeast(0)
        val next = (previous + 1) % frames.size
        val interval = (times.getOrNull(previous + 1) ?: 1f) - times[previous]
        val fraction = if (interval > 0f) ((wrap - times[previous]) / interval).coerceIn(0f,1f) else 0f
        if (next == 0 && source == "sport") return frames[previous]
        return frames[previous].zip(frames[next]) { a,b -> a + (b - a) * fraction }
    }
}

internal object ExerciseMotions {
    private val idPattern = Regex("[A-Za-z0-9_-]{1,100}")
    private var legacy: JSONObject? = null
    private var sports: JSONObject? = null

    fun load(context: Context, exerciseId: String): ExerciseMotion? {
        if (!idPattern.matches(exerciseId)) return null
        return runCatching {
            when {
                exerciseId.startsWith("hx_") -> {
                    val records = legacy ?: read(context,"hunter2000-poses.json").also { legacy = it }
                    parse(records.getJSONObject(exerciseId),"historical")
                }
                exerciseId.startsWith("sport-") -> {
                    val records = sports ?: read(context,"exercise-actor/sports-poses.json").also { sports = it }
                    parse(records.getJSONObject(exerciseId),"sport")
                }
                else -> parse(read(context,"demos30/$exerciseId.json"),"original")
            }
        }.getOrNull()
    }

    fun loadSequence(context: Context, exercise: Exercise): List<ExerciseMotion>? {
        val ids = exercise.raw.a("sequenceParts").strings()
        if (ids.size != 2 || ids[0] == ids[1]) return null
        return ids.map { load(context, it) ?: return null }
    }

    private fun read(context: Context, file: String) =
        context.assets.open(file).bufferedReader().use { JSONObject(it.readText()) }

    internal fun parse(json: JSONObject, source: String): ExerciseMotion {
        val native = source == "original"
        val data = json.getJSONArray(if (native) "frames" else "poses")
        require(data.length() in 2..240)
        val frames = List(data.length()) { i ->
            val frame = if (native) data.getJSONObject(i).getJSONArray("p") else data.getJSONArray(i)
            require(frame.length() == 11)
            List(11) { j ->
                val v = frame.getJSONArray(j)
                Offset(v.getDouble(0).toFloat(),v.getDouble(1).toFloat()).also {
                    require(it.x.isFinite() && it.y.isFinite() && it.x in -2000f..2000f && it.y in -2000f..2000f)
                }
            }
        }
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
        val view = when (camera?.optString("primary")) {
            "front" -> ExerciseView.FRONT
            "top", "topoblique" -> ExerciseView.OVERHEAD
            "rear" -> ExerciseView.REAR
            else -> ExerciseView.SIDE
        }
        val prone = json.optString("posture23") == "prone"
        val handSupport = prone && (view == ExerciseView.OVERHEAD || frames.any { frame ->
            val lower = maxOf(frame[7].y, frame[8].y, frame[9].y, frame[10].y)
            val torso = (frame[2] - frame[1]).getDistance()
            maxOf(frame[4].y, frame[6].y) >= lower - .12f * torso &&
                frame[0].y + .20f * torso < maxOf(frame[4].y, frame[6].y)
        })
        return ExerciseMotion(
            frames, duration, times, source, json.optString("facing23", "right"),
            json.optBoolean("hold"), json.optString("equipment"), props, view, prone, handSupport
        )
    }
}
