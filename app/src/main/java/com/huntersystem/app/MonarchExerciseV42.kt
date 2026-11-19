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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Preserves the original media2d SVG exactly and exposes its separate vector rig.
 * The JSON-driven skeleton is technical; it does not deform the static SVG artwork.
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
    val reduceMotion=LocalMonarchMotion.current.reduced
    val rig=remember(context,demo){OriginalVectorRigAssets.load(context,demo)}
    val hasArtwork=demo.isNotBlank()
    var layer by remember(demo){mutableStateOf("appearance")}
    var rigPlaying by remember(demo){mutableStateOf(isRunning)}
    var rigSpeed by remember(demo){mutableFloatStateOf(speed.coerceIn(.5f,2f))}
    var rigScrub by remember(demo){mutableFloatStateOf(progress.coerceIn(0f,1f))}
    var rigRestart by remember(demo){mutableIntStateOf(0)}
    LaunchedEffect(isRunning){rigPlaying=isRunning}

    val frame=RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().clip(frame)
        .background(Brush.verticalGradient(listOf(c.surface,c.panel)))
        .monarchOutline(frame)
        .padding(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically){
            Eyebrow("DEMOSTRACIÓN · DOS CAPAS")
            HText(if(!hasArtwork)"SIN ILUSTRACIÓN"
                else if(layer=="appearance")"IMAGEN FIJA"
                else if(rig==null)"RIG NO DISPONIBLE"
                else "ARTICULACIONES VECTORIALES",
                size=11,bold=true,color=c.muted)
        }
        if(rig!=null){
            HSelect("CAPA VECTORIAL",layer,listOf(
                "appearance" to "Estética original",
                "structure" to "Estructura articulada",
                "both" to "Ambas superpuestas"
            ),Modifier.fillMaxWidth()){layer=it}
        }
        Box(Modifier.fillMaxWidth().height(height.dp)
            .clip(RoundedCornerShape(17.dp)).background(c.input),
            contentAlignment=Alignment.Center){
            when {
                !hasArtwork->HText(
                    "Este ejercicio aún no tiene ilustración original.",
                    size=15,color=c.muted
                )
                layer=="appearance"||rig==null->
                    OriginalSvg(demo,Modifier.fillMaxSize(),play=false,mirror=mirror)
                else->{
                    if(layer=="both")
                        OriginalSvg(demo,Modifier.fillMaxSize(),play=false,mirror=mirror)
                    OriginalVectorRigCanvas(
                        rig=rig,
                        modifier=Modifier.fillMaxSize(),
                        play=rigPlaying&&!reduceMotion,
                        speed=rigSpeed,
                        mirror=mirror,
                        scrub=rigScrub,
                        restartToken=restartToken+rigRestart
                    )
                }
            }
        }
        if(rig!=null&&layer!="appearance"){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),
                verticalAlignment=Alignment.CenterVertically){
                HButton(if(rigPlaying)"Ⅱ PAUSAR RIG" else "▶ MOVER RIG",
                    Modifier.weight(1f)){rigPlaying=!rigPlaying}
                HButton("↺",Modifier.width(58.dp)){
                    rigRestart++;rigScrub=0f
                }
            }
            HSelect("VELOCIDAD DEL RIG",rigSpeed.toString(),listOf(
                "0.5" to "0,5×","1.0" to "1×","1.5" to "1,5×","2.0" to "2×"
            ),Modifier.fillMaxWidth()){rigSpeed=it.toFloat()}
            HText("Recorrer poses",size=12,color=c.muted)
            Slider(rigScrub,{rigScrub=it;rigPlaying=false})
            if(reduceMotion)HText(
                "Movimiento reducido: usa el deslizador para recorrer las poses.",
                size=12,color=c.muted
            )
            HText("La ilustración original sigue intacta: el rig es una capa aparte.",
                size=12,color=c.muted)
        }
        HText(name,size=14,color=c.text,maxLines=1)
    }
}
