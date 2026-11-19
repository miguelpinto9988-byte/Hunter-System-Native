package com.huntersystem.app

import android.content.Context
import androidx.compose.ui.geometry.Offset
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.floor

internal data class ExerciseMotion(
    val frames: List<List<Offset>>,
    val duration: Float,
    val times: List<Float>,
    val source: String,
    val facing: String,
    val staticHold: Boolean = false,
    val equipment: String = ""
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
        val duration = json.optDouble("duration",4.8).toFloat()
        require(duration in .5f..30f)
        return ExerciseMotion(frames,duration,times,source,json.optString("facing23",
            json.optJSONObject("view21")?.optString("primary","right") ?: "right"),
            json.optBoolean("hold"),json.optString("equipment"))
    }
}
