package com.huntersystem.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/** The stage uses one scale and one framing rectangle for the whole exercise. */
internal data class ExerciseFraming(
    val bounds: Rect,
    val unit: Float,
    val floor: Float,
    val equipment: ExerciseEquipmentScene
) {
    fun scaleFor(stage: Size, closeFraming: Boolean = false): Float {
        val fit = minOf(stage.width / bounds.width, stage.height / bounds.height) * .92f
        // Compact or foreshortened poses leave room around the athlete instead of zooming in.
        val torsoShare = if (closeFraming) .42f else .30f
        val torsoLimit = stage.minDimension * torsoShare / (165f * unit)
        return minOf(fit, torsoLimit)
    }

    companion object {
        fun from(motion: ExerciseMotion): ExerciseFraming {
            val torsoLengths = motion.frames.map { (it[2] - it[1]).getDistance() }.sorted()
            val unit = torsoLengths[torsoLengths.size / 2].coerceAtLeast(35f) / 165f
            val points = motion.frames.flatten()
            val left = minOf(points.minOf { it.x },
                motion.props.minOfOrNull { it.bounds.left } ?: Float.POSITIVE_INFINITY)
            val top = minOf(points.minOf { it.y },
                motion.props.minOfOrNull { it.bounds.top } ?: Float.POSITIVE_INFINITY)
            val right = maxOf(points.maxOf { it.x },
                motion.props.maxOfOrNull { it.bounds.right } ?: Float.NEGATIVE_INFINITY)
            val bottom = maxOf(points.maxOf { it.y },
                motion.props.maxOfOrNull { it.bounds.bottom } ?: Float.NEGATIVE_INFINITY)
            val equipment = ExerciseEquipmentScene(motion, Rect(left,top,right,bottom), unit)
            val scene = equipment.bounds()
            return ExerciseFraming(
                Rect(
                    Offset(scene.left - 84f * unit, scene.top - 70f * unit),
                    Offset(scene.right + 84f * unit, scene.bottom + 70f * unit)
                ),
                unit,
                bottom,
                equipment
            )
        }
    }
}
