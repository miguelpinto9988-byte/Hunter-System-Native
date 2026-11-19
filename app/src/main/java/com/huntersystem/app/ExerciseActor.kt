package com.huntersystem.app

import android.graphics.Picture
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

private val teal = Color(0xFF4D9599)
private val tealLight = Color(0xFF76ABAA)
private val navy = Color(0xFF29475D)
private val navyDark = Color(0xFF20364B)
private val skin = Color(0xFFE9B597)
private val skinFar = Color(0xFFBD8975)
private val shoe = Color(0xFF294456)
private val sole = Color(0xFFD5E6E3)

@Composable
internal fun ExerciseActor(motion: ExerciseMotion, modifier: Modifier, play: Boolean,
                           mirror: Boolean, reset: Int) {
    val context = LocalContext.current
    val headAsset = when (motion.view) {
        ExerciseView.FRONT -> "rostro-frontal.svg"
        ExerciseView.OVERHEAD, ExerciseView.REAR -> "rostro-superior.svg"
        ExerciseView.SIDE -> "rostro.svg"
    }
    val face = remember(context, headAsset) {
        runCatching {
            context.assets.open("exercise-actor/$headAsset").use {
                SVG.getFromInputStream(it).renderToPicture(69,82)
            }
        }.getOrNull()
    }
    val reduced = LocalMonarchMotion.current.reduced
    var phase by remember(motion,reset) { mutableFloatStateOf(0f) }
    LaunchedEffect(motion,play,reduced,reset) {
        if (!play || reduced || motion.staticHold) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (isActive) {
            val next = withFrameNanos { it }
            phase = (phase + (next - previous).coerceAtMost(80_000_000L) /
                (motion.duration * 1_000_000_000f)) % 1f
            previous = next
        }
    }
    val minX = remember(motion) { min(motion.frames.minOf { p -> p.minOf(Offset::x) },
        motion.props.minOfOrNull { it.bounds.left } ?: Float.POSITIVE_INFINITY) }
    val maxX = remember(motion) { max(motion.frames.maxOf { p -> p.maxOf(Offset::x) },
        motion.props.maxOfOrNull { it.bounds.right } ?: Float.NEGATIVE_INFINITY) }
    val minY = remember(motion) { min(motion.frames.minOf { p -> p.minOf(Offset::y) },
        motion.props.minOfOrNull { it.bounds.top } ?: Float.POSITIVE_INFINITY) }
    val maxY = remember(motion) { max(motion.frames.maxOf { p -> p.maxOf(Offset::y) },
        motion.props.maxOfOrNull { it.bounds.bottom } ?: Float.NEGATIVE_INFINITY) }
    val scaleUnit = remember(motion) {
        motion.frames.map { (it[2] - it[1]).getDistance() }.sorted().let { it[it.size / 2] }
            .coerceAtLeast(35f) / 165f
    }
    Canvas(modifier) {
        drawRoundRect(Color(0xFF101F2A),cornerRadius=CornerRadius(18f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFF22464D),Color(0xFF142C3A),Color(0xFF101F2A)),
            center=Offset(size.width*.48f,size.height*.47f),radius=max(size.width,size.height)*.51f),
            radius=min(size.width,size.height)*.47f,center=Offset(size.width*.5f,size.height*.47f))
        val width = maxX - minX + 168f * scaleUnit
        val height = maxY - minY + 140f * scaleUnit
        val fit = min(size.width / width,size.height / height) * .92f
        val origin = Offset((minX+maxX)/2,(minY+maxY)/2)
        val p = motion.at(phase)
        withTransform({
            translate(size.width/2,size.height/2)
            scale(if(mirror) -fit else fit,fit,pivot=Offset.Zero)
            translate(-origin.x,-origin.y)
        }) {
            drawStage(motion, p, origin, width, height, scaleUnit, maxY)
            motion.props.forEach { drawProp(it, scaleUnit) }
            drawEquipment(motion.equipment,p,scaleUnit,maxY)
            drawActor(p, scaleUnit, motion, face)
        }
    }
}

private fun DrawScope.drawStage(
    motion: ExerciseMotion, pose: List<Offset>, center: Offset,
    width: Float, height: Float, unit: Float, bottom: Float
) {
    val ink = Color(0xFF47606E)
    if (motion.view == ExerciseView.OVERHEAD) {
        val matWidth = width * .83f
        val matHeight = height * .88f
        val corner = center - Offset(matWidth / 2, matHeight / 2)
        val size = Size(matWidth, matHeight)
        val radius = CornerRadius(unit * 23f)
        drawRoundRect(Color(0xFF254651), corner, size, radius)
        drawRoundRect(ink, corner, size, radius, style = Stroke(unit * 1.5f))
        if (motion.handSupport) {
            for (i in listOf(4, 6, 8, 10)) {
                drawCircle(Color(0xFF17343F), unit * 20f, pose[i])
            }
        }
    } else {
        val floor = bottom + (if (motion.handSupport) 6f else 16f) * unit
        drawLine(ink, Offset(center.x - width * .43f, floor),
            Offset(center.x + width * .43f, floor), strokeWidth = 1.6f * unit)
    }
}

private fun direction(from:Offset,to:Offset):Offset {
    val vector=to-from
    return if(vector.getDistance()>.01f) vector/vector.getDistance() else Offset(0f,1f)
}
private fun normal(vector:Offset)=Offset(-vector.y,vector.x)
private fun mix(a:Offset,b:Offset,t:Float)=a+(b-a)*t

private fun smoothBody(points: List<Offset>, widths: List<Float>): Path {
    val front=ArrayList<Offset>()
    val back=ArrayList<Offset>()
    for(i in points.indices){
        val side=normal(direction(points[max(0,i-1)],points[min(points.lastIndex,i+1)]))
        front += points[i]+side*widths[i]
        back += points[i]-side*(widths[i]*.86f)
    }
    val outline=front+back.asReversed()
    return Path().apply {
        moveTo(outline.first().x,outline.first().y)
        for(i in outline.indices){
            val before=outline[(i+outline.size-1)%outline.size]
            val a=outline[i];val b=outline[(i+1)%outline.size]
            val after=outline[(i+2)%outline.size]
            val c1=a+(b-before)/6f;val c2=b-(after-a)/6f
            cubicTo(c1.x,c1.y,c2.x,c2.y,b.x,b.y)
        }
        close()
    }
}

private fun DrawScope.drawActor(p: List<Offset>, u: Float, motion: ExerciseMotion, face: Picture?) {
    val head=p[0];val shoulder=p[1];val hip=p[2]
    val down=direction(shoulder,hip)
    val side=normal(down)
    val overhead = motion.view == ExerciseView.OVERHEAD
    val broad = motion.view != ExerciseView.SIDE
    val left = if (motion.prone && !broad) down.x > 0f else motion.facing == "left"
    fun limb(near:Boolean,leg:Boolean) {
        val joint=if(leg) p[if(near) 7 else 9] else p[if(near) 3 else 5]
        val end=if(leg) p[if(near) 8 else 10] else p[if(near) 4 else 6]
        val center = if (leg) hip else shoulder
        val lateral = if (broad) {
            val projected = ((joint.x - center.x) * side.x + (joint.y - center.y) * side.y)
            val sign = if (abs(projected) > 4f * u) Math.signum(projected) else if (near) 1f else -1f
            sign * if (leg) 20f else 29f
        } else if (near) -8f else 10f
        val start = center + side * (lateral * u)
        val tip = end - direction(joint, end) * (if (leg) 5f * u else 0f)
        val points=listOf(start,mix(start,joint,.33f),mix(start,joint,.71f),joint,
            mix(joint,end,.26f),mix(joint,end,.65f),tip)
        val halfWidths=if(leg) listOf(15f,26f,20f,12.5f,14f,10.7f,7.5f)
            else listOf(13f,16f,12f,9.5f,9f,7.1f,5.8f)
        val hem=mix(start,joint,if(leg) .66f else .58f)
        val fabric=if(leg) { if(near) navy else navyDark }
            else if(near) Color(0xFF57999D) else Color(0xFF3D747F)
        val tone = if (near || broad) skin else skinFar
        val fabricDark=if(leg)navyDark else teal
        val contour = smoothBody(points, halfWidths.map { it * u })
        drawPath(contour, tone)
        val armAxis = direction(start, joint)
        val seam = normal(armAxis) * (36f * u)
        val cuff = Path().apply {
            val back = start - armAxis * (26f * u)
            val a = back + seam
            val b = hem + seam
            val c = hem - seam
            val d = back - seam
            moveTo(a.x, a.y)
            lineTo(b.x, b.y)
            lineTo(c.x, c.y)
            lineTo(d.x, d.y)
            close()
        }
        clipPath(contour) {
            drawPath(cuff, Brush.linearGradient(listOf(fabricDark, fabric), start, hem))
        }
        if (!leg) {
            val planted = motion.handSupport && (overhead || abs(end.y - max(p[4].y, p[6].y)) < 21f * u)
            val forearm = direction(joint, end)
            val fingers = when {
                planted && overhead -> down * -1f
                planted -> Offset(if (left) -1f else 1f, 0f)
                else -> forearm
            }
            drawHand(end, fingers, u, tone, near, planted && !overhead)
        } else if (broad && !overhead) {
            drawFrontShoe(end, u, end.x < hip.x)
        } else {
            val axis=direction(joint,end)
            val turn=if(abs(down.y)<.60f) side*(if(left)1f else -1f) else Offset(if(left)-1f else 1f,0f)
            val foot = when {
                overhead -> direction(Offset.Zero, mix(down, axis, .4f))
                motion.handSupport && motion.prone -> direction(Offset.Zero, Offset(if (left) -.43f else .43f, .9f))
                else -> direction(Offset.Zero, mix(turn, axis, .16f))
            }
            val lower=normal(foot)*(if(foot.x<0) -1f else 1f)
            val heel=end-foot*(10*u)
            val toe=end+foot*(29*u)
            val toeBottom=toe+lower*(9*u)
            val heelBottom=heel+lower*(9*u)
            val topHeel=heel-lower*(9*u)
            val topToe=toe-lower*(4*u)
            val upper=Path().apply{
                moveTo(heelBottom.x,heelBottom.y)
                quadraticTo(topHeel.x,topHeel.y, (end-lower*(12*u)).x,(end-lower*(12*u)).y)
                quadraticTo(topToe.x,topToe.y, toeBottom.x,toeBottom.y)
                close()
            }
            drawPath(upper,shoe)
            drawLine(sole,heelBottom,toeBottom,3f*u,StrokeCap.Round)
            drawLine(Color(0xFF79B7B0),mix(heel,toe,.24f),mix(heel,toe,.83f),1.5f*u,StrokeCap.Round)
        }
    }
    limb(false,false)
    limb(false,true)
    limb(true,true)
    val neckTop=mix(head,shoulder,.46f)
    drawLine(skin,neckTop,shoulder+down*(10*u),18*u,StrokeCap.Round)
    val torso=listOf(shoulder-down*(8*u),mix(shoulder,hip,.19f),
        mix(shoulder,hip,.40f),mix(shoulder,hip,.74f),hip+down*(14*u))
    val torsoWidths = if (broad) listOf(33f, 47f, 40f, 32f, 29f)
        else listOf(14f, 36f, 35f, 26f, 24f)
    drawPath(smoothBody(torso, torsoWidths.map { it * u }),
        Brush.linearGradient(listOf(tealLight,teal,navy),end=hip+down*(17*u),start=shoulder))
    val waist=hip-down*(7*u)
    drawLine(Color(0xFF83ACAE),waist-side*(20*u),waist+side*(20*u),1.2f*u,StrokeCap.Round)
    limb(true,false)
    if(face!=null){
        val headScale=u*.96f
        val angle = atan2(down.y, down.x) * 180f / Math.PI.toFloat() - 90f
        val tilt = when {
            overhead -> angle
            motion.prone -> angle.coerceIn(-100f, 100f)
            else -> angle.coerceIn(-28f, 28f)
        }
        drawIntoCanvas { compose ->
            val c=compose.nativeCanvas
            c.save()
            c.translate(head.x,head.y)
            c.rotate(tilt)
            c.scale(if (left && !broad) -headScale else headScale,headScale)
            c.translate(-35f,-45f)
            c.drawPicture(face)
            c.restore()
        }
    }
}

private fun DrawScope.drawFrontShoe(ankle: Offset, unit: Float, left: Boolean) {
    val offset = if (left) -4f else 4f
    val foot = ankle + Offset(offset * unit, 9f * unit)
    val heelLeft = ankle + Offset(-8f * unit, -9f * unit)
    val heelRight = ankle + Offset(8f * unit, -9f * unit)
    val toeLeft = foot + Offset(-14f * unit, 5f * unit)
    val toeRight = foot + Offset(14f * unit, 5f * unit)
    val upper = Path().apply {
        moveTo(toeLeft.x, toeLeft.y)
        quadraticTo(heelLeft.x - 4f * unit, heelLeft.y, heelLeft.x, heelLeft.y)
        quadraticTo(ankle.x, ankle.y - 13f * unit, heelRight.x, heelRight.y)
        quadraticTo(heelRight.x + 4f * unit, heelRight.y, toeRight.x, toeRight.y)
        close()
    }
    drawPath(upper, shoe)
    drawLine(sole, toeLeft, toeRight, 3f * unit, StrokeCap.Round)
    drawLine(Color(0xFF79B7B0), foot - Offset(7f * unit, 3f * unit),
        foot + Offset(7f * unit, -3f * unit), 1.4f * unit, StrokeCap.Round)
}

private fun DrawScope.drawHand(
    wrist: Offset, forward: Offset, unit: Float, tone: Color, near: Boolean, sideSupport: Boolean
) {
    val across = normal(forward) * if (near) 1f else -1f
    fun point(length: Float, width: Float) = wrist + (forward * length + across * width) * unit
    val palmWidth = if (sideSupport) .53f else 1f
    val palm = smoothBody(
        listOf(point(-4f, 0f), point(4f, 0f), point(11f, 0f), point(14f, 0f)),
        listOf(5.5f, 7f, 8f, 6.3f).map { it * unit * palmWidth }
    )
    drawPath(palm, tone)
    val fingerLengths = listOf(20f, 24f, 25f, 22f)
    for ((index, length) in fingerLengths.withIndex()) {
        val spread = (index - 1.5f) * 4.4f
        drawLine(tone, point(10f, spread * palmWidth),
            point(length, spread * 1.32f * palmWidth), (if (sideSupport) 3f else 3.7f) * unit, StrokeCap.Round)
    }
    val thumb = if (sideSupport) 5.5f else 10.5f
    drawLine(tone, point(3f, 4f * palmWidth), point(12f, thumb), 5f * unit, StrokeCap.Round)
}

private fun DrawScope.drawProp(prop: ExerciseProp, unit: Float) {
    val bounds = prop.bounds
    val color = Color(0xFF607E8B)
    val left = Offset(bounds.left, bounds.top)
    val right = Offset(bounds.right, bounds.top)
    if (prop.kind == "wall" || prop.kind == "step") {
        drawRoundRect(color.copy(alpha = .65f), left, bounds.size, CornerRadius(4f * unit))
        return
    }
    val seat = if (prop.kind == "incline") right.copy(y = bounds.center.y) else right
    drawLine(color, left, seat, 8f * unit, StrokeCap.Round)
    for (point in listOf(left, seat)) {
        drawLine(color, point, point.copy(y = bounds.bottom), 4f * unit, StrokeCap.Round)
    }
    if (prop.kind == "chair") {
        drawLine(color, left.copy(y = left.y - bounds.height * .45f), left, 5f * unit, StrokeCap.Round)
    }
}

private fun DrawScope.drawEquipment(kind:String,p:List<Offset>,u:Float,floor:Float){
    val a=p[4];val b=p[6];val between=mix(a,b,.5f)
    val muted=Color(0xFF799BAA)
    when(kind){
        "ball" -> {
            val center=Offset(p[8].x+17*u,floor+4*u)
            drawCircle(Color(0xFFBDD2D6),13*u,center)
            drawCircle(Color(0xFF4C7786),5*u,center)
        }
        "basketball","volleyball" -> drawCircle(if(kind=="basketball") Color(0xFFD59165) else Color(0xFFD4E1D6),
            13*u,between)
        "bar" -> drawLine(muted,a-Offset(37*u,0f),a+Offset(37*u,0f),5*u,StrokeCap.Round)
        "barbell" -> {
            drawLine(muted,between-Offset(70*u,0f),between+Offset(70*u,0f),5*u,StrokeCap.Round)
            drawCircle(muted,13*u,between-Offset(56*u,0f))
            drawCircle(muted,13*u,between+Offset(56*u,0f))
        }
        "wall" -> drawLine(muted,a-Offset(0f,82*u),Offset(a.x,floor+16*u),4*u)
        "rope" -> drawLine(muted,a-Offset(70*u,74*u),a+Offset(80*u,-74*u),3*u)
        "racket" -> {
            val tip=a+direction(p[3],a)*(26*u)
            drawLine(muted,a,tip,4*u)
            drawOval(muted,tip-Offset(11*u,14*u),Size(22*u,28*u))
        }
        "parallettes" -> for(hand in listOf(a,b)){
            drawLine(muted,hand-Offset(23*u,0f),hand+Offset(23*u,0f),5*u,StrokeCap.Round)
            for(x in listOf(hand.x-17*u,hand.x+17*u)) drawLine(muted,Offset(x,hand.y),Offset(x,floor+14*u),4*u)
        }
        "bike" -> {
            val rear=p[2]+Offset(-64*u,112*u)
            val front=p[2]+Offset(104*u,112*u)
            val crank=p[2]+Offset(16*u,109*u)
            for(pair in listOf(rear to crank,crank to front,p[2] to rear,p[2] to crank,a to front))
                drawLine(muted,pair.first,pair.second,4*u)
            for(wheel in listOf(front,rear)) drawCircle(muted,34*u,wheel,style=androidx.compose.ui.graphics.drawscope.Stroke(3*u))
        }
    }
}
