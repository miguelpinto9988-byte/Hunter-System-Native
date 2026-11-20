package com.huntersystem.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Formateo independiente del texto, seguro para valores de temporizador restaurados. */
internal fun guidedSessionClock(seconds:Int):String {
    val safe = seconds.coerceAtLeast(0)
    return "${safe/60}:${(safe%60).toString().padStart(2,'0')}"
}

internal fun guidedSessionProgress(total:Int,remaining:Int):Float =
    if(total<=0)0f else (1f-remaining.coerceIn(0,total).toFloat()/total).coerceIn(0f,1f)

@Composable
internal fun ColumnScope.GuidedSessionContent(content:@Composable ColumnScope.()->Unit){
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
        .padding(top=12.dp,bottom=8.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
}

@Composable
internal fun GuidedSessionFooter(content:@Composable ColumnScope.()->Unit){
    val c=LocalHunter.current
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
    Column(Modifier.fillMaxWidth().padding(top=8.dp),
        verticalArrangement=Arrangement.spacedBy(7.dp),content=content)
}

@Composable
internal fun GuidedSessionHeader(preview:Boolean,title:String,index:Int,total:Int){
    val c=LocalHunter.current
    val safeTotal=total.coerceAtLeast(1)
    val current=index.coerceIn(1,safeTotal)
    val shape=RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().padding(top=10.dp).clip(shape)
        .background(c.surface).monarchOutline(shape)
        .padding(horizontal=13.dp,vertical=10.dp),
        verticalArrangement=Arrangement.spacedBy(6.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)){
                if(preview)Eyebrow("VISTA PREVIA")
                HText(title,size=16,bold=true,maxLines=2)
            }
            HText("Paso $current de $safeTotal",size=14,bold=true,color=c.accent)
        }
        HProgress(index.toFloat()/safeTotal)
    }
}

@Composable
internal fun GuidedSessionStep(title:String,summary:String,kind:String){
    val c=LocalHunter.current
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
        Eyebrow(when(kind){
            "preparation"->"CALENTAMIENTO"
            "cooldown"->"VUELTA A LA CALMA"
            "rest"->"RECUPERACIÓN"
            else->"EJERCICIO"
        })
        Heading(title,size=28)
        HText(summary,size=15,bold=true,color=c.muted)
    }
}

@Composable
internal fun GuidedSessionDemo(
    exercise:Exercise,mirror:Boolean,running:Boolean,reset:Int
){
    val screenHeight=LocalConfiguration.current.screenHeightDp
    BoxWithConstraints(Modifier.fillMaxWidth()){
        val demoHeight=when {
            screenHeight<700 -> 136
            maxWidth<350.dp -> 164
            maxWidth<440.dp -> 192
            else -> 235
        }
        MonarchExerciseStage(exercise,mirror,height=demoHeight,play=running,reset=reset,
            showExerciseName=false)
    }
}

@Composable
internal fun GuidedSessionTimer(
    remaining:Int,totalSeconds:Int,running:Boolean,emphasized:Boolean,
    onToggle:()->Unit,onRestart:()->Unit
){
    val c=LocalHunter.current
    val progress=guidedSessionProgress(totalSeconds,remaining)
    val shape=RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().clip(shape)
        .background(Brush.horizontalGradient(listOf(c.surface,c.panel)))
        .monarchOutline(shape).padding(if(emphasized)16.dp else 10.dp),
        verticalArrangement=Arrangement.spacedBy(if(emphasized)12.dp else 6.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Column(Modifier.weight(1f)){
                Eyebrow("TIEMPO RESTANTE")
                Heading(guidedSessionClock(remaining),size=if(emphasized)46 else 30)
            }
            HButton(if(running)"Pausar" else if(remaining==0)"Reiniciar" else "Iniciar reloj",
                primary=true,onClick=if(remaining==0)onRestart else onToggle)
        }
        HProgress(progress)
        if(remaining in 1 until totalSeconds)
            HButton("Reiniciar reloj",Modifier.fillMaxWidth(),onClick=onRestart)
    }
}

@Composable
internal fun GuidedSessionStopButton(modifier:Modifier=Modifier,onStop:()->Unit){
    val warning=if(LocalHunter.current.isLight)Color(0xFF9A2543) else Color(0xFFFF8A9C)
    val shape=RoundedCornerShape(16.dp)
    Box(modifier.defaultMinSize(minHeight=48.dp).clip(shape)
        .background(warning.copy(alpha=.07f))
        .border(1.dp,warning.copy(alpha=.7f),shape)
        .clickable(role=Role.Button,onClick=onStop)
        .padding(horizontal=8.dp,vertical=10.dp),contentAlignment=Alignment.Center){
        HText("Detener por molestias",size=14,bold=true,color=warning)
    }
}
