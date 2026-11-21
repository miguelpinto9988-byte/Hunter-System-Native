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
