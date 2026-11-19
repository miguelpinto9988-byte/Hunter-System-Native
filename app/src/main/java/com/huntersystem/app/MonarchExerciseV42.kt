package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
    val motion=remember(context,exercise.id){ExerciseMotions.load(context,exercise.id)}
    val c=LocalHunter.current
    val frame=RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().clip(frame)
        .background(Brush.verticalGradient(listOf(c.surface,c.panel)))
        .monarchOutline(frame)
        .padding(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Eyebrow("DEMOSTRACIÓN")
        if(motion==null&&exercise.demo.isBlank()){
            HText("Este ejercicio aún no tiene demostración.",size=15,color=c.muted)
        }else{
            Box(Modifier.fillMaxWidth().height(height.dp)
                .clip(RoundedCornerShape(17.dp)).background(c.input),
                contentAlignment=Alignment.Center){
                if(motion!=null) ExerciseActor(motion,Modifier.fillMaxSize(),play,mirror,reset)
                else OriginalSvg(exercise.demo,Modifier.fillMaxSize(),mirror=mirror)
            }
            if(motion?.source in listOf("historical","sport"))
                HText("Animación ilustrativa. Consulta la guía del ejercicio.",size=12,color=c.muted)
        }
        HText(exercise.name,size=14,color=c.text,maxLines=1)
    }
}
