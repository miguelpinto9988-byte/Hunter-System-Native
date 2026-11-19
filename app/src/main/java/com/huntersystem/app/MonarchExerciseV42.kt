package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun MonarchExerciseStage(name:String,demo:String,mirror:Boolean,height:Int=340){
    val c=LocalHunter.current
    val frame=RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().clip(frame)
        .background(Brush.verticalGradient(listOf(c.surface,c.panel)))
        .monarchOutline(frame)
        .padding(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Eyebrow("DEMOSTRACIÓN")
        if(demo.isBlank()){
            HText("Este ejercicio aún no tiene ilustración de este estilo.",size=15,color=c.muted)
        }else{
            Box(Modifier.fillMaxWidth().height(height.dp)
                .clip(RoundedCornerShape(17.dp)).background(c.input),
                contentAlignment=Alignment.Center){
                OriginalSvg(demo,Modifier.fillMaxSize(),mirror=mirror)
            }
        }
        HText(name,size=14,color=c.text,maxLines=1)
    }
}
