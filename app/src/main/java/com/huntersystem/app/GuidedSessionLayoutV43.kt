package com.huntersystem.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
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
    val safeTotal=total.coerceAtLeast(1)
    val current=index.coerceIn(1,safeTotal)
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
                Eyebrow(if(preview)"VISTA PREVIA" else "ENTRENAMIENTO EN CURSO")
                Heading(title,size=24)
            }
            val shape=RoundedCornerShape(14.dp)
            Column(Modifier.clip(shape).background(c.accent.copy(alpha=.10f))
                .monarchOutline(shape)
                .padding(horizontal=12.dp,vertical=8.dp),
                horizontalAlignment=Alignment.End){
                HText("${current} / ${total}",size=17,bold=true,color=c.accent)
                HText("PASO ACTUAL",size=10,color=c.muted)
            }
        }
        HProgress(index.toFloat()/safeTotal)
    }
}

/** La demostración sigue siendo el foco y se adapta al ancho disponible. */
@Composable
internal fun GuidedSessionDemo(
    name:String,demo:String,running:Boolean,speed:Float,mirror:Boolean
){
    BoxWithConstraints(Modifier.fillMaxWidth()){
        val demoHeight=when {
            maxWidth<350.dp -> 225
            maxWidth<440.dp -> 265
            else -> 310
        }
        MonarchExerciseStage(name,demo,running,speed,mirror,
            height=demoHeight,showProgress=false)
    }
}

/** Temporizador, objetivo y progreso del intervalo en una sola tarjeta compacta. */
@Composable
internal fun GuidedSessionTimer(remaining:Int,totalSeconds:Int,summary:String,running:Boolean){
    val c=LocalHunter.current
    val progress=guidedSessionProgress(totalSeconds,remaining)
    val shape=RoundedCornerShape(22.dp)
    Row(Modifier.fillMaxWidth().clip(shape)
        .background(Brush.horizontalGradient(listOf(c.surface,c.panel)))
        .monarchOutline(shape)
        .padding(horizontal=13.dp,vertical=12.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(16.dp)){
        Box(Modifier.size(110.dp),contentAlignment=Alignment.Center){
            Canvas(Modifier.fillMaxSize().padding(4.dp)){
                val stroke=Stroke(width=7.dp.toPx(),cap=StrokeCap.Round)
                drawArc(c.line,-90f,360f,false,style=stroke)
                drawArc(c.accent,-90f,360f*progress,false,style=stroke)
            }
            Column(horizontalAlignment=Alignment.CenterHorizontally){
                Heading(guidedSessionClock(remaining),size=29)
                HText("RESTANTE",size=10,color=c.muted,bold=true)
            }
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)){
            Eyebrow("TIEMPO DE LA SERIE")
            HText(summary,size=15,bold=true,maxLines=3)
            HText(if(running)"● EN CURSO" else "Ⅱ EN PAUSA",size=12,
                color=if(running)c.accent else c.muted)
        }
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
                Modifier.weight(1.7f),primary=true){onToggle()}
            HButton("↺ REINICIAR",Modifier.weight(1f)){onRestart()}
        }
        HText("OPCIONES DE VISUALIZACIÓN",size=12,color=c.muted,bold=true)
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

/** Botón de seguridad con tratamiento visual separado de la navegación. */
@Composable
internal fun GuidedSessionStopButton(onStop:()->Unit){
    val warning=Color(0xFFFF8A9C)
    val shape=RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxWidth().defaultMinSize(minHeight=54.dp).clip(shape)
        .background(warning.copy(alpha=.07f))
        .border(1.dp,warning.copy(alpha=.7f),shape)
        .clickable(role=Role.Button,onClick=onStop)
        .padding(14.dp),contentAlignment=Alignment.Center){
        HText("Detener por molestias",size=15,bold=true,color=warning)
    }
}
