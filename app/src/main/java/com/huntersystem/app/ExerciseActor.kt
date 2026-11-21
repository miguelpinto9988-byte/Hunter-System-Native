package com.huntersystem.app

import android.graphics.Picture
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.caverock.androidsvg.SVG
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.isActive
import kotlin.math.abs
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
internal fun rememberExercisePhase(key: Any, duration: Float, play: Boolean, reset: Int): MutableFloatState {
    val reduced = LocalMonarchMotion.current.reduced
    val visible = LocalLifecycleOwner.current.lifecycle.currentStateAsState().value.isAtLeast(Lifecycle.State.STARTED)
    val phase = remember(key, reset) { mutableFloatStateOf(0f) }
    LaunchedEffect(key, duration, play, reduced, reset, visible) {
        if (!play || reduced || !visible) return@LaunchedEffect
        var previous = withInfiniteAnimationFrameNanos { it }
        while (isActive) {
            val next = withInfiniteAnimationFrameNanos { it }
            phase.floatValue = (phase.floatValue + (next - previous).coerceAtMost(80_000_000L) /
                (duration * 1_000_000_000f)) % 1f
            previous = next
        }
    }
    return phase
}

@Composable
internal fun ExerciseActor(motion: ExerciseMotion, modifier: Modifier, play: Boolean,
                           mirror: Boolean, reset: Int, sequenceFraming: List<ExerciseFraming> = emptyList(),
                           themedStage: Boolean = false, controlsWidth: Dp = 0.dp,
                           phaseOverride: (() -> Float)? = null) {
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
    val playback = rememberExercisePhase(motion, motion.duration,
        play && !motion.staticHold && phaseOverride == null, reset)
    val framing = remember(motion) { ExerciseFraming.from(motion) }
    val equipmentPictures = remember(context, framing) {
        val assets = framing.equipment.frame(motion.frames.first(),0f).sprites.map { it.asset } +
            framing.equipment.authoredSupports.mapNotNull { ExerciseEquipmentPlan.forProp(it.kind)?.asset }
        assets.distinct().associateWith { EquipmentSvg.load(context,it) }
    }
    val labels = remember(motion) { equipmentLabels(motion) }
    val firstArmSide = remember(motion) { projectedSide(motion,3,5) }
    val firstLegSide = remember(motion) { projectedSide(motion,7,9) }
    val lightStage = LocalHunter.current.isLight
    val backdrop = ExerciseGymBackdrop.forView(motion.view)
    val gymPicture = remember(context, backdrop, lightStage) { backdrop.load(context, lightStage) }
    val stageBase = if (lightStage) Color(0xFFE4EDF9) else Color(0xFF101F2A)

    val accessibleStage = if(labels.isEmpty()) modifier else modifier.semantics {
        contentDescription = "Equipo: " + labels.joinToString(", ")
    }
    Box(accessibleStage) {
        Canvas(Modifier.matchParentSize()) {
            if (gymPicture != null && size.minDimension > 0f) drawGymBackdrop(gymPicture)
            else drawRoundRect(stageBase, cornerRadius = CornerRadius(18f))
        }
        Canvas(Modifier.matchParentSize()) {
            if (size.minDimension <= 0f) return@Canvas
            val stage = Size((size.width - controlsWidth.toPx()).coerceAtLeast(0f), size.height)
            val commonScale = sequenceFraming.minOfOrNull { it.scaleFor(stage, themedStage) * it.unit }
            val fit = commonScale?.div(framing.unit) ?: framing.scaleFor(stage, themedStage)
            if (fit <= 0f) return@Canvas
            val origin = framing.bounds.center
            val phase = phaseOverride?.invoke() ?: playback.floatValue
            val p = motion.at(phase)
            val equipment = framing.equipment.frame(p,phase)
            withTransform({
                translate(stage.width/2,stage.height/2)
                scale(if(mirror) -fit else fit,fit,pivot=Offset.Zero)
                translate(-origin.x,-origin.y)
            }) {
                drawStage(motion, p, origin, framing.bounds.width, framing.bounds.height, framing.unit, framing.floor)
                drawExerciseEquipment(equipment,framing.equipment.authoredSupports,equipmentPictures,framing.unit)
                drawActor(p, framing.unit, motion, face, firstArmSide, firstLegSide, phase,equipment.grippedWrists)
                for (sprite in equipment.sprites) {
                    if (sprite.foreground) drawEquipmentSprite(sprite,equipmentPictures[sprite.asset])
                }
            }
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

private fun projectedSide(motion:ExerciseMotion, first:Int, second:Int):Float {
    if (motion.view == ExerciseView.SIDE) return 1f
    val separation = motion.frames.sumOf { pose ->
        val across = normal(direction(pose[1], pose[2]))
        val pair = pose[first] - pose[second]
        (pair.x * across.x + pair.y * across.y).toDouble()
    }
    return if (separation < 0) -1f else 1f
}

private fun direction(from:Offset,to:Offset):Offset {
    val vector=to-from
    return if(vector.getDistance()>.01f) vector/vector.getDistance() else Offset(0f,1f)
}
private fun normal(vector:Offset)=Offset(-vector.y,vector.x)
private fun mix(a:Offset,b:Offset,t:Float)=a+(b-a)*t

private fun smoothBody(points: List<Offset>, widths: List<Float>, symmetric: Boolean = false): Path {
    val front=ArrayList<Offset>()
    val back=ArrayList<Offset>()
    for(i in points.indices){
        val side=normal(direction(points[max(0,i-1)],points[min(points.lastIndex,i+1)]))
        front += points[i]+side*widths[i]
        back += points[i]-side*(widths[i] * (if (symmetric) 1f else .86f))
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

private fun DrawScope.drawActor(
    p: List<Offset>, u: Float, motion: ExerciseMotion, face: Picture?, firstArmSide: Float, firstLegSide: Float,
    phase: Float, grippedWrists: Set<Int>
) {
    val head=p[0];val shoulder=p[1];val hip=p[2]
    val down=direction(shoulder,hip)
    val side=normal(down)
    val overhead = motion.view == ExerciseView.OVERHEAD
    val front = motion.view == ExerciseView.FRONT
    val neckSpan = (shoulder.x - head.x) * down.x + (shoulder.y - head.y) * down.y
    // The head art extends 33 units toward the collar, which starts 8 units before the shoulders.
    val visibleNeck = (neckSpan - 41f * u).coerceAtLeast(0f)
    val headPosition = if (overhead) head + down * (visibleNeck * .25f) else head
    val broad = motion.view != ExerciseView.SIDE
    val armsBehindShirt = overhead || motion.view == ExerciseView.REAR
    val headPlacement = ExercisePoseCommands.head(motion, p)
    val left = headPlacement.left
    val shirt = Brush.linearGradient(listOf(tealLight, teal, navy),
        start = shoulder, end = hip + down * (17f * u))
    fun limb(near:Boolean,leg:Boolean) {
        val joint=if(leg) p[if(near) 7 else 9] else p[if(near) 3 else 5]
        val end=if(leg) p[if(near) 8 else 10] else p[if(near) 4 else 6]
        val center = if (leg) hip else shoulder
        val lateral = if (broad) {
            val width = if (leg) firstLegSide * 20f else firstArmSide * 29f
            if (near) width else -width
        } else if (near) -8f else 10f
        val start = center + side * (lateral * u)
        val tip = end - direction(joint, end) * (if (leg) 5f * u else 0f)
        val points=listOf(start,mix(start,joint,.33f),mix(start,joint,.71f),joint,
            mix(joint,end,.26f),mix(joint,end,.65f),tip)
        val halfWidths = when {
            front && leg -> listOf(15f, 20f, 17f, 12.5f, 13f, 9.5f, 7.5f)
            leg -> listOf(15f, 26f, 20f, 12.5f, 14f, 10.7f, 7.5f)
            else -> listOf(13f, 16f, 12f, 9.5f, 9f, 7.1f, 5.8f)
        }
        val hem=mix(start,joint,if(leg) .66f else .58f)
        val fabric=if(leg) { if(near) navy else navyDark }
            else if(near) Color(0xFF57999D) else Color(0xFF3D747F)
        val tone = if (near || broad) skin else skinFar
        val fabricDark=if(leg)navyDark else teal
        val contour = smoothBody(points, halfWidths.map { it * u }, symmetric = broad && !leg)
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
        if (!front || !leg) clipPath(contour) {
            val shading = if (broad && !leg) shirt
                else Brush.linearGradient(listOf(fabricDark, fabric), start, hem)
            drawPath(cuff, shading)
        }
        if (!leg) {
            val planted = motion.handSupport && (overhead || abs(end.y - max(p[4].y, p[6].y)) < 21f * u)
            val forearm = direction(joint, end)
            val fingers = when {
                planted && overhead -> down * -1f
                planted -> Offset(if (left) -1f else 1f, 0f)
                else -> forearm
            }
            val gesture = motion.handGesture
            val curl = if ((if (near) 4 else 6) in grippedWrists) .74f else gesture?.curl(phase) ?: 0f
            drawHand(end, gesture?.direction(fingers,phase,near) ?: fingers, u, tone, near,
                planted && !overhead, curl, gesture?.projectedLength(phase) ?: 1f)
        } else if (motion.authoredFloor == AuthoredFloorMovement.TOE_TOWEL) {
            drawToeGather(end,u,tone,if (near) ExercisePoseCommands.toeGather(phase) else 0f)
        } else if (broad && !overhead) {
            drawFrontShoe(end, u, if (near) motion.firstFootOnLeft else !motion.firstFootOnLeft)
        } else {
            val foot = ExercisePoseCommands.foot(motion, p, joint, end, near, phase)
            val lower=normal(foot)*(if(foot.x<0) -1f else 1f)
            val fixedHeel=motion.toeLift!=null || motion.authoredFloor?.isHamstringSlide==true
            val heelAxis=if(fixedHeel) Offset(if(foot.x<0) -1f else 1f,0f) else foot
            val heel=end-heelAxis*(10*u)
            val toe=heel+foot*(39*u)
            val toeBottom=toe+lower*(9*u)
            val heelBottom=heel+(if(fixedHeel) Offset(0f,1f) else lower)*(9*u)
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
    if (!broad) limb(false, false)
    limb(false, true)
    limb(true, true)
    if (armsBehindShirt) {
        limb(false, false)
        limb(true, false)
    }
    val waist = hip - down * (13f * u)
    if (front) drawFrontShorts(waist, hip, down, side, p[7], p[9], u)
    val neckTop = mix(headPosition, shoulder, .46f)
    drawLine(skin,neckTop,shoulder+down*(10*u),18*u,StrokeCap.Round)
    val torso=listOf(shoulder-down*(8*u),mix(shoulder,hip,.19f),
        mix(shoulder,hip,.40f), mix(shoulder,hip,.74f),
        if (front) waist else hip + down * (14f * u))
    val torsoWidths = when {
        front -> listOf(33f, 47f, 39f, 31f, 33f)
        broad -> listOf(33f, 47f, 40f, 32f, 29f)
        else -> listOf(14f, 36f, 35f, 26f, 24f)
    }
    val shirtShape = smoothBody(torso, torsoWidths.map { it * u }, symmetric = broad)
    if (front) {
        val halfClip = side * (170f * u)
        val top = shoulder - down * (80f * u)
        val hem = Path().apply {
            moveTo((top - halfClip).x, (top - halfClip).y)
            lineTo((top + halfClip).x, (top + halfClip).y)
            lineTo((waist + halfClip).x, (waist + halfClip).y)
            lineTo((waist - halfClip).x, (waist - halfClip).y)
            close()
        }
        clipPath(hem) { drawPath(shirtShape, shirt) }
    } else {
        drawPath(shirtShape, shirt)
    }
    val seam = if (front) waist else hip - down * (7f * u)
    val seamWidth = if (front) 31f else 20f
    drawLine(Color(0xFF83ACAE), seam - side * (seamWidth * u),
        seam + side * (seamWidth * u), 1.2f * u, StrokeCap.Round)
    if (!armsBehindShirt) {
        if (broad) limb(false, false)
        limb(true, false)
    }
    if(face!=null){
        val headScale=u*.96f
        drawIntoCanvas { compose ->
            val c=compose.nativeCanvas
            c.save()
            c.translate(headPosition.x, headPosition.y)
            c.rotate(headPlacement.rotation)
            c.scale(if (left && !broad) -headScale else headScale,headScale)
            c.translate(-35f,-45f)
            c.drawPicture(face)
            c.restore()
        }
    }
}

private fun DrawScope.drawToeGather(ankle: Offset, unit: Float, tone: Color, curl: Float) {
    fun point(x: Float,y: Float) = ankle + Offset(x,y) * unit
    val toe = point(36f-9f*curl,9f)
    val foot = Path().apply {
        val heel = point(-10f,10f)
        moveTo(heel.x,heel.y)
        quadraticTo(point(-16f,2f).x,point(-16f,2f).y,point(-9f,-8f).x,point(-9f,-8f).y)
        quadraticTo(point(6f,-9f).x,point(6f,-9f).y,point(17f,-1f).x,point(17f,-1f).y)
        quadraticTo(point(32f-5f*curl,-4f-4f*curl).x,point(32f-5f*curl,-4f-4f*curl).y,toe.x,toe.y)
        close()
    }
    drawPath(foot,tone)
    drawLine(tone.copy(alpha=.65f),point(18f,5f),point(25f-4f*curl,3f),1.8f*unit,StrokeCap.Round)
}

private fun DrawScope.drawFrontShorts(
    waist: Offset, hip: Offset, down: Offset, side: Offset,
    firstKnee: Offset, secondKnee: Offset, unit: Float
) {
    val firstProjection = (firstKnee.x - secondKnee.x) * side.x +
        (firstKnee.y - secondKnee.y) * side.y
    val (leftKnee, rightKnee) = if (firstProjection < 0f) firstKnee to secondKnee
        else secondKnee to firstKnee
    val leftHip = hip - side * (20f * unit)
    val rightHip = hip + side * (20f * unit)
    val leftHem = mix(leftHip, leftKnee, .43f)
    val rightHem = mix(rightHip, rightKnee, .43f)
    val leftAcross = normal(direction(leftHip, leftKnee)) * (20f * unit)
    val rightAcross = normal(direction(rightHip, rightKnee)) * (20f * unit)
    val leftOuter = leftHem - leftAcross
    val leftInner = leftHem + leftAcross
    val rightInner = rightHem - rightAcross
    val rightOuter = rightHem + rightAcross
    val leftWaist = waist - side * (33f * unit)
    val rightWaist = waist + side * (33f * unit)
    val rise = hip + down * (31f * unit)
    val leftHipLine = leftHip - side * (16f * unit)
    val rightHipLine = rightHip + side * (16f * unit)
    val shape = Path().apply {
        moveTo(leftWaist.x, leftWaist.y)
        lineTo(rightWaist.x, rightWaist.y)
        quadraticTo(rightHipLine.x, rightHipLine.y, rightOuter.x, rightOuter.y)
        lineTo(rightInner.x, rightInner.y)
        val rightCurve = mix(rightInner, rise, .66f)
        val leftCurve = mix(leftInner, rise, .66f)
        cubicTo(rightCurve.x, rightCurve.y, leftCurve.x, leftCurve.y, leftInner.x, leftInner.y)
        lineTo(leftOuter.x, leftOuter.y)
        quadraticTo(leftHipLine.x, leftHipLine.y, leftWaist.x, leftWaist.y)
        close()
    }
    val frontFabric = Brush.radialGradient(
        listOf(Color(0xFF304D63), navy, navyDark),
        center = mix(waist, hip, .8f),
        radius = 66f * unit
    )
    drawPath(shape, frontFabric)
    val stitch = Color(0xFF8FB5C1).copy(alpha = .23f)
    for ((top, curve, bottom) in listOf(
        Triple(leftWaist + side * (4f * unit), leftHipLine + side * (4f * unit), mix(leftOuter, leftInner, .11f)),
        Triple(rightWaist - side * (4f * unit), rightHipLine - side * (4f * unit), mix(rightOuter, rightInner, .11f))
    )) {
        val seam = Path().apply {
            val from = mix(top, curve, .24f)
            val to = mix(bottom, curve, .12f)
            moveTo(from.x, from.y)
            quadraticTo(curve.x, curve.y, to.x, to.y)
        }
        drawPath(seam, stitch, style = Stroke(width = 1.25f * unit, cap = StrokeCap.Round))
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
    wrist: Offset, forward: Offset, unit: Float, tone: Color, near: Boolean, sideSupport: Boolean,
    curl: Float, projectedLength: Float
) {
    val across = normal(forward) * if (near) 1f else -1f
    fun point(length: Float, width: Float) = wrist + (forward * (length * projectedLength) + across * width) * unit
    val palmWidth = if (sideSupport) .53f else 1f
    val palm = smoothBody(
        listOf(point(-4f, 0f), point(4f, 0f), point(11f, 0f), point(14f, 0f)),
        listOf(5.5f, 7f, 8f, 6.3f).map { it * unit * palmWidth }
    )
    drawPath(palm, tone)
    val fingerLengths = listOf(20f, 24f, 25f, 22f)
    for ((index, length) in fingerLengths.withIndex()) {
        val spread = (index - 1.5f) * 4.4f
        val flexedTip = point(length - (length - 12.5f) * curl, spread * (1.32f - .36f * curl) * palmWidth)
        drawLine(tone, point(10f, spread * palmWidth), flexedTip,
            (if (sideSupport) 3f else 3.7f) * unit, StrokeCap.Round)
    }
    val thumb = if (sideSupport) 5.5f else 10.5f
    drawLine(tone, point(3f, 4f * palmWidth), point(12f, thumb - 5f * curl), 5f * unit, StrokeCap.Round)
}
