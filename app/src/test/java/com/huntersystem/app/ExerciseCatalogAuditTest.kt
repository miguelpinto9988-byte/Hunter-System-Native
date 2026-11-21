package com.huntersystem.app

import android.content.Context
import androidx.compose.ui.geometry.Size
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.acos
import kotlin.math.PI

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExerciseCatalogAuditTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val catalog = Catalog(context)

    @Test fun everyCanonicalEntryResolvesItsOwnDemonstrationOrBothSequenceComponents() {
        val sources = linkedMapOf<String, JSONObject>()
        val cards = catalog.historicalExercises
        val samples = listOf(0f,.12f,.25f,.38f,.5f,.62f,.75f,.88f)
        val report = System.getenv("HUNTER_EXERCISE_AUDIT")?.let(::File)
        fun inspect(id: String): JSONObject = sources.getOrPut(id) {
            val exercise = catalog.byId.getValue(id)
            val motion = if (id == exercise.id) ExerciseMotions.load(context,exercise)
                else ExerciseMotions.load(context,id)?.let {
                    it.copy(apparatus=ExerciseEquipmentPlan.forExercise(exercise,it))
                }
            checkNotNull(motion) { id }
            val framing = ExerciseFraming.from(motion)
            assertTrue(id,framing.scaleFor(Size(288f,176f),true) > 0f)
            val poses = samples.map(motion::at)
            val frames = samples.zip(poses).map { (phase,pose) -> framing.equipment.frame(pose,phase) }
            val first = poses.first()
            val span = (first[2]-first[1]).getDistance()
            val distal = listOf(3 to 4,5 to 6,7 to 8,9 to 10)
            val movingLandmarks = first.indices.filter { i ->
                poses.any { pose -> (pose[i]-first[i]).getDistance() >= span*.035f }
            }
            val shortSegment = motion.frames.any { pose ->
                distal.any { (a,b) -> (pose[a]-pose[b]).getDistance() < span*.035f }
            }
            fun angularRange(root: Int, joint: Int, end: Int): Float {
                val values = poses.mapNotNull { pose ->
                    val upper = pose[root]-pose[joint]
                    val lower = pose[end]-pose[joint]
                    val scale = upper.getDistance()*lower.getDistance()
                    if(scale<.01f) null else acos(((upper.x*lower.x+upper.y*lower.y)/scale).coerceIn(-1f,1f))
                }
                return if(values.isEmpty()) 0f else (values.max()-values.min())*180f/PI.toFloat()
            }
            val gear = (motion.apparatus.map { it.gear } +
                framing.equipment.authoredSupports.mapNotNull { ExerciseEquipmentPlan.forProp(it.kind) }).distinct()
            obj("canonica" to exercise.id,"vista" to motion.view.name,
                "isometrico" to motion.staticHold,"huella" to motion.frames.hashCode().toString(),
                "puntos_moviles" to arr(movingLandmarks),"segmento_corto" to shortSegment,
                "rango_codo" to maxOf(angularRange(1,3,4),angularRange(1,5,6)),
                "rango_rodilla" to maxOf(angularRange(2,7,8),angularRange(2,9,10)),
                "equipo_movimiento" to (frames.distinct().size > 1),
                "gesto_de_detalle" to (motion.toeLift!=null || motion.handGesture!=null ||
                    motion.authoredFloor==AuthoredFloorMovement.TOE_TOWEL),
                "equipo" to arr(gear.map { it.name }),"etiquetas" to arr(equipmentLabels(motion)),
                "svg" to arr(frames.flatMap { it.sprites.map(EquipmentSprite::asset) }
                    .plus(framing.equipment.authoredSupports.mapNotNull { ExerciseEquipmentPlan.forProp(it.kind)?.asset })
                    .distinct()))
        }
        val visible = catalog.exercises.mapTo(hashSetOf()) { it.id }
        val items = JSONArray()
        for (exercise in cards) {
            val ids = exercise.raw.a("sequenceParts").strings().ifEmpty { listOf(exercise.id) }
            assertTrue(exercise.id,ids.size in 1..2 && ids.distinct().size==ids.size)
            for (id in ids) inspect(id)
            if (report!=null) items.put(obj("id" to exercise.id,"visible" to (exercise.id in visible),
                "fuentes" to arr(ids)))
        }
        assertEquals(5000,cards.size)
        if (report!=null) {
            report.parentFile?.mkdirs()
            val byId = JSONObject()
            for ((id,info) in sources) byId.put(id,info)
            report.writeText(obj("fichas" to items,"fuentes" to byId).toString())
        }
    }
}
