package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/** Formateo independiente del texto, seguro para valores de temporizador restaurados. */
internal fun guidedSessionClock(seconds:Int):String {
    val safe = seconds.coerceAtLeast(0)
    return "${safe/60}:${(safe%60).toString().padStart(2,'0')}"
}

internal fun guidedSessionProgress(total:Int,remaining:Int):Float =
    if(total<=0)0f else (1f-remaining.coerceIn(0,total).toFloat()/total).coerceIn(0f,1f)

/** Encabezado legible sin desperdiciar altura en pantallas pequeñas. */
@Composable
internal fun GuidedSessionHeader(preview:Boolean,title:String,index:Int,total:Int){
    val c=LocalHunter.current
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
                Eyebrow(if(preview)"VISTA PREVIA" else "SESIÓN GUIADA")
                Heading(title,size=26)
            }
            val chip=RoundedCornerShape(14.dp)
            Box(Modifier.clip(chip).background(c.input)
                .border(1.dp,c.accent.copy(alpha=.55f),chip)
                .padding(horizontal=12.dp,vertical=10.dp)){
                HText("${index.coerceAtMost(total)}/$total",size=15,bold=true,color=c.accent)
            }
        }
        HProgress(index.toFloat()/total.coerceAtLeast(1))
        HText("PASO ${index.coerceAtMost(total)} DE $total",size=12,color=c.muted,bold=true)
    }
}

/** La demostración sigue siendo el foco y se adapta al ancho disponible. */
@Composable
internal fun GuidedSessionDemo(
    name:String,demo:String,running:Boolean,speed:Float,mirror:Boolean
){
    BoxWithConstraints(Modifier.fillMaxWidth()){
        val demoHeight=when {
            maxWidth<350.dp -> 198
            maxWidth<440.dp -> 230
            else -> 295
        }
        MonarchExerciseStage(name,demo,running,speed,mirror,
            height=demoHeight,showProgress=false)
    }
}

/** Temporizador, objetivo y progreso del intervalo en una sola tarjeta compacta. */
@Composable
internal fun GuidedSessionTimer(
    remaining:Int,totalSeconds:Int,summary:String,running:Boolean
){
    val c=LocalHunter.current
    val shape=RoundedCornerShape(19.dp)
    Column(Modifier.fillMaxWidth().clip(shape)
        .background(Brush.horizontalGradient(listOf(c.surface,c.panel)))
        .border(1.dp,c.line,shape)
        .padding(horizontal=16.dp,vertical=12.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)){
                Eyebrow("TIEMPO RESTANTE")
                Heading(guidedSessionClock(remaining),size=46)
            }
            Column(Modifier.weight(1f),horizontalAlignment=Alignment.End,
                verticalArrangement=Arrangement.spacedBy(6.dp)){
                HText(summary,size=14,bold=true,maxLines=3)
                HText(if(running)"EN CURSO" else "EN PAUSA",size=12,
                    color=if(running)c.accent else c.muted,bold=true)
            }
        }
        HProgress(guidedSessionProgress(totalSeconds,remaining))
    }
}

/** Las acciones del tiempo prevalecen; espejo y velocidad no compiten con ellas. */
@Composable
internal fun GuidedSessionPlaybackControls(
    running:Boolean,speed:Float,mirror:Boolean,
    onToggle:()->Unit,onRestart:()->Unit,onMirror:()->Unit,onSpeed:(Float)->Unit
){
    val c=LocalHunter.current
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            HButton(if(running)"Ⅱ PAUSAR" else "▶ REPRODUCIR",
                Modifier.weight(1.5f),primary=true){onToggle()}
            HButton("↺ REINICIAR",Modifier.weight(1f)){onRestart()}
        }
        HText("AJUSTES DE LA DEMOSTRACIÓN",size=12,color=c.muted,bold=true)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),
            verticalAlignment=Alignment.Bottom){
            HSelect("Velocidad",speed.toString(),
                listOf("0.5" to "0,5×","1.0" to "1×","1.5" to "1,5×","2.0" to "2×"),
                Modifier.weight(1f)){onSpeed(it.toFloat())}
            HButton(if(mirror)"⇄ ESPEJO ✓" else "⇄ ESPEJO",
                Modifier.weight(1f)){onMirror()}
        }
    }
}
