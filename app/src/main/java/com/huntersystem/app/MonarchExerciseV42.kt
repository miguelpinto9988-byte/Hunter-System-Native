package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    val custom3dClip = if (!thumbnail && parts == null) exercise3dClip(exercise.id) else null
    var showProjection by remember(exercise.id) { mutableStateOf(false) }
    val projectionAvailable = !thumbnail && onTogglePlayback == null && custom3dClip == null &&
        (motion != null || !parts.isNullOrEmpty())
    val projection = remember(exercise.id, showProjection, parts, motion) {
        if (showProjection && projectionAvailable) projectExercise3d(parts ?: listOfNotNull(motion), exercise.id) else null
    }
    val clip3d = custom3dClip ?: if (projection != null) "projected" else null
    var ready3d by remember(exercise.id, clip3d) { mutableStateOf(false) }
    var failed3d by remember(exercise.id, clip3d) { mutableStateOf(false) }
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
        if(!guided) {
            Eyebrow(if (ready3d) "DEMOSTRACIÓN 3D" else "DEMOSTRACIÓN")
            if (catalog.isExercise3dReviewed(exercise.id)) {
                HText("✓ Animación 3D revisada", size = 13, color = c.accent)
            }
            if (projectionAvailable) {
                HButton(if (showProjection) "Volver a la demostración original" else "Ver movimiento en 3D") {
                    showProjection = !showProjection
                }
                if (showProjection) HText(
                    if (failed3d) "El visor 3D no está disponible en este dispositivo."
                    else "Vista 3D proyectada desde la demostración original. Usa la guía para revisar la técnica.",
                    size = 12, color = c.muted
                )
            }
        }
        if(motion==null && parts==null && custom3dClip==null){
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
                if (parts != null && playlist != null && boundary != null && !ready3d) {
                    ExerciseActor(parts[current], Modifier.fillMaxSize(), false, mirror, reset, sequenceFraming,
                        themedStage=guided,controlsWidth=controlsWidth) {
                        val phase = playlist.floatValue
                        if (current == 0) (phase / boundary).coerceIn(0f, .9999f)
                        else ((phase - boundary) / (1f - boundary)).coerceIn(0f, .9999f)
                    }
                } else if(motion!=null && !ready3d) ExerciseActor(motion,Modifier.fillMaxSize(),play,mirror,reset,
                    themedStage=guided,controlsWidth=controlsWidth)
                if (clip3d != null && !failed3d) {
                    key(exercise.id, clip3d) {
                        InteractiveExercise3d(
                            clip3d, projection, play && !LocalMonarchMotion.current.reduced, mirror, reset,
                            onReady = { available ->
                                ready3d = available
                                failed3d = !available
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                if(hasPlaybackControl)
                    GuidedSessionButton(if(play)"Pausar demostración" else "Reanudar demostración",
                        if(play)GuidedSessionGlyph.PAUSE else GuidedSessionGlyph.PLAY,
                        Modifier.align(Alignment.TopEnd).padding(5.dp),
                        iconOnly=true,onClick=onTogglePlayback)
            }
            if (!thumbnail && parts != null && playlist != null && boundary != null && !ready3d) {
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
