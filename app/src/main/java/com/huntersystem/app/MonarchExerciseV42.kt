package com.huntersystem.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

/**
 * Dos capas separadas: rig vectorial animado desde JSON y estética SVG fija.
 * Los SVG animados completos no forman parte de la aplicación.
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
    val context=LocalContext.current
    val rig=remember(context,demo){LegacyRigRepository.load(context,demo)}
    val hasArtwork=demo.isNotBlank()
    val hasRig=rig!=null
    var layer by remember(demo){mutableStateOf("motion")}
    val frame=RoundedCornerShape(24.dp)

    Column(Modifier.fillMaxWidth().clip(frame)
        .background(Brush.verticalGradient(listOf(c.surface,c.panel)))
        .monarchOutline(frame)
        .padding(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically){
            Eyebrow("DEMOSTRACIÓN · DOS CAPAS")
            HText(
                if(!hasRig)"SIN RIG DISPONIBLE"
                else if(isRunning && layer!="appearance")"● MOVIMIENTO VECTORIAL"
                else "CAPAS INDEPENDIENTES",
                size=11,bold=true,color=if(hasRig&&isRunning)c.accent else c.muted
            )
        }

        if(hasRig&&hasArtwork){
            HSelect("CAPA VISIBLE",layer,listOf(
                "motion" to "Esqueleto en movimiento",
                "appearance" to "Apariencia 2D estática",
                "both" to "Movimiento y apariencia separados"
            ),Modifier.fillMaxWidth()){layer=it}
        }

        Box(Modifier.fillMaxWidth().height(height.dp)
            .clip(RoundedCornerShape(17.dp)).background(c.input),
            contentAlignment=Alignment.Center){
            when {
                hasRig&&layer=="both"&&hasArtwork->{
                    Row(Modifier.fillMaxSize()){
                        Box(Modifier.weight(1f).fillMaxHeight()){
                            LegacyExerciseRig(rig!!,Modifier.fillMaxSize(),
                                isRunning,speed,mirror,progress,restartToken)
                        }
                        Box(Modifier.weight(1f).fillMaxHeight()){
                            OriginalSvg(demo,Modifier.fillMaxSize(),play=false,mirror=mirror)
                        }
                    }
                }
                hasRig&&layer!="appearance"->{
                    LegacyExerciseRig(rig!!,Modifier.fillMaxSize(),
                        isRunning,speed,mirror,progress,restartToken)
                }
                hasArtwork->{
                    OriginalSvg(demo,Modifier.fillMaxSize(),play=false,mirror=mirror)
                }
                else->{
                    HText("Este ejercicio no tiene todavía una capa de movimiento ni una ilustración 2D.",
                        size=15,color=c.muted)
                }
            }
        }
        if(showProgress&&hasRig){
            LinearProgressIndicator(progress={progress.coerceIn(0f,1f)},
                modifier=Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(6.dp)),
                color=c.accent,trackColor=c.line)
        }
        HText(name,size=14,color=c.text,maxLines=1)
    }
}

/** Controles sólo del rig: el temporizador real de la sesión es independiente. */
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
            HButton(if(running)"Ⅱ PAUSAR" else "▶ REPRODUCIR",
                Modifier.weight(1f),primary=true){onToggle()}
            HButton("↺ REINICIAR",Modifier.weight(1f)){onRestart()}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp),
            verticalAlignment=Alignment.CenterVertically){
            HSelect("VELOCIDAD DEL ESQUELETO",speed.toString(),
                listOf("0.5" to "0,5×","1.0" to "1×","1.5" to "1,5×","2.0" to "2×"),
                Modifier.weight(1f)){onSpeed(it.toFloat())}
            HButton(if(mirror)"ESPEJO ✓" else "⇄ ESPEJO",Modifier.weight(1f)){onMirror()}
        }
    }
}
