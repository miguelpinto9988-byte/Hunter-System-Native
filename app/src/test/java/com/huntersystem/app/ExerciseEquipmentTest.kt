package com.huntersystem.app

import android.content.Context
import androidx.compose.ui.geometry.Size
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExerciseEquipmentTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val catalog = Catalog(context)

    private fun motion(id: String) = checkNotNull(ExerciseMotions.load(context, catalog.byId.getValue(id)))

    @Test fun unilateralRowMovesOneDumbbellAndKeepsTheOtherHandOnTheTable() {
        val row = motion("row")
        val framing = ExerciseFraming.from(row)
        val start = framing.equipment.frame(row.at(0f),0f)
        val raised = framing.equipment.frame(row.at(.5f),.5f)
        val originalWeight = start.sprites.single { it.asset == "dumbbell" }
        val movedWeight = raised.sprites.single { it.asset == "dumbbell" }
        assertEquals(setOf(4), start.grippedWrists)
        assertTrue(movedWeight.center.y < originalWeight.center.y - 50)
        assertEquals(row.at(0f)[6],row.at(.5f)[6])
        assertEquals("table",framing.equipment.authoredSupports.single().kind)
        assertEquals(listOf("Mancuerna","Mesa de apoyo"),equipmentLabels(row))

        val double = motion("bentrow")
        val weights = ExerciseFraming.from(double).equipment.frame(double.at(.5f),.5f)
        assertEquals(2,weights.sprites.count { it.asset == "dumbbell" })
        assertEquals(setOf(4,6),weights.grippedWrists)
    }

    @Test fun towelWrapsTheFeetAndRemainsSteadyDuringTheIsometricHold() {
        for (id in listOf("towelrow","towelrow_short","towelrow_easy")) {
            val row = motion(id)
            assertTrue("towel" in catalog.byId.getValue(id).raw.a("materialRequirements").strings())
            val equipment = ExerciseFraming.from(row).equipment
            val start = equipment.frame(row.at(0f),0f)
            val hold = equipment.frame(row.at(.72f),.72f)
            assertTrue(row.staticHold)
            assertEquals(setOf(4,6),start.grippedWrists)
            assertEquals(listOf("Toalla"),equipmentLabels(row))
            assertEquals(3,start.sprites.count { it.asset == "towel" })
            assertEquals(start,hold)
            assertTrue(start.sprites.single { it.foreground }.center.x > row.at(0f)[8].x)
        }
    }

    @Test fun machineMountsStayFixedAndMovingPartsFollowTheUser() {
        val cable = motion("gym-cable-row23")
        val layout = ExerciseFraming.from(cable).equipment
        val first = layout.frame(cable.at(0f),0f)
        val pull = layout.frame(cable.at(.5f),.5f)
        val firstCord = first.links.single { it.stroke == EquipmentStroke.CABLE }
        val lastCord = pull.links.single { it.stroke == EquipmentStroke.CABLE }
        assertEquals(firstCord.start,lastCord.start)
        assertTrue((firstCord.end-lastCord.end).getDistance() > 20f)
        assertEquals(first.sprites.single { it.asset=="weight-stack" },pull.sprites.single { it.asset=="weight-stack" })
        assertTrue(pull.sprites.single { it.asset=="weight-plates" }.center.y <
            first.sprites.single { it.asset=="weight-plates" }.center.y)

        val extension = motion("gym-leg-extension23")
        val base = ExerciseFraming.from(extension).equipment
        val start = base.frame(extension.at(0f),0f)
        val end = base.frame(extension.at(.5f),.5f)
        assertEquals(start.sprites.single { it.asset=="weight-stack" },end.sprites.single { it.asset=="weight-stack" })
        assertTrue((start.sprites.single { it.asset=="roller" }.center-end.sprites.single { it.asset=="roller" }.center)
            .getDistance() > 40f)

        val cycling = motion("gym-airbike30")
        val bike = ExerciseFraming.from(cycling).equipment
        assertTrue(bike.authoredSupports.isEmpty())
        val cycleStart = bike.frame(cycling.at(0f),0f)
        val cycleQuarter = bike.frame(cycling.at(.25f),.25f)
        val wheel = cycleStart.sprites.single { it.asset=="fan" }
        val turned = cycleQuarter.sprites.single { it.asset=="fan" }
        assertEquals(wheel.center,turned.center)
        assertEquals(90f,turned.angle,.001f)
        assertTrue(cycleStart.sprites.first { it.asset=="pedal" }.center != cycleQuarter.sprites.first { it.asset=="pedal" }.center)
    }

    @Test fun sequencesLoadTheEquipmentForEachComponentRatherThanTheirCombinedList() {
        val sequence = catalog.byId.getValue("sport-seq-calistenia-towelrow-kickback")
        val parts = checkNotNull(ExerciseMotions.loadSequence(context,sequence,catalog))
        assertTrue(parts[0].apparatus.any { it.gear==DemoGear.TOWEL })
        assertFalse(parts[1].apparatus.any { it.gear==DemoGear.TOWEL })
        assertEquals(emptyList<String>(),equipmentLabels(motion("jumpjack")))
    }

    @Test fun hamstringSlidesKeepTheWorkingHeelOnTheTowelAndRequireItInTheProfile() {
        for (family in listOf("home-ham-slide23","home-ham-slide-one23")) {
            val single = family=="home-ham-slide-one23"
            for (suffix in listOf("","-tempo23","-pause23")) {
                val id=family+suffix
                val exercise=catalog.byId.getValue(id)
                val animation=motion(id)
                val stage=ExerciseFraming.from(animation)
                val rest=animation.at(0f)
                assertEquals(ExercisePosture.SUPINE,animation.posture)
                assertEquals(if(single) TechniqueDiagram.HAMSTRING_SLIDE_SINGLE else TechniqueDiagram.HAMSTRING_SLIDE,
                    techniqueDiagram(exercise,animation))
                for (index in 0..20) {
                    val phase=index/20f
                    val pose=animation.at(phase)
                    assertEquals(rest[1],pose[1])
                    assertTrue(id,pose[2].y < pose[1].y-25f)
                    assertEquals(rest[8].y,pose[8].y,.001f)
                    if(single)assertTrue(id,pose[10].y < pose[8].y-70f)
                    else assertEquals(rest[10].y,pose[10].y,.001f)
                    val towel=stage.equipment.frame(pose,phase).sprites.single { it.asset=="towel" }
                    assertEquals(pose[8].x-8f*stage.unit,towel.center.x,.01f)
                }
                val rules=Rules(catalog)
                val basic=obj("equipment" to "body")
                assertTrue(rules.materialReasons(exercise,basic).contains("Falta material: Toalla"))
                basic.put("materials46",arr(listOf("towel")))
                assertFalse(rules.materialReasons(exercise,basic).contains("Falta material: Toalla"))
            }
        }
        val sequence=catalog.byId.getValue("sport-seq-fuerza-kickback-home-ham-slide-one23")
        assertTrue("towel" in sequence.raw.a("materialRequirements").strings())
        assertTrue("towel" in profileAllowedMaterials("body"))
    }

    @Test fun theTowelGatherAndTheReverseSliderMoveOnlyWithTheWorkingFoot() {
        val curl=motion("new46-toetowel")
        val curlStage=ExerciseFraming.from(curl).equipment
        val open=curlStage.frame(curl.at(0f),0f).sprites.single { it.asset=="towel" }
        val gathered=curlStage.frame(curl.at(.5f),.5f).sprites.single { it.asset=="towel" }
        assertFalse(curl.staticHold)
        assertEquals(curl.at(0f),curl.at(.5f))
        assertTrue(gathered.size.width < open.size.width && gathered.center.x < open.center.x)
        assertEquals(open.center.y,gathered.center.y,.001f)

        val lunge=motion("new46-slidereverse")
        val lungeStage=ExerciseFraming.from(lunge).equipment
        val back=lunge.at(.5f)
        assertEquals(lunge.at(0f)[10],back[10])
        assertTrue(back[8].x < lunge.at(0f)[8].x-30f)
        assertEquals(back[8].x,lungeStage.frame(back,.5f).sprites.single { it.asset=="slider" }.center.x,.001f)
    }

    @Test fun theEquipmentDistinguishesTheMachineRollerContactsAndBothRenegadeGrips() {
        for (id in listOf("gym-leg-extension23","gym-ham-seated23","gym-ham-lying23")) {
            val exercise=motion(id)
            val stage=ExerciseFraming.from(exercise).equipment
            for (phase in listOf(0f,.25f,.5f,.75f)) {
                val pose=exercise.at(phase)
                val frame=stage.frame(pose,phase)
                val pad=frame.sprites.single { it.asset=="roller" }.center
                val shin=pose[8]-pose[7]
                val contact=pad-pose[8]
                val relative=(shin.y*contact.x-shin.x*contact.y) * if(exercise.facing=="left") -1f else 1f
                assertTrue(id,if(id=="gym-leg-extension23") relative>0f else relative<0f)
                assertEquals(exercise.at(0f)[7],frame.sprites.single { it.asset=="pulley" }.center)
            }
        }
        for (id in listOf("home-renegade23","home-renegade-knee23")) {
            val workout=motion(id)
            val frame=ExerciseFraming.from(workout).equipment.frame(workout.at(.25f),.25f)
            assertEquals(2,frame.sprites.count { it.asset=="dumbbell" })
            assertEquals(setOf(4,6),frame.grippedWrists)
        }
    }

    @Test fun overheadArmSweepsAndPulloversMoveAgainstTheirOwnSupports() {
        val prone=motion("reverse")
        val stage=ExerciseFraming.from(prone).equipment
        val start=prone.at(0f)
        val end=prone.at(.43f)
        assertEquals(start.take(3),end.take(3))
        assertTrue(end[4].y > start[4].y+60f)
        assertEquals(stage.frame(start,0f).sprites.single { it.asset=="towel" },
            stage.frame(end,.43f).sprites.single { it.asset=="towel" })
        assertEquals(GearMount.FOREHEAD,motion("prone").apparatus.single { it.gear==DemoGear.TOWEL }.mount)

        val pullover=motion("gym-pulloverm30")
        val machine=ExerciseFraming.from(pullover).equipment
        val raised=pullover.at(0f)
        val lowered=pullover.at(.5f)
        assertEquals(raised.take(3),lowered.take(3))
        assertTrue(lowered[3].y > raised[3].y+110f)
        assertEquals(2,machine.frame(lowered,.5f).sprites.count { it.asset=="roller" && it.foreground })
        val supportedRow=motion("gym-supportrow30")
        assertEquals(ArmLeverContact.CHEST_SUPPORT,
            supportedRow.apparatus.single { it.gear==DemoGear.ARM_LEVER }.armLeverContact)
        val highRow=motion("gym-highrow30")
        assertTrue(highRow.at(0f)[4].y < highRow.at(0f)[1].y - 75f)
        assertTrue(highRow.at(.5f)[4].y > highRow.at(0f)[4].y + 115f)

        val bandPallof=catalog.byId.getValue("hx_core_0089")
        assertEquals(setOf("band","anchor"),bandPallof.raw.a("materialRequirements").strings().toSet())
        assertEquals(setOf("kb"),catalog.byId.getValue("hx_lower_0003").raw.a("materialRequirements").strings().toSet())
    }

    @Test fun mappedEquipmentFitsOneStableFrameAndUsesPackagedVectorAssets() {
        val used = mutableSetOf<String>()
        val equipmentExercises = catalog.historicalExercises.filter {
            it.raw.a("sequenceParts").length()==0
        }
        for (exercise in equipmentExercises) {
            val motion = checkNotNull(ExerciseMotions.load(context,exercise)) { exercise.id }
            val framing = ExerciseFraming.from(motion)
            val scale = framing.scaleFor(Size(288f,176f),closeFraming=true)
            assertTrue(exercise.id,scale.isFinite() && scale > 0f)
            for (phase in listOf(0f,.37f,.81f)) {
                val frame = framing.equipment.frame(motion.at(phase),phase)
                used.addAll(frame.sprites.map { it.asset })
                for (sprite in frame.sprites) {
                    val bounds = sprite.bounds
                    assertTrue(exercise.id,bounds.left >= framing.bounds.left && bounds.right <= framing.bounds.right &&
                        bounds.top >= framing.bounds.top && bounds.bottom <= framing.bounds.bottom)
                }
            }
        }
        assertTrue(used.containsAll(listOf("towel","dumbbell","weight-stack","fan","roller","barbell")))
        val assets = context.assets.list("exercise-actor/equipment").orEmpty().map { it.removeSuffix(".svg") }.toSet()
        assertTrue(assets.containsAll(used))
        for (asset in assets) assertNotNull(asset,EquipmentSvg.load(context,asset))
    }
}
