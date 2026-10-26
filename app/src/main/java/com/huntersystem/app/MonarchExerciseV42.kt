package com.huntersystem.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Hunter System V42.6 Monarca fase 07: escenario reutilizable de ejercicios.
 * Renderiza únicamente Rive (.riv) de manera nativa, sin alternativas SVG o MP4.
 * La conversión del SVG de origen no certifica anatomía ni biomecánica.
 */
@Composable
fun MonarchExerciseStage(
    name:String,
    demo:String,
    isRunning:Boolean,
    speed:Float,
    mirror:Boolean,
    progress:Float=0f,
    height:Int=340,
    showProgress:Boolean=true,
    restartToken:Int=0
){
    val c=LocalHunter.current
    val frame=RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().clip(frame)
        .background(Brush.verticalGradient(listOf(c.surface,c.panel)))
        .border(1.dp,c.accent.copy(alpha=.38f),frame)
        .padding(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically){
            Eyebrow("DEMOSTRACIÓN NATIVA · RIVE 2D")
            HText(if(isRunning)"● EN MOVIMIENTO" else "Ⅱ PAUSADA",
                size=12,bold=true,color=if(isRunning)c.accent else c.muted)
        }
        Box(Modifier.fillMaxWidth().height(height.dp)
            .clip(RoundedCornerShape(17.dp)).background(c.input),
            contentAlignment=Alignment.Center){
            HunterRiveExercise(demo,Modifier.fillMaxWidth().fillMaxHeight(),
                play=isRunning,mirror=mirror,restartToken=restartToken)
        }
        if(showProgress){
            LinearProgressIndicator(progress={progress.coerceIn(0f,1f)},
                modifier=Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(6.dp)),
                color=c.accent,trackColor=c.line)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            HText(name,size=14,color=c.text,maxLines=1,modifier=Modifier.weight(1f))
            HText("RIVE 1×  ${if(mirror)"ESPEJO" else "NORMAL"}",
                size=12,color=c.muted)
        }
    }
}

/** Controles táctiles con objetivos grandes; velocidad visual y temporizador son independientes. */
@Composable
fun MonarchPlaybackControls(
    running:Boolean,
    speed:Float,
    mirror:Boolean,
    onToggle:()->Unit,
    onRestart:()->Unit,
    onMirror:()->Unit,
    onSpeed:(Float)->Unit
){
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){
            HButton(if(running)"Ⅱ  PAUSAR" else "▶  REPRODUCIR",
                Modifier.weight(1f),primary=true){onToggle()}
            HButton("↺  REINICIAR",Modifier.weight(1f)){onRestart()}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp),
            verticalAlignment=Alignment.CenterVertically){
            HText("Rive: reproducción normal 1×",Modifier.weight(1f),size=13)
            HButton(if(mirror)"ESPEJO ✓" else "⇄ ESPEJO",Modifier.weight(1f)){onMirror()}
        }
    }
}
