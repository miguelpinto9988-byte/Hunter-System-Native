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
fun MonarchExerciseStage(exercise:Exercise,catalog:Catalog,mirror:Boolean,height:Int=340,
                         play:Boolean=true, reset:Int=0, showExerciseName:Boolean=true,
                         onTogglePlayback:(()->Unit)?=null,thumbnail:Boolean=false){
    val context=LocalContext.current
    val clips = remember(context, catalog, exercise.id) {
        ExerciseMotions.loadClips(context, exercise, catalog)
    }
    val parts = remember(clips) { clips?.map(ExerciseClip::motion) }
    val motion = remember(context, exercise.id, parts) {
        if (parts == null) ExerciseMotions.load(context, exercise) else null
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
    val guided=onTogglePlayback!=null||thumbnail
    val frame=if(guided)GuidedSessionFrame else RoundedCornerShape(24.dp)
    val canPlay=parts?.any { !it.staticHold } ?: (motion?.staticHold==false)
    val hasPlaybackControl=onTogglePlayback!=null&&canPlay&&!LocalMonarchMotion.current.reduced
    val controlsWidth=if(hasPlaybackControl)56.dp else 0.dp
    Column(Modifier.fillMaxWidth().clip(frame)
        .background(Brush.verticalGradient(listOf(c.surface,c.panel)))
        .monarchOutline(frame)
        .padding(if(thumbnail)0.dp else if(guided)8.dp else 12.dp),
        verticalArrangement=Arrangement.spacedBy(if(guided)4.dp else 9.dp)){
        if(!guided) Eyebrow("DEMOSTRACIÓN")
        if(motion==null && parts==null){
            if(!thumbnail) {
                val message = if (CoreExerciseClips.hasInconsistentInstructions(exercise))
                    "Demostración en revisión. La ficha combina indicaciones de movimientos distintos."
                else "Este ejercicio aún no tiene demostración."
                HText(message,size=15,color=c.muted)
            }
        }else{
            Box(Modifier.fillMaxWidth().height(height.dp)
                .clip(if(guided)GuidedSessionFrame else RoundedCornerShape(17.dp)).background(c.input),
                contentAlignment=Alignment.Center){
                if (parts != null && playlist != null && boundary != null) {
                    ExerciseActor(parts[current], Modifier.fillMaxSize(), false, mirror, reset, sequenceFraming,
                        themedStage=guided,controlsWidth=controlsWidth) {
                        val phase = playlist.floatValue
                        if (current == 0) (phase / boundary).coerceIn(0f, .9999f)
                        else ((phase - boundary) / (1f - boundary)).coerceIn(0f, .9999f)
                    }
                } else if(motion!=null) ExerciseActor(motion,Modifier.fillMaxSize(),play,mirror,reset,
                    themedStage=guided,controlsWidth=controlsWidth)
                if(hasPlaybackControl)
                    GuidedSessionButton(if(play)"Pausar demostración" else "Reanudar demostración",
                        if(play)GuidedSessionGlyph.PAUSE else GuidedSessionGlyph.PLAY,
                        Modifier.align(Alignment.TopEnd).padding(5.dp),
                        iconOnly=true,onClick=onTogglePlayback)
            }
            if (!thumbnail && parts != null && playlist != null && boundary != null) {
                val label = clips?.getOrNull(current)?.label.orEmpty()
                HText("${current + 1} de ${parts.size} · $label",
                    size=12,color=c.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HButton("Gesto 1", Modifier.weight(1f)) { playlist.floatValue = 0f }
                    HButton("Gesto 2", Modifier.weight(1f)) { playlist.floatValue = boundary }
                }
            }
            val reference = exercise.raw.s("motionReferenceLabel")
            if (!thumbnail && reference.isNotBlank()) HText(reference,size=12,color=c.muted)
            if(!thumbnail && (parts != null || motion?.source in listOf("historical","sport")))
                HText("Animación ilustrativa. Consulta la guía del ejercicio.",size=12,color=c.muted)
        }
        if(!guided)ExerciseTechniquePanel(exercise, parts?.getOrNull(current) ?: motion,
            clips?.getOrNull(current)?.sourceId ?: exercise.id)
        if(!guided) {
            val visible = parts?.getOrNull(current) ?: motion
            val labels = visible?.let(::equipmentLabels).orEmpty()
            if(labels.isNotEmpty()) HText("Equipo · ${labels.joinToString(" · ")}",size=13,color=c.muted)
        }
        if(showExerciseName)HText(exercise.name,size=14,color=c.text,maxLines=1)
    }
}
