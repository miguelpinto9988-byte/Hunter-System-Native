package com.huntersystem.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal data class EquipmentSprite(
    val asset: String,
    val center: Offset,
    val size: Size,
    val angle: Float = 0f,
    val foreground: Boolean = false
) {
    val bounds: Rect
        get() {
            val radians = angle * PI.toFloat() / 180f
            val c = abs(cos(radians))
            val s = abs(sin(radians))
            val half = Offset((size.width*c+size.height*s)/2, (size.width*s+size.height*c)/2)
            return Rect(center - half, center + half)
        }
}

internal enum class EquipmentStroke(val width: Float) { CABLE(2f), BAND(5f), ROPE(3.4f), STEEL(5f) }

internal data class EquipmentLink(val start: Offset, val end: Offset, val stroke: EquipmentStroke, val bend: Offset? = null)

internal data class EquipmentFrame(
    val sprites: List<EquipmentSprite>,
    val links: List<EquipmentLink>,
    val grippedWrists: Set<Int>
)

/** All fixed attachments use the original keyframes. Moving attachments use the current interpolated pose. */
internal class ExerciseEquipmentScene(
    private val motion: ExerciseMotion,
    private val body: Rect,
    private val unit: Float
) {
    private val rest = motion.frames.first()
    private val facing = if (motion.facing == "left") -1f else 1f
    private val ground = body.bottom + 15f * unit
    private val front = (if (facing > 0f) body.right else body.left) + facing * 45f * unit
    private val rear = (if (facing > 0f) body.left else body.right) - facing * 38f * unit
    private val overhead = motion.view == ExerciseView.OVERHEAD
    private val pedals = motion.frames.flatMap { listOf(it[8],it[10]) }
    private val workingLeg by lazy { motion.frames.map { it[8] } }
    private val crank = Offset((pedals.minOf { it.x } + pedals.maxOf { it.x }) / 2,
        (pedals.minOf { it.y } + pedals.maxOf { it.y }) / 2 + 8f * unit)

    val authoredSupports: List<ExerciseProp> = if (motion.apparatus.any { it.gear in ExerciseEquipmentPlan.cardioMachines })
        motion.props.filterNot { it.kind in setOf("chair","bench") } else motion.props

    fun frame(pose: List<Offset>, phase: Float): EquipmentFrame {
        val draw = Placement(pose,phase)
        for (item in motion.apparatus) draw.add(item)
        return EquipmentFrame(draw.sprites,draw.links,draw.gripped)
    }

    /** Keep the zoom fixed during playback and include the entire installed machine. */
    fun bounds(): Rect {
        if (motion.apparatus.isEmpty()) return body
        var left = body.left
        var right = body.right
        var top = body.top
        var bottom = body.bottom
        fun include(rect: Rect) {
            left = minOf(left,rect.left)
            right = maxOf(right,rect.right)
            top = minOf(top,rect.top)
            bottom = maxOf(bottom,rect.bottom)
        }
        for ((index,pose) in motion.frames.withIndex()) {
            val sample = frame(pose,motion.times[index])
            for (sprite in sample.sprites) include(sprite.bounds)
            for (link in sample.links) {
                include(Rect(link.start,link.start))
                include(Rect(link.end,link.end))
                link.bend?.let { bend ->
                    val midpoint = middle(middle(link.start,bend),middle(bend,link.end))
                    include(Rect(midpoint,midpoint))
                }
            }
        }
        return Rect(left,top,right,bottom)
    }

    private inner class Placement(private val p: List<Offset>, private val phase: Float) {
        val sprites = mutableListOf<EquipmentSprite>()
        val links = mutableListOf<EquipmentLink>()
        val gripped = mutableSetOf<Int>()
        private val hips = p[2]
        private val feet = middle(p[8],p[10])
        private val wrists = listOf(4,6)
        private val hands get() = middle(grip(4),grip(6))

        private fun grip(index: Int): Offset = p[index] + unitVector(p[index] - p[index-1]) * (8f * unit)
        private fun wrists(mount: GearMount) = when (mount) {
            GearMount.NEAR -> listOf(4)
            GearMount.FAR -> listOf(6)
            else -> wrists
        }
        private fun handCenter(mount: GearMount): Offset {
            val used = wrists(mount)
            gripped.addAll(used)
            return if (used.size == 1) grip(used.single()) else hands
        }

        private fun mark(asset: String, center: Offset, width: Float, height: Float, angle: Float = 0f, foreground: Boolean = false) {
            sprites += EquipmentSprite(asset,center,Size(width*unit,height*unit),angle,foreground)
        }
        private fun line(a: Offset,b: Offset,style: EquipmentStroke = EquipmentStroke.STEEL,bend: Offset? = null) {
            links += EquipmentLink(a,b,style,bend)
        }
        private fun ribbon(asset: String, a: Offset, b: Offset, width: Float, front: Boolean = false) {
            mark(asset,middle(a,b),(b-a).getDistance()/unit+5f,width,angle(b-a),front)
        }
        private fun stand(x: Float, top: Float, asset: String = "weight-stack", width: Float = 75f) {
            mark(asset,Offset(x,(top+ground)/2),width,(ground-top)/unit)
        }
        private fun support(asset: String, at: Offset, width: Float, floor: Float = ground) {
            val height = (floor-at.y).coerceAtLeast(15f*unit)
            mark(asset,at+Offset(0f,height/2),width,height/unit)
        }

        fun add(item: DemoApparatus) {
            val asset = item.gear.asset
            when (item.gear) {
                DemoGear.DUMBBELL, DemoGear.KETTLEBELL -> freeWeight(item)
                DemoGear.BARBELL, DemoGear.TRAP_BAR, DemoGear.LANDMINE, DemoGear.SMITH -> loadedBar(item)
                DemoGear.PLATE -> mark(asset,handCenter(item.mount),53f,42f)
                DemoGear.TOWEL -> towel(item)
                DemoGear.BAND, DemoGear.CABLE, DemoGear.SKI_ERG, DemoGear.STRAP, DemoGear.BATTLE_ROPE, DemoGear.ROPE -> tether(item)
                DemoGear.LEG_PRESS, DemoGear.GUIDED_SQUAT, DemoGear.LEG_LEVER, DemoGear.HIP_LEVER,
                DemoGear.ARM_LEVER, DemoGear.ASSISTED -> lever(item)
                DemoGear.TIBIALIS -> for ((knee,ankle) in listOf(9 to 10,7 to 8)) {
                    val axis = ExercisePoseCommands.foot(motion,p,p[knee],p[ankle],ankle==8,phase)
                    val normal = Offset(-axis.y,axis.x)*facing
                    val heel = p[ankle]-Offset(10f*facing*unit,0f)
                    val pivot = heel+Offset(-12f*facing*unit,8f*unit)
                    val pad = heel+axis*(30f*unit)-normal*(10f*unit)
                    line(pivot,pivot+Offset(39f*facing*unit,0f))
                    line(pivot,pad)
                    mark(asset,pad,32f,26f,foreground=true)
                }
                DemoGear.BIKE, DemoGear.AIR_BIKE, DemoGear.RECUMBENT, DemoGear.ROWER, DemoGear.TREADMILL,
                DemoGear.ELLIPTICAL, DemoGear.STEP_MILL -> cardio(item.gear)
                DemoGear.BALL, DemoGear.MED_BALL -> mark(asset,
                    if(item.mount==GearMount.KNEE) middle(p[7],p[9]) else handCenter(item.mount),62f,48f)
                DemoGear.SWISS_BALL -> mark(asset,rest[2]+Offset(0f,68f*unit),183f,135f)
                DemoGear.SOCCER -> {
                    val ankle = if (rest[8].x*facing >= rest[10].x*facing) rest[8] else rest[10]
                    val toe = ankle + Offset(33f*facing*unit,0f)
                    mark(asset,Offset(toe.x,if(overhead) toe.y else ground-14f*unit),40f,35f)
                }
                DemoGear.BASKETBALL, DemoGear.VOLLEYBALL -> mark(asset,hands+Offset(0f,-22f*unit),55f,47f)
                DemoGear.RACKET -> {
                    val hand = handCenter(item.mount)
                    val axis = unitVector(p[4]-p[3])
                    mark(asset,hand+axis*(30f*unit),98f,58f,angle(axis))
                }
                DemoGear.JUMP_ROPE -> jumpRope()
                DemoGear.RINGS -> for (wrist in wrists) {
                    val hand = handCenter(if (wrist == 4) GearMount.NEAR else GearMount.FAR)
                    val ring = hand + Offset(0f,-13f*unit)
                    line(Offset(rest[wrist].x,body.top-48f*unit),ring,EquipmentStroke.ROPE)
                    mark(asset,ring,38f,32f)
                }
                DemoGear.PULL_BAR,DemoGear.LOW_BAR,DemoGear.PARALLETTES,DemoGear.STICK -> simpleBar(item.gear)
                DemoGear.AB_WHEEL -> {
                    mark(asset,handCenter(item.mount)+Offset(0f,13f*unit),64f,46f,phase*360f)
                    mark("bar",hands,63f,23f)
                }
                DemoGear.SLIDER -> {
                    val contacts = when (item.mount) {
                        GearMount.BOTH -> wrists
                        GearMount.NEAR -> listOf(8)
                        else -> listOf(8,10)
                    }
                    for (index in contacts)
                        mark(asset,p[index]+Offset(0f,15f*unit),54f,19f)
                }
                DemoGear.FOAM_ROLLER -> mark(asset,rest[1]+(rest[2]-rest[1])*.27f+
                    Offset(0f,20f*unit),135f,32f,if (overhead) angle(rest[2]-rest[1]) else 0f)
                DemoGear.BENCH,DemoGear.CHAIR,DemoGear.TABLE,DemoGear.INCLINED_SUPPORT ->
                    support(asset,if (item.gear==DemoGear.TABLE) rest[6]+Offset(0f,11f*unit)
                        else rest[2]+Offset(0f,20f*unit),if (item.gear==DemoGear.CHAIR) 100f else 151f)
                DemoGear.STEP,DemoGear.BOX -> {
                    val foot = listOf(rest[8],rest[10]).minBy { it.y }
                    val height = if (item.gear==DemoGear.BOX) 65f else 36f
                    mark(asset,Offset(foot.x,ground-(height/2)*unit),133f,height)
                }
                DemoGear.MAT -> if(!overhead) mark(asset,Offset(body.center.x,ground+3f*unit),
                    (body.width/unit+80f).coerceAtLeast(240f),20f)
                DemoGear.WALL -> {
                    val nearWall = if (motion.equipment=="rear-wall") rear else rest[4].x
                    stand(nearWall,rest[1].y-72f*unit,asset,29f)
                }
                DemoGear.RACK -> stand(body.center.x,body.top-46f*unit,asset,240f)
                DemoGear.ANCHOR -> mark(asset,Offset(front,ground-32f*unit),44f,63f)
                DemoGear.CONES -> for (x in listOf(body.left - 21f*unit,body.right + 21f*unit))
                    mark(asset,Offset(x,ground-16f*unit),44f,40f)
                DemoGear.LADDER -> mark(asset,Offset(body.center.x,ground),body.width/unit+40f,55f,90f)
                DemoGear.HURDLE -> mark(asset,Offset(front,ground-26f*unit),95f,61f)
                DemoGear.SLED -> {
                    val sled = Offset(handCenter(item.mount).x+55f*facing*unit,ground-18f*unit)
                    line(hands,sled+Offset(-20f*facing*unit,-20f*unit))
                    mark(asset,sled,119f,65f)
                }
                DemoGear.HARNESS -> mark(asset,hips-Offset(0f,24f*unit),81f,41f,foreground=true)
            }
        }

        private fun freeWeight(item: DemoApparatus) {
            val size = if(item.gear==DemoGear.KETTLEBELL) 52f else 57f
            when(item.mount) {
                GearMount.SHARED -> mark(item.gear.asset,handCenter(item.mount),size,36f,
                    if(item.gear==DemoGear.KETTLEBELL) 0f else 90f)
                GearMount.HIP -> mark(item.gear.asset,hips-Offset(0f,17f*unit),size,36f,foreground=true)
                else -> for (index in wrists(item.mount).asReversed()) {
                    gripped += index
                    val forearm = p[index]-p[index-1]
                    val angle = if (item.gear==DemoGear.KETTLEBELL) 0f else angle(forearm)+90f
                    val center = grip(index)+Offset(0f,if(item.gear==DemoGear.KETTLEBELL) 10f*unit else 0f)
                    mark(item.gear.asset,center,size,36f,angle)
                }
            }
        }

        private fun loadedBar(item: DemoApparatus) {
            val center = handCenter(item.mount)
            val side = motion.view==ExerciseView.SIDE
            val angle = if (side && item.gear==DemoGear.TRAP_BAR) -8f else 0f
            if(item.gear==DemoGear.SMITH) {
                stand(if (side) middle(rest[4],rest[6]).x else body.center.x,body.top-44f*unit,"smith",198f)
            }
            if(item.gear==DemoGear.LANDMINE) {
                val anchor = Offset(front,ground-8f*unit)
                line(anchor,center)
                mark("anchor",anchor,35f,34f)
                mark("plate",center+(anchor-center)*.15f,47f,34f,angle(anchor-center))
            } else {
                val width = if (side) 127f else ((p[4]-p[6]).getDistance()/unit + 69f).coerceAtLeast(135f)
                mark(item.gear.asset,center,width,47f,angle)
            }
        }

        private fun towel(item: DemoApparatus) {
            if (item.mount==GearMount.FOREHEAD) {
                mark("towel",rest[0]+Offset(0f,7f*unit),91f,40f)
                return
            }
            if(item.mount==GearMount.FLOOR) {
                when (motion.authoredFloor) {
                    AuthoredFloorMovement.HAMSTRING_SLIDE, AuthoredFloorMovement.HAMSTRING_SLIDE_SINGLE ->
                        mark("towel",p[8]+Offset(-8f*facing*unit,14f*unit),57f,14f)
                    AuthoredFloorMovement.TOE_TOWEL -> {
                        val curl = ExercisePoseCommands.toeGather(phase)
                        mark("towel",p[8]+Offset((43f-11f*curl)*unit,14f*unit),
                            78f-29f*curl,13f+4f*curl)
                    }
                    else -> mark("towel",p[8]+Offset(9f*facing*unit,11f*unit),68f,24f)
                }
                return
            }
            val farWrap = p[10] + Offset(26f*facing*unit,7f*unit)
            val nearWrap = p[8] + Offset(27f*facing*unit,7f*unit)
            gripped += wrists
            ribbon("towel",grip(6),farWrap,12f)
            ribbon("towel",grip(4),nearWrap,15f)
            ribbon("towel",farWrap,nearWrap,13f,front=true)
        }

        private fun tether(item: DemoApparatus) {
            val cable = item.gear==DemoGear.CABLE || item.gear==DemoGear.SKI_ERG
            val band = item.gear==DemoGear.BAND
            if (band && item.mount==GearMount.ANKLE_LOOP) {
                val width = ((p[8]-p[10]).getDistance()/unit + 32f).coerceAtLeast(45f)
                mark("band",feet,width,26f,angle(p[10]-p[8]),true)
                return
            }
            if (band && item.mount==GearMount.FOOT_TETHER) {
                val foot = p[10]+Offset(27f*facing*unit,0f)
                for (index in wrists) {
                    gripped += index
                    line(foot,grip(index),EquipmentStroke.BAND)
                }
                mark("band",foot,39f,28f,foreground=true)
                return
            }
            val targets = when(item.mount) {
                GearMount.ANKLE -> listOf(p[8])
                GearMount.KNEE -> listOf(middle(p[7],p[9]))
                GearMount.HIP -> listOf(hips)
                else -> if (item.bilateralTether || item.gear==DemoGear.STRAP || item.gear==DemoGear.BATTLE_ROPE ||
                    item.gear==DemoGear.SKI_ERG) wrists.map { gripped += it; grip(it) }
                    else listOf(handCenter(item.mount))
            }
            if (band && item.mount==GearMount.KNEE) {
                val pair = p[7] to p[9]
                val width = ((pair.first-pair.second).getDistance()/unit + 32f).coerceAtLeast(45f)
                mark("band",middle(pair.first,pair.second),width,26f,angle(pair.second-pair.first),true)
                return
            }
            val top = body.top - 45f*unit
            val style = if (band) EquipmentStroke.BAND else if(cable) EquipmentStroke.CABLE else EquipmentStroke.ROPE
            for ((index,target) in targets.withIndex()) {
                val x = if(item.bilateralTether && index==0) body.left-52f*unit else front
                val height = if(item.gear==DemoGear.STRAP) .95f else item.pulleyHeight
                val anchor = Offset(x,ground-(ground-top)*height)
                if(cable) {
                    stand(x,top)
                    mark("pulley",anchor,32f,25f)
                    val restTarget = when(item.mount) {
                        GearMount.ANKLE -> rest[8]
                        GearMount.NEAR -> rest[4]
                        GearMount.FAR -> rest[6]
                        else -> if(targets.size==1) middle(rest[4],rest[6]) else rest[if(index==0) 4 else 6]
                    }
                    val extension = ((target-anchor).getDistance()-(restTarget-anchor).getDistance())/unit
                    val lift = (extension*.30f).coerceIn(0f,48f)
                    val stack = Offset(x,ground-(ground-top)*.22f-lift*unit)
                    mark("weight-plates",stack,38f,24f)
                } else if(index==0) {
                    mark("anchor",anchor+Offset(0f,19f*unit),36f,49f)
                }
                val ripple = if(item.gear==DemoGear.BATTLE_ROPE)
                    middle(anchor,target)+Offset(0f,(kotlin.math.sin((phase+index*.5f)*2f*PI.toFloat())*36f+30f)*unit)
                    else null
                if (item.ropeGrip) {
                    val split = target + unitVector(anchor-target)*(35f*unit)
                    line(anchor,split,EquipmentStroke.CABLE)
                    for (wrist in wrists) {
                        gripped += wrist
                        line(split,grip(wrist),EquipmentStroke.ROPE)
                        mark("handle",grip(wrist),24f,23f)
                    }
                    continue
                }
                line(anchor,target,style,ripple)
                if(item.mount in setOf(GearMount.ANKLE,GearMount.HIP)) mark("band",target,41f,24f,foreground=true)
                else if(item.mount!=GearMount.KNEE && !band)
                    mark(if (item.gear==DemoGear.CABLE && item.mount==GearMount.SHARED) "bar" else "handle",
                        target,if(item.mount==GearMount.SHARED) 85f else 37f,26f)
            }
        }

        private fun lever(item: DemoApparatus) {
            val gear = item.gear
            when (gear) {
                DemoGear.LEG_PRESS -> {
                    val lowest = workingLeg.maxBy { it.y }
                    val farthest = workingLeg.maxBy { it.x*facing }
                    val diagonal = (lowest.y-farthest.y) > 36f*unit
                    val foot = p[8] + Offset(31f*facing*unit,2f*unit)
                    val railStart = rest[8]+Offset(-72f*facing*unit,39f*unit)
                    val railEnd = Offset(front,if (diagonal) farthest.y + 22f*unit else railStart.y)
                    line(railStart,railEnd)
                    line(railStart,railStart.copy(y=ground))
                    line(railEnd,railEnd.copy(y=ground))
                    mark("platform",foot,85f,27f,if(diagonal) 55f else 90f)
                    mark("plate",foot+Offset(35f*facing*unit,21f*unit),64f,44f)
                }
                DemoGear.GUIDED_SQUAT -> {
                    val support = middle(rest[8],rest[10]) + Offset(0f,17f*unit)
                    val slidingPad = if (item.mount == GearMount.HIP) hips else p[1]-Offset(0f,17f*unit)
                    val reverse = -62f*facing*unit
                    val track = Offset(rest[1].x+reverse, body.top - 51f*unit)
                    line(track,Offset(track.x+42f*facing*unit,ground))
                    line(Offset(track.x-27f*facing*unit,track.y),Offset(track.x+15f*facing*unit,ground))
                    mark("platform",support,141f,35f)
                    mark(if (item.mount == GearMount.HIP) "band" else "roller",slidingPad,94f,35f,foreground=true)
                    mark("plate",slidingPad+Offset(reverse,24f*unit),68f,46f)
                    line(slidingPad,slidingPad+Offset(reverse,24f*unit))
                }
                DemoGear.LEG_LEVER -> {
                    val hinge = rest[7]
                    val shin = unitVector(p[8]-p[7])
                    val shinFront = Offset(shin.y,-shin.x)*facing
                    val side = if (item.legRollerContact==LegRollerContact.BACK_SHIN) -1f else 1f
                    val pad = if (item.mount==GearMount.KNEE) p[7]-Offset(0f,16f*unit)
                        else p[8] - shin*(17f*unit) + shinFront*(side*15f*unit)
                    stand(rear,rest[2].y-43f*unit,width=65f)
                    line(hinge,Offset(hinge.x,ground))
                    line(hinge,pad)
                    mark("pulley",hinge,30f,25f)
                    mark("roller",pad,63f,34f,foreground=true)
                }
                DemoGear.HIP_LEVER -> {
                    stand(rear,rest[1].y-20f*unit,width=70f)
                    val hinge = rest[2]+Offset(-77f*facing*unit,8f*unit)
                    val pads = if (item.mount==GearMount.KNEE) listOf(p[7],p[9]) else listOf(hips-Offset(0f,15f*unit))
                    for (pad in pads) {
                        line(hinge,pad)
                        mark("roller",pad,if (pads.size == 1) 83f else 52f,33f,foreground=true)
                    }
                }
                DemoGear.ARM_LEVER -> {
                    val contact = item.armLeverContact
                    val inFront = contact == ArmLeverContact.HIGH_HAND || contact == ArmLeverContact.CHEST_SUPPORT
                    val base = if(inFront) front else rear
                    val height = if(contact==ArmLeverContact.HIGH_HAND || contact==ArmLeverContact.OVERHEAD_ELBOW)
                        84f else 48f
                    stand(base,body.top-25f*unit)
                    val hinge = Offset(base,rest[1].y-height*unit)
                    if(inFront) {
                        val chest = rest[1]+Offset(30f*facing*unit,42f*unit)
                        line(Offset(base,chest.y),chest)
                        mark("roller",chest,77f,38f,angle=90f)
                    }
                    for (index in wrists(item.mount)) {
                        gripped += index
                        val elbowPad=contact==ArmLeverContact.ELBOW_PAD || contact==ArmLeverContact.OVERHEAD_ELBOW
                        val end = if(elbowPad) p[index-1]+Offset(0f,9f*unit) else grip(index)
                        line(hinge,end)
                        if(elbowPad) mark("roller",end,47f,25f,foreground=true)
                        mark("handle",grip(index),41f,27f)
                    }
                    mark("pulley",hinge,30f,24f)
                }
                DemoGear.ASSISTED -> {
                    stand(rear,body.top-40f*unit)
                    mark("bar",middle(rest[4],rest[6]),158f,40f)
                    val support = middle(p[7],p[9])+Offset(0f,17f*unit)
                    line(Offset(rear,support.y),support)
                    mark("platform",support,134f,28f)
                }
                else -> Unit
            }
        }

        private fun cardio(gear: DemoGear) {
            val h = rest[2]
            when(gear) {
                DemoGear.BIKE, DemoGear.AIR_BIKE, DemoGear.RECUMBENT -> {
                    val left = h+Offset(-34f*facing*unit,24f*unit)
                    val grip = middle(rest[4],rest[6])
                    val hub = crank+Offset(83f*facing*unit,-18f*unit)
                    line(left,crank)
                    line(grip,hub)
                    line(crank,hub)
                    line(Offset(left.x,ground),Offset(hub.x+50f*facing*unit,ground))
                    mark("bench",left,88f,25f)
                    if(gear==DemoGear.RECUMBENT) mark("bench",left+Offset(-28f*facing*unit,-49f*unit),100f,34f,-75f*facing)
                    mark(gear.asset,hub,116f,90f,phase*360f)
                    mark("console",grip+Offset(24f*facing*unit,-21f*unit),58f,30f)
                    for(index in listOf(8,10)) {
                        val pedal=p[index]+Offset(8f*facing*unit,12f*unit)
                        line(crank,pedal)
                        mark("pedal",pedal,55f,23f)
                    }
                    if(gear==DemoGear.AIR_BIKE) for (index in wrists) {
                        gripped += index
                        line(hub,grip(index))
                        mark("handle",grip(index),44f,25f)
                    }
                }
                DemoGear.ROWER -> {
                    val rail = rest[2].y+42f*unit
                    val console = Offset(front,rest[4].y-27f*unit)
                    val base = Offset(front,rail)
                    line(Offset(rear,rail),base)
                    line(console,base)
                    mark("fan",base,86f,63f,phase*360f)
                    mark("bench",hips+Offset(0f,23f*unit),83f,41f)
                    mark("console",console,71f,38f)
                    mark("platform",rest[8]+Offset(14f*facing*unit,12f*unit),55f,26f,-47f*facing)
                    val hand = handCenter(GearMount.SHARED)
                    line(base+Offset(-20f*facing*unit,-17f*unit),hand,EquipmentStroke.CABLE)
                    mark("bar",hand,89f,30f)
                }
                DemoGear.TREADMILL, DemoGear.STEP_MILL -> {
                    val end = Offset(front,ground+5f*unit)
                    val panel = Offset(front-17f*facing*unit,rest[1].y+63f*unit)
                    line(end,panel)
                    mark("console",panel,81f,47f)
                    val width = (body.width/unit+100f).coerceAtLeast(200f)
                    if(gear==DemoGear.STEP_MILL) mark("stepmill",Offset(body.center.x,ground-38f*unit),width,83f)
                    else {
                        mark("platform",Offset(body.center.x,ground+10f*unit),width,33f)
                        for(column in 0..6) {
                            val x=body.center.x+(-.36f+(column+phase)*.10f)*width*unit*facing
                            line(Offset(x,ground+2f*unit),Offset(x+6f*facing*unit,ground+4f*unit),EquipmentStroke.CABLE)
                        }
                    }
                }
                DemoGear.ELLIPTICAL -> {
                    val wheel = Offset(front,ground-44f*unit)
                    mark("fan",wheel,118f,92f,phase*360f)
                    val control = Offset(front,rest[4].y-27f*unit)
                    line(wheel,control)
                    mark("console",control,73f,41f)
                    for((hand,ankle) in listOf(4 to 8,6 to 10)) {
                        gripped += hand
                        val pedal = p[ankle]+Offset(9f*facing*unit,13f*unit)
                        line(wheel,pedal)
                        line(pedal,grip(hand))
                        mark("pedal",pedal,78f,33f)
                        mark("handle",grip(hand),40f,24f)
                    }
                }
                else -> Unit
            }
        }

        private fun simpleBar(gear: DemoGear) {
            if(gear==DemoGear.STICK) {
                mark("bar",handCenter(GearMount.SHARED),143f,27f,angle(p[6]-p[4]))
                return
            }
            if(gear==DemoGear.PARALLETTES) {
                for (wrist in wrists) {
                    val hand=rest[wrist]
                    support("bench",hand,62f)
                }
                gripped += wrists
                return
            }
            val width = ((rest[4]-rest[6]).getDistance()/unit+85f).coerceAtLeast(140f)
            val bar = middle(rest[4],rest[6])
            val drop = if(gear==DemoGear.PULL_BAR) 23f*unit else 8f*unit
            mark("bar",bar,width,41f)
            for(sign in listOf(-1f,1f)) {
                val end = bar+Offset(width*.39f*sign*unit,0f)
                line(end,end.copy(y = if(overhead) end.y+drop else ground))
            }
            gripped += wrists
        }

        private fun jumpRope() {
            val first=grip(4)
            val second=grip(6)
            val angle=phase*2f*PI.toFloat()
            val y=(rest[1].y+rest[2].y)/2+kotlin.math.cos(angle)*(ground-body.top)*.55f
            val center=Offset(body.center.x+kotlin.math.sin(angle)*71f*unit,y)
            line(first,second,EquipmentStroke.ROPE,center*2f-middle(first,second))
            gripped += wrists
            mark("handle",first,27f,21f)
            mark("handle",second,27f,21f)
        }
    }
}

private fun middle(a: Offset,b: Offset) = (a+b)/2f
private fun unitVector(v: Offset): Offset = if (v.getDistance()>.01f) v/v.getDistance() else Offset(0f,1f)
private fun angle(v: Offset) = atan2(v.y,v.x)*180f/PI.toFloat()
