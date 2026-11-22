package com.huntersystem.app

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExercisePerspectiveTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun frontExerciseAndFloorExercisesUseTheirOwnProjection() {
        val jumpingJack = checkNotNull(ExerciseMotions.load(context, "jumpjack"))
        assertEquals(ExerciseView.FRONT, jumpingJack.view)
        assertFalse(jumpingJack.handSupport)

        for (id in listOf("home-pushwide30", "home-push-one23")) {
            val motion = checkNotNull(ExerciseMotions.load(context, id))
            assertEquals(ExerciseView.OVERHEAD, motion.view)
            assertTrue(motion.prone)
            assertTrue(motion.handSupport)
        }
        for (id in listOf("pushup", "kneepush", "home-scappush30")) {
            val motion = checkNotNull(ExerciseMotions.load(context, id))
            assertEquals(ExerciseView.SIDE, motion.view)
            assertTrue(motion.prone)
            assertTrue(motion.handSupport)
        }
    }
    @Test fun aSupineBridgeKeepsTheGazeUpAndShoesGrounded() {
        val motion = checkNotNull(ExerciseMotions.load(context,"bridge"))
        assertEquals(ExercisePosture.SUPINE,motion.posture)
        assertEquals(motion.at(.5f),motion.at(2.5f))
        assertFalse(motion.at(0f)[2]==motion.at(.5f)[2])
        for (step in 0..20) {
            val pose = motion.at(step/20f)
            val head = ExercisePoseCommands.head(motion,pose)
            assertFalse(head.left)
            assertEquals(-90f,head.rotation,.001f)
            val foot = ExercisePoseCommands.foot(motion,pose,pose[7],pose[8])
            assertTrue(foot.x>.99f && kotlin.math.abs(foot.y)<.001f)
            assertEquals(motion.frames.first()[8],pose[8])
            assertEquals(motion.frames.first()[10],pose[10])
        }
        assertFalse(checkNotNull(ExerciseMotions.load(context,"prone")).handSupport)
    }

    @Test fun pelvicTiltMovesAndDiagramExplainsDiamondHandPlacement() {
        val catalog=Catalog(context)
        val pelvic=checkNotNull(ExerciseMotions.load(context,"new46-pelvictilt"))
        assertFalse(pelvic.frames.first()[2]==pelvic.at(.25f)[2])
        assertEquals(pelvic.frames.first()[8],pelvic.at(.25f)[8])
        val diamond=catalog.byId.getValue("home-diamond23")
        val diamondMotion=checkNotNull(ExerciseMotions.load(context,diamond.id))
        assertEquals(TechniqueDiagram.DIAMOND,techniqueDiagram(diamond,diamondMotion))
        val first=diamondMotion.frames.first()
        val second=diamondMotion.at(.5f)
        assertTrue(first[4].y>first[1].y && first[6].y>first[1].y)
        assertEquals(first[4],second[4])
        assertEquals(first[6],second[6])
        val neck=catalog.byId.getValue("neck")
        assertEquals(TechniqueDiagram.NECK,techniqueDiagram(neck,ExerciseMotions.load(context,neck.id)))
    }

    @Test fun rebuiltFloorExercisesHonorTheirMovementAndContacts() {
        val vUp=checkNotNull(ExerciseMotions.load(context,"home-vup23-tempo23"))
        assertEquals(AuthoredFloorMovement.V_UP,vUp.authoredFloor)
        assertEquals(4.8f,vUp.duration,.001f)
        val initial=vUp.at(0f)
        val peak=vUp.at(1.35f/vUp.duration)
        assertEquals(initial[2],peak[2])
        assertTrue(peak[1].y < initial[1].y-100f)
        assertTrue(peak[8].y < initial[8].y-100f)

        val clam=checkNotNull(ExerciseMotions.load(context,"new46-reverseclam"))
        assertEquals(ExerciseView.SIDE,clam.view)
        assertEquals(ExercisePosture.SIDE_LYING,clam.posture)
        val rest=clam.at(0f)
        val rotated=clam.at(.46f)
        assertTrue(rest[8].x<rest[7].x && rest[10].x<rest[9].x)
        for(index in listOf(2,7,8,9))assertEquals(rest[index],rotated[index])
        assertTrue(rotated[10].y < rest[10].y-25f)

        val kickback=checkNotNull(ExerciseMotions.load(context,"kickback"))
        val lower=kickback.at(0f)
        val raised=kickback.at(.46f)
        for(index in listOf(4,6,9,10))assertEquals(lower[index],raised[index])
        assertTrue(raised[7].x > lower[7].x+100f)
        val catalog=Catalog(context)
        val sequence=catalog.historicalExercises.first { it.raw.a("sequenceParts").strings()==listOf("kickback","new46-reverseclam") }
        assertEquals(listOf(AuthoredFloorMovement.KICKBACK,AuthoredFloorMovement.REVERSE_CLAM),
            ExerciseMotions.loadSequence(context,sequence)?.map { it.authoredFloor })
    }

    @Test fun seatedToeLiftsMoveTheToesWithStableLegsAndHonorTheVariantTiming() {
        val catalog = Catalog(context)
        val ids = listOf("toelift","toelift_slow","toelift_pause",
            "gym-tibialis30","gym-tibialis30-ecc30","gym-tibialis30-hold30")
        for (id in ids) {
            val motion = checkNotNull(ExerciseMotions.load(context,id))
            val action = checkNotNull(motion.toeLift)
            val peakPhase = (action.rise + action.pause / 2f) / motion.duration
            val rest = motion.at(0f)
            assertEquals(ExerciseView.SIDE,motion.view)
            assertEquals(TechniqueDiagram.TOE_LIFT,techniqueDiagram(catalog.byId.getValue(id),motion))
            for ((knee,ankle) in listOf(7 to 8,9 to 10)) {
                assertEquals(rest[knee],motion.at(peakPhase)[knee])
                assertEquals(rest[ankle],motion.at(peakPhase)[ankle])
                assertEquals(0f,ExercisePoseCommands.foot(motion,rest,rest[knee],rest[ankle]).y,.001f)
                assertTrue(ExercisePoseCommands.foot(motion,rest,rest[knee],rest[ankle],phase=peakPhase).y < -.4f)
                assertEquals(0f,ExercisePoseCommands.foot(motion,rest,rest[knee],rest[ankle],phase=1f).y,.001f)
            }
            if (id.endsWith("-ecc30")) assertEquals(3f,action.lower,.001f)
            if (id.endsWith("-hold30")) assertEquals(2f,action.pause,.001f)
        }
    }

    @Test fun frontLoadedSquatsUseDifferentImplementsEvenWhenTheirPosesMatch() {
        val examples = mapOf("gym-front-bb23" to "barbell","doublefront" to "dumbbells","goblet" to "goblet-weight")
        for ((id,equipment) in examples) assertEquals(equipment,ExerciseMotions.load(context,id)?.equipment)
    }

    @Test fun proneLiftsMoveOnlyTheIntendedLimbs() {
        for (id in listOf("prone","prone_slow","prone_pause","contralateral")) {
            val motion = checkNotNull(ExerciseMotions.load(context,id))
            val initial = motion.at(0f)
            val firstLift = motion.at(.25f)
            val secondLift = motion.at(.75f)
            assertFalse(motion.handSupport)
            assertEquals(id,initial.take(3),firstLift.take(3))
            assertEquals(id,initial.take(3),secondLift.take(3))
            assertTrue(id,initial[4] != firstLift[4])
            if (id == "contralateral") {
                assertEquals(initial[6],firstLift[6])
                assertEquals(initial[8],firstLift[8])
                assertTrue(initial[10] != firstLift[10])
                assertEquals(initial[4],secondLift[4])
                assertEquals(initial[10],secondLift[10])
                assertTrue(initial[6] != secondLift[6])
                assertTrue(initial[8] != secondLift[8])
            } else {
                assertTrue(initial[6] != firstLift[6])
                assertEquals(initial.drop(7),firstLift.drop(7))
            }
        }
    }

    @Test fun wristAndFingerGesturesMoveWhileKeepingTheirForearmsAnchored() {
        val catalog = Catalog(context)
        val wrist = checkNotNull(ExerciseMotions.load(context,"wrist"))
        val fingers = checkNotNull(ExerciseMotions.load(context,"fingers"))
        for (motion in listOf(wrist,fingers)) assertEquals(motion.at(0f),motion.at(.5f))
        val wristGesture = checkNotNull(wrist.handGesture)
        val forward = Offset(0f,1f)
        assertTrue(wristGesture.direction(forward,.25f,true).x < 0f)
        assertTrue(wristGesture.direction(forward,.75f,true).x > 0f)
        assertTrue(wristGesture.projectedLength(.5f) < wristGesture.projectedLength(0f))
        val fingerGesture = checkNotNull(fingers.handGesture)
        assertEquals(0f,fingerGesture.curl(0f),.001f)
        assertEquals(1f,fingerGesture.curl(.5f),.001f)
        assertEquals(TechniqueDiagram.FINGERS,techniqueDiagram(catalog.byId.getValue("fingers"),fingers))
    }

    @Test fun adductionAndHipRotationKeepTheirDifferentSupportContacts() {
        val catalog = Catalog(context)
        val hold = checkNotNull(ExerciseMotions.load(context,"new46-copenhagen"))
        assertTrue(hold.staticHold)
        assertEquals(hold.at(0f),hold.at(.5f))
        assertEquals(TechniqueDiagram.COPENHAGEN_HOLD,techniqueDiagram(catalog.byId.getValue("new46-copenhagen"),hold))
        for (id in listOf("new46-copenhagendyn","new46-sideadduct")) {
            val motion = checkNotNull(ExerciseMotions.load(context,id))
            assertFalse(motion.staticHold)
            assertEquals(ExerciseView.SIDE,motion.view)
            val rest = motion.at(0f)
            val peak = motion.at(.46f)
            for (index in listOf(0,1,2,3,4,9,10)) assertEquals(id,rest[index],peak[index])
            assertTrue(id,peak[8].y < rest[8].y - 40f)
            assertEquals(id == "new46-copenhagendyn",motion.props.any { it.kind=="bench" })
        }
        val rotation = checkNotNull(ExerciseMotions.load(context,"new46-pronehipir"))
        val rest = rotation.at(0f)
        val peak = rotation.at(.46f)
        assertEquals(ExerciseView.OVERHEAD,rotation.view)
        for (index in listOf(2,7,9)) assertEquals(rest[index],peak[index])
        assertTrue((rest[8] - rest[7]).getDistance() > 50f)
        assertTrue((rest[10] - rest[9]).getDistance() > 50f)
        assertTrue(peak[10].x - peak[8].x > rest[10].x - rest[8].x + 80f)
    }

    @Test fun sportsExpansionLoadsFromTheCatalogWithAllThreeDifficultyLevels() {
        val catalog = Catalog(context)
        val expansion = catalog.historicalExercises.filter { it.id.startsWith("sport-ampl-") }
        val disciplines = setOf("Calistenia", "Fútbol", "Atletismo", "Movilidad", "Fuerza")
        assertEquals(disciplines, expansion.map { it.sport }.toSet())
        for ((_, group) in expansion.groupBy { it.sport }) {
            assertEquals(setOf(0, 1, 2), group.map { it.planningLevel }.toSet())
        }
        for (exercise in expansion) {
            assertFalse(exercise.validationBlocked)
            val motion = checkNotNull(ExerciseMotions.load(context, exercise.id)) { exercise.id }
            assertTrue(motion.frames.distinct().size > 1)
            val framing = ExerciseFraming.from(motion)
            assertTrue(framing.scaleFor(Size(200f, 320f)) > 0f)
        }
    }

    @Test fun everyStandaloneExerciseLoadsThroughTheApprovedRenderer() {
        val catalog = Catalog(context)
        val standalone = catalog.historicalExercises.filter { it.raw.a("sequenceParts").length() == 0 }
        assertEquals(2_031, standalone.size)
        for (exercise in standalone) {
            val motion = checkNotNull(ExerciseMotions.load(context, exercise.id)) { exercise.id }
            assertTrue(exercise.id, ExerciseFraming.from(motion).scaleFor(Size(320f, 220f)) > 0f)
        }
    }

    @Test fun everyNewSequenceLoadsWithItsOwnPerspectiveAndACommonScale() {
        val catalog = Catalog(context)
        assertEquals(5_000, catalog.historicalExercises.size)
        assertEquals("new46-tke", catalog.byId.getValue("hx_lower_0019").id)
        assertTrue(catalog.historicalExercises.none { it.id == "hx_lower_0019" })
        val sequences = catalog.historicalExercises.filter { it.id.startsWith("sport-seq-") }
        assertEquals(2_969, sequences.size)
        val pairs = mutableSetOf<List<String>>()
        val replacements = mapOf("Fuerza" to listOf(0), "Calistenia" to listOf(1, 2),
            "Atletismo" to listOf(1))
        for ((discipline, exercises) in sequences.groupBy { it.sport }) {
            val levels = listOf(208, 223, 162).mapIndexed { index, count ->
                index to (count + if (index in replacements[discipline].orEmpty()) 1 else 0)
            }.toMap()
            assertEquals(levels, exercises.groupingBy { it.planningLevel }.eachCount())
        }
        assertEquals("sport-ampl-pushup-staggered-hands", catalog.byId.getValue("home-staggered23").id)
        assertEquals("sport-ampl-track-a-skip", catalog.byId.getValue("new46-askip").id)
        assertEquals("new46-scapularhang", catalog.byId.getValue("sport-calisthenics-scapula").id)
        for (exercise in sequences) {
            assertFalse(exercise.validationBlocked)
            val pair = exercise.raw.a("sequenceParts").strings().map {
                val component = catalog.byId.getValue(it)
                component.raw.s("baseId", component.id)
            }.sorted()
            assertTrue(exercise.id, pairs.add(pair))
            val components = checkNotNull(ExerciseMotions.loadSequence(context, exercise)) { exercise.id }
            assertEquals(2, components.size)
            val stage = Size(208f, 320f)
            val frames = components.map(ExerciseFraming::from)
            val unitScale = frames.minOf { it.scaleFor(stage) * it.unit }
            assertTrue(exercise.id, unitScale.isFinite() && unitScale > 0f)
            for (frame in frames) {
                assertTrue(frame.bounds.width * unitScale / frame.unit < stage.width)
                assertTrue(frame.bounds.height * unitScale / frame.unit < stage.height)
            }
        }
    }
}
