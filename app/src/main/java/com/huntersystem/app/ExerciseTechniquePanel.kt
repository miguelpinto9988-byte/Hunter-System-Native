package com.huntersystem.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.min

internal enum class TechniqueDiagram(val title: String, val cue: String) {
    DIAMOND("Manos en diamante", "Vista desde arriba. Acerca los pulgares y los índices bajo el pecho para formar un rombo. Mantén los dedos orientados hacia delante."),
    BRIDGE("Dirección de la cadera", "Apoya la espalda alta y las plantas de los pies. Mira al techo y eleva la pelvis hacia arriba con control. Baja por el mismo recorrido."),
    PELVIS("Basculación pélvica", "Alterna una inclinación pequeña de la pelvis hacia delante y hacia atrás. Usa un recorrido cómodo y mantén el pecho estable."),
    HINGE("Bisagra de cadera", "Envía la cadera hacia atrás con las rodillas ligeramente flexionadas. Mantén el apoyo de los pies y vuelve a extender la cadera."),
    STANCE("Apoyo visto desde arriba", "Empieza con un apoyo estable. Orienta las rodillas en la dirección de las puntas de los pies al bajar."),
    SPLIT("Colocación de los pies", "Vista desde arriba. Coloca un pie delante y el otro detrás, dejando un espacio lateral entre ambos para mantener el equilibrio."),
    LATERAL("Desplazamiento lateral", "Separa los pies para el paso lateral. Desplaza la pelvis hacia la pierna que se flexiona, con su rodilla en la dirección de la punta del pie."),
    NECK("Giro suave del cuello", "Gira la cara hacia un lado y vuelve al centro. Alterna hacia el otro lado con los hombros quietos."),
    ANKLE("Movimiento del tobillo", "Mueve la punta del pie en el sentido indicado en la ficha. El movimiento es pequeño y se concentra en el tobillo."),
    WRIST("Movimiento de la muñeca", "Mantén el antebrazo estable y mueve la mano desde la muñeca en el sentido indicado en la ficha."),
    OPPOSITE("Brazo y pierna contrarios", "Boca abajo, eleva un brazo y la pierna contraria ligeramente. Baja y alterna al otro lado con el tronco estable."),
    SCAPULA("Brazos en W", "Boca abajo, dobla los codos formando una W. Eleva suavemente las manos y los codos al acercar los omóplatos. Baja con control."),
    REFERENCE("Detalle de la posición", "La figura muestra la posición de referencia. Sigue la indicación de la ficha para el gesto pequeño o la posición sostenida.")
}

internal fun techniqueDiagram(exercise: Exercise, motion: ExerciseMotion?, subject: String = exercise.id): TechniqueDiagram? {
    val base = if(subject==exercise.id) "${exercise.id} ${exercise.oldFamily} ${normalize(exercise.name)}" else subject
    return when {
        "contralateral" in base && motion?.prone == true && motion.needsPlacementGuide -> TechniqueDiagram.OPPOSITE
        subject in setOf("prone","prone_slow","prone_pause") -> TechniqueDiagram.SCAPULA
        "diamond" in base || "diamante" in base -> TechniqueDiagram.DIAMOND
        "pelvictilt" in base || "basculacion pelvica" in base || "inclinacion pelvica" in base -> TechniqueDiagram.PELVIS
        "bridge" in base || "puente de gluteo" in base || "hipthrust" in base || "hip thrust" in base -> TechniqueDiagram.BRIDGE
        ("rdl" in base || "hinge" in base || "bisagra" in base || "peso muerto" in base) &&
            "traccion" !in base && "remo" !in base -> TechniqueDiagram.HINGE
        "lateralunge" in base || "zancada lateral" in base -> TechniqueDiagram.LATERAL
        "bulgarian" in base || "bulgara" in base || "sentadilla dividida" in base ||
            "zancada" in base -> TechniqueDiagram.SPLIT
        subject in setOf("neck","neck_easy","neck_short") -> TechniqueDiagram.NECK
        "chair" == subject || "sentadilla libre" in base || "sentadilla a silla" in base -> TechniqueDiagram.STANCE
        "ankle" in base || "movilidad de tobillos" in base || "dorsiflexion" in base -> TechniqueDiagram.ANKLE
        "wrist" in base || "movilidad de munecas" in base -> TechniqueDiagram.WRIST
        motion?.needsPlacementGuide == true -> TechniqueDiagram.REFERENCE
        else -> null
    }
}

@Composable
internal fun ExerciseTechniquePanel(exercise: Exercise, providedMotion: ExerciseMotion? = null,
                                    subject: String = exercise.id) {
    val context=LocalContext.current
    val motion=remember(context,subject,providedMotion) { providedMotion ?: ExerciseMotions.load(context,subject) }
    val diagram=remember(exercise.id,motion,subject) { techniqueDiagram(exercise,motion,subject) } ?: return
    val colors=LocalHunter.current
    val detail=when(diagram) {
        TechniqueDiagram.REFERENCE, TechniqueDiagram.ANKLE, TechniqueDiagram.WRIST ->
            if(subject==exercise.id) exercise.raw.a("instructions").strings().take(3).joinToString(" ").ifBlank {
                exercise.raw.s("steps").ifBlank { exercise.raw.s("cue") }
            } else ""
        else -> ""
    }
    HPanel(decor=false,padding=10) {
        HText(diagram.title,size=17,bold=true)
        Canvas(Modifier.fillMaxWidth().height(98.dp).semantics { contentDescription=diagram.title+". "+diagram.cue }) {
            val factor=min(size.width/260f,size.height/93f)
            translate((size.width-260f*factor)/2f,(size.height-93f*factor)/2f) {
                scale(factor,factor,pivot=Offset.Zero) {
                    drawTechnique(diagram,colors.text,colors.accent,motion)
                }
            }
        }
        HText(diagram.cue,size=14,color=colors.muted)
        if(detail.isNotBlank()) HText(detail,size=14)
    }
}

private fun DrawScope.drawTechnique(diagram: TechniqueDiagram, ink: Color, accent: Color, motion: ExerciseMotion?) {
    fun line(a: Offset,b: Offset,color: Color=ink,w: Float=3f) =
        drawLine(color,a,b,w,StrokeCap.Round)
    fun chain(points: List<Offset>,color: Color=ink,w: Float=3f) {
        points.zipWithNext().forEach { (a,b) -> line(a,b,color,w) }
    }
    fun arrow(from: Offset,to: Offset) {
        val axis=(to-from)/(to-from).getDistance().coerceAtLeast(1f)
        val side=Offset(-axis.y,axis.x)
        line(from,to,accent)
        chain(listOf(to-axis*10f+side*5f,to,to-axis*10f-side*5f),color=accent)
    }
    when(diagram) {
        TechniqueDiagram.DIAMOND -> {
            for(side in listOf(-1f,1f)) {
                fun p(x:Float,y:Float)=Offset(130f+side*x,y)
                val hand=Path().apply {
                    moveTo(p(41f,78f).x,78f)
                    for((x,y) in listOf(50f to 52f,48f to 22f,42f to 19f,38f to 37f,
                        33f to 14f,27f to 14f,26f to 35f,18f to 12f,12f to 13f,
                        18f to 37f,4f to 31f,0f to 35f,14f to 49f,0f to 63f,
                        6f to 68f,23f to 61f,29f to 79f)) lineTo(p(x,y).x,y)
                    close()
                }
                drawPath(hand,ink.copy(alpha=.21f))
                drawPath(hand,ink,style=androidx.compose.ui.graphics.drawscope.Stroke(2f))
            }
            chain(listOf(Offset(130f,35f),Offset(145f,49f),Offset(130f,63f),Offset(115f,49f),
                Offset(130f,35f)),color=accent,w=2f)
            arrow(Offset(204f,65f),Offset(204f,25f))
        }
        TechniqueDiagram.BRIDGE -> {
            line(Offset(22f,78f),Offset(231f,78f),ink.copy(alpha=.36f))
            drawCircle(ink,9f,Offset(50f,65f))
            val shoulder=Offset(64f,69f)
            val hip=Offset(128f,33f)
            val knee=Offset(173f,27f)
            val foot=Offset(194f,76f)
            chain(listOf(shoulder,Offset(131f,72f),knee,foot),color=ink.copy(alpha=.37f))
            chain(listOf(shoulder,hip,knee,foot,Offset(216f,76f)))
            drawCircle(accent,5f,hip)
            arrow(Offset(124f,61f),Offset(124f,39f))
        }
        TechniqueDiagram.PELVIS -> {
            val center=Offset(130f,47f)
            chain(listOf(Offset(100f,29f),Offset(110f,59f),Offset(130f,68f),
                Offset(150f,59f),Offset(160f,29f)),w=4f)
            line(Offset(102f,40f),Offset(158f,54f),ink.copy(alpha=.32f))
            line(Offset(102f,54f),Offset(158f,40f),accent)
            drawCircle(accent,5f,center)
            arrow(Offset(180f,61f),Offset(180f,33f))
            arrow(Offset(80f,33f),Offset(80f,61f))
        }
        TechniqueDiagram.HINGE -> {
            drawCircle(ink,8f,Offset(178f,19f))
            chain(listOf(Offset(166f,29f),Offset(106f,51f),Offset(139f,65f),Offset(136f,80f),Offset(154f,80f)))
            chain(listOf(Offset(145f,23f),Offset(145f,51f),Offset(139f,65f)),color=ink.copy(alpha=.35f))
            drawCircle(accent,5f,Offset(106f,51f))
            arrow(Offset(143f,50f),Offset(84f,50f))
        }
        TechniqueDiagram.STANCE, TechniqueDiagram.SPLIT, TechniqueDiagram.LATERAL -> {
            val gap=if(diagram==TechniqueDiagram.LATERAL) 54f else 29f
            for(side in listOf(-1f,1f)) {
                val x=130f+side*gap
                val y=if(diagram==TechniqueDiagram.SPLIT) 17f*side else 0f
                line(Offset(x,70f+y),Offset(x,33f+y),ink,15f)
                line(Offset(x,33f+y),Offset(x+side*7f,24f+y),ink,13f)
                if(diagram!=TechniqueDiagram.LATERAL) arrow(Offset(x,64f+y),Offset(x+side*7f,19f+y))
            }
            if(diagram==TechniqueDiagram.LATERAL) arrow(Offset(127f,47f),Offset(171f,47f))
        }
        TechniqueDiagram.NECK -> {
            val head=Offset(130f,34f)
            drawCircle(ink.copy(alpha=.22f),19f,head)
            drawCircle(ink,19f,head,style=androidx.compose.ui.graphics.drawscope.Stroke(2f))
            line(Offset(130f,51f),Offset(130f,65f),w=8f)
            line(Offset(87f,70f),Offset(173f,70f))
            arrow(Offset(106f,36f),Offset(74f,36f))
            arrow(Offset(154f,36f),Offset(186f,36f))
        }
        TechniqueDiagram.ANKLE, TechniqueDiagram.WRIST -> {
            val joint=Offset(119f,60f)
            line(Offset(112f,18f),joint,w=10f)
            line(joint,Offset(169f,60f),w=9f)
            line(joint,Offset(153f,35f),ink.copy(alpha=.36f),8f)
            drawCircle(accent,5f,joint)
            arrow(Offset(179f,58f),Offset(167f,31f))
        }
        TechniqueDiagram.OPPOSITE, TechniqueDiagram.SCAPULA -> {
            val upper=Offset(130f,31f)
            val hip=Offset(130f,60f)
            drawCircle(ink,7f,Offset(130f,18f))
            line(upper,hip,w=8f)
            val w=diagram==TechniqueDiagram.SCAPULA
            val leftHand=Offset(if(w) 86f else 106f,if(w) 19f else 7f)
            val rightHand=Offset(if(w) 174f else 154f,if(w) 19f else 7f)
            chain(listOf(leftHand,Offset(105f,if(w) 44f else 21f),upper,
                Offset(155f,if(w) 44f else 21f),rightHand))
            chain(listOf(Offset(116f,87f),hip,Offset(144f,87f)))
            val second=if(w) rightHand else Offset(144f,83f)
            for(joint in listOf(leftHand,second)) {
                drawCircle(accent,6f,joint)
                drawCircle(ink,2f,joint)
            }
        }
        TechniqueDiagram.REFERENCE -> {
            val pose=motion?.frames?.firstOrNull() ?: return
            val left=pose.minOf { it.x }
            val top=pose.minOf { it.y }
            val fit=min(180f/(pose.maxOf { it.x }-left).coerceAtLeast(1f),
                68f/(pose.maxOf { it.y }-top).coerceAtLeast(1f))
            val sketch=pose.map { Offset(130f+(it.x-(left+pose.maxOf { p -> p.x })/2f)*fit,13f+(it.y-top)*fit) }
            for(indices in listOf(listOf(0,1,2),listOf(1,3,4),listOf(1,5,6),listOf(2,7,8),listOf(2,9,10))) {
                chain(indices.map(sketch::get))
            }
            drawCircle(ink,6f,sketch[0])
            for(joint in listOf(4,6,8,10))drawCircle(accent,3f,sketch[joint])
        }
    }
}
