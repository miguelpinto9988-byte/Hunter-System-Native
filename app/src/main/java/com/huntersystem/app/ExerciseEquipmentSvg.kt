package com.huntersystem.app

import android.content.Context
import android.graphics.Picture
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.caverock.androidsvg.SVG

internal object EquipmentSvg {
    private const val WIDTH = 100
    private const val HEIGHT = 64

    fun load(context: Context, asset: String): Picture? = runCatching {
        context.assets.open("exercise-actor/equipment/$asset.svg").use {
            SVG.getFromInputStream(it).renderToPicture(WIDTH,HEIGHT)
        }
    }.getOrNull()
}

internal fun equipmentLabels(motion: ExerciseMotion): List<String> =
    (motion.apparatus.map {
        if(it.gear==DemoGear.DUMBBELL && it.mount==GearMount.BOTH) "Mancuernas" else it.label
    } + motion.props.mapNotNull { ExerciseEquipmentPlan.forProp(it.kind) }
        .filterNot { prop -> prop in setOf(DemoGear.CHAIR,DemoGear.BENCH) &&
            motion.apparatus.any { it.gear in ExerciseEquipmentPlan.cardioMachines } }
        .map { it.label })
        .distinct()

internal fun DrawScope.drawExerciseEquipment(
    frame: EquipmentFrame,
    supports: List<ExerciseProp>,
    pictures: Map<String, Picture?>,
    unit: Float
) {
    for (prop in supports) {
        val asset = ExerciseEquipmentPlan.forProp(prop.kind)?.asset ?: continue
        val bounds = prop.bounds
        // The original seat coordinates specify the contact plane, below the surface of the upper cushion.
        val top = if (prop.kind == "chair") bounds.top - bounds.height*.40f else bounds.top
        val adjusted = bounds.copy(top=top)
        drawEquipmentSprite(EquipmentSprite(asset,adjusted.center,adjusted.size),pictures[asset])
    }
    for (link in frame.links) {
        val color = when (link.stroke) {
            EquipmentStroke.BAND -> Color(0xFFEFB571)
            EquipmentStroke.ROPE -> Color(0xFFDAAE77)
            EquipmentStroke.CABLE -> Color(0xFFC2D6DB)
            EquipmentStroke.STEEL -> Color(0xFF789BAA)
        }
        val width = link.stroke.width*unit
        val bend = link.bend
        if(bend==null) drawLine(color,link.start,link.end,width,StrokeCap.Round)
        else drawPath(Path().apply {
            moveTo(link.start.x,link.start.y)
            quadraticTo(bend.x,bend.y,link.end.x,link.end.y)
        },color,style=Stroke(width,cap=StrokeCap.Round))
    }
    for (sprite in frame.sprites) {
        if(!sprite.foreground) drawEquipmentSprite(sprite,pictures[sprite.asset])
    }
}

internal fun DrawScope.drawEquipmentSprite(sprite: EquipmentSprite, picture: Picture?) {
    if (picture == null || sprite.size.minDimension<=0f) return
    drawIntoCanvas { projected ->
        val canvas = projected.nativeCanvas
        canvas.save()
        canvas.translate(sprite.center.x,sprite.center.y)
        canvas.rotate(sprite.angle)
        canvas.scale(sprite.size.width/picture.width,sprite.size.height/picture.height)
        canvas.translate(-picture.width*.5f,-picture.height*.5f)
        canvas.drawPicture(picture)
        canvas.restore()
    }
}
