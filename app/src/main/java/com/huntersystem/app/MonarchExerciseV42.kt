package com.huntersystem.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/** Ilustraciones 2D estáticas; se han retirado las demostraciones SVG animadas. */
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
    val hasArtwork=demo.isNotBlank()
    Column(Modifier.fillMaxWidth().clip(frame)
        .background(Brush.verticalGradient(listOf(c.surface,c.panel)))
        .monarchOutline(frame)
        .padding(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically){
            Eyebrow("DEMOSTRACIÓN · SVG 2D ESTÁTICO")
            HText(if(hasArtwork)"IMAGEN FIJA" else "SIN ILUSTRACIÓN",
                size=12,bold=true,color=c.muted)
        }
        Box(Modifier.fillMaxWidth().height(height.dp)
            .clip(RoundedCornerShape(17.dp)).background(c.input),
            contentAlignment=Alignment.Center){
            if(hasArtwork){
                OriginalSvg(demo,Modifier.fillMaxWidth().fillMaxHeight(),
                    play=false,mirror=mirror)
            }else{
                HText("Este ejercicio no tiene ilustración 2D disponible.",
                    size=15,color=c.muted)
            }
        }
        HText(name,size=14,color=c.text,maxLines=1)
    }
}
