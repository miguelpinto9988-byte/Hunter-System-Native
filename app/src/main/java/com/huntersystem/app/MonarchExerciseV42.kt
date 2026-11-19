package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun MonarchExerciseStage(exercise:Exercise,mirror:Boolean,height:Int=340,
                         play:Boolean=true, reset:Int=0){
    val context=LocalContext.current
    val parts = remember(context, exercise.id) {
        ExerciseMotions.loadSequence(context, exercise)
    }
    val motion = remember(context, exercise.id, parts) {
        if (parts == null) ExerciseMotions.load(context, exercise.id) else null
    }
    val playlist = parts?.let {
        rememberExercisePhase(it, it.sumOf { part -> part.duration.toDouble() }.toFloat(), play, reset)
    }
    val boundary = parts?.let { it[0].duration / (it[0].duration + it[1].duration) }
    val current = remember(playlist, boundary) {
        derivedStateOf { if (playlist != null && boundary != null && playlist.floatValue >= boundary) 1 else 0 }
    }.value
    val sequenceFraming = remember(parts) { parts?.map(ExerciseFraming::from).orEmpty() }
    val c=LocalHunter.current
    val frame=RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().clip(frame)
        .background(Brush.verticalGradient(listOf(c.surface,c.panel)))
        .monarchOutline(frame)
        .padding(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Eyebrow("DEMOSTRACIÓN")
        if(motion==null && parts==null && exercise.demo.isBlank()){
            HText("Este ejercicio aún no tiene demostración.",size=15,color=c.muted)
        }else{
            Box(Modifier.fillMaxWidth().height(height.dp)
                .clip(RoundedCornerShape(17.dp)).background(c.input),
                contentAlignment=Alignment.Center){
                if (parts != null && playlist != null && boundary != null) {
                    ExerciseActor(parts[current], Modifier.fillMaxSize(), false, mirror, reset, sequenceFraming) {
                        val phase = playlist.floatValue
                        if (current == 0) (phase / boundary).coerceIn(0f, .9999f)
                        else ((phase - boundary) / (1f - boundary)).coerceIn(0f, .9999f)
                    }
                } else if(motion!=null) ExerciseActor(motion,Modifier.fillMaxSize(),play,mirror,reset)
                else OriginalSvg(exercise.demo,Modifier.fillMaxSize(),mirror=mirror)
            }
            if (parts != null && playlist != null && boundary != null) {
                val labels = exercise.raw.a("sequenceLabels").strings()
                HText("${current + 1} de ${parts.size} · ${labels.getOrNull(current).orEmpty()}",
                    size=12,color=c.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HButton("Gesto 1", Modifier.weight(1f)) { playlist.floatValue = 0f }
                    HButton("Gesto 2", Modifier.weight(1f)) { playlist.floatValue = boundary }
                }
            }
            if(parts != null || motion?.source in listOf("historical","sport"))
                HText("Animación ilustrativa. Consulta la guía del ejercicio.",size=12,color=c.muted)
        }
        HText(exercise.name,size=14,color=c.text,maxLines=1)
    }
}
