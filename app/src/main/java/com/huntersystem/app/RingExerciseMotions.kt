package com.huntersystem.app

import androidx.compose.ui.geometry.Offset

/** Side-view poses for the three ring progressions without an existing clip. */
internal object RingExerciseMotions {
    private data class RingPose(
        val shoulder: Offset,
        val hip: Offset,
        val elbow: Offset,
        val wrist: Offset,
        val knee: Offset,
        val ankle: Offset
    ) {
        fun points(): List<Offset> {
            val neck = shoulder + (shoulder - hip) * .29f + Offset(8f, 0f)
            // Slight vertical separation gives both limbs a visible silhouette from the side.
            val far = Offset(-12f, -7f)
            return listOf(neck, shoulder, hip, elbow, wrist, elbow + far, wrist + far,
                knee, ankle, knee + far, ankle + far)
        }
    }

    private fun p(sx: Float, sy: Float, hx: Float, hy: Float, ex: Float, ey: Float,
                  wx: Float, wy: Float, kx: Float, ky: Float, ax: Float, ay: Float) =
        RingPose(Offset(sx, sy), Offset(hx, hy), Offset(ex, ey), Offset(wx, wy),
            Offset(kx, ky), Offset(ax, ay))

    private val grip = p(-18f, 0f, -28f, 162f, -25f, 99f, 35f, 51f, -3f, 289f, 23f, 409f)
    private val lowStart = p(-64f, 17f, -104f, 172f, -26f, 102f, 32f, 62f, -29f, 290f, 38f, 397f)
    private val lowPull = p(-22f, -22f, -79f, 129f, -52f, 81f, 31f, 56f, 7f, 248f, 38f, 397f)
    private val lowCatch = p(69f, -22f, 2f, 126f, -17f, 28f, 28f, 55f, 55f, 253f, 38f, 397f)
    private val hang = p(-5f, 62f, -6f, 225f, 0f, -42f, 14f, -132f, 1f, 359f, -4f, 480f)
    private val pull = p(-26f, -117f, -41f, 42f, -66f, -32f, 22f, -122f, -30f, 178f, -23f, 300f)
    private val turnover = p(62f, -199f, 5f, -48f, -30f, -148f, 17f, -113f, 22f, 88f, 12f, 208f)
    private val support = p(24f, -292f, 18f, -128f, 17f, -193f, 13f, -109f, 16f, 9f, 6f, 131f)

    fun load(id: String): ExerciseMotion? {
        val keyframes = when (id) {
            "performance-ring-false-grip" -> listOf(0f to grip, .5f to grip)
            "performance-ring-transition" -> listOf(0f to lowStart, .24f to lowPull, .47f to lowCatch,
                .58f to lowCatch, .79f to lowPull, .96f to lowStart)
            "performance-strict-ring-muscle-up" -> listOf(0f to hang, .25f to pull,
                .39f to turnover, .51f to support, .60f to support, .72f to turnover,
                .83f to pull, .97f to hang)
            else -> return null
        }
        return ExerciseMotion(
            keyframes.map { it.second.points() },
            if (id == "performance-strict-ring-muscle-up") 7.2f else 5.2f,
            keyframes.map { it.first }, "ring-authored", "right",
            staticHold = id == "performance-ring-false-grip",
            apparatus = listOf(DemoApparatus(DemoGear.RINGS, GearMount.BOTH)),
            suspended = id == "performance-strict-ring-muscle-up"
        )
    }
}
