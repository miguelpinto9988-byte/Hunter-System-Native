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
 * Vistas vectoriales nuevas basadas en la figura delgada histórica de media2d.
 * No cambia los IDs del catálogo ni activa ejercicios pendientes de revisión.
 */
internal fun thinRigViews(demo:String):List<Pair<String,String>> = when(demo.substringAfterLast('/')) {
    "squat.svg" -> listOf(
        "FRONTAL" to "media2d/rig-v2/squat_front.svg",
        "LATERAL" to "media2d/rig-v2/squat_side.svg"
    )
    "pushup.svg" -> listOf("CENITAL" to "media2d/rig-v2/pushup_top.svg")
    else -> emptyList()
}

/**
 * Hunter System V42.6 Monarca fase 07: escenario reutilizable de ejercicios.
 * Reproduce los SVG animados originales en Canvas Android, sin motor Rive ni WebView.
 * Estas demostraciones no sustituyen la revisión biomecánica.
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
    val views=remember(demo){thinRigViews(demo)}
    var viewIndex by remember(demo){mutableIntStateOf(0)}
    val activeDemo=views.getOrNull(viewIndex)?.second?:demo
    val frame=RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().clip(frame)
        .background(Brush.verticalGradient(listOf(c.surface,c.panel)))
        .monarchOutline(frame)
        .padding(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically){
            Eyebrow(if(views.isNotEmpty())"DEMOSTRACIÓN NATIVA · RIG VECTORIAL 3D" else "DEMOSTRACIÓN NATIVA · SVG 2D")
            HText(if(isRunning)"● EN MOVIMIENTO" else "Ⅱ PAUSADA",
                size=12,bold=true,color=if(isRunning)c.accent else c.muted)
        }
        if(views.size>1){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                views.forEachIndexed{index,view->
                    HButton(view.first,Modifier.weight(1f),primary=viewIndex==index){
                        viewIndex=index
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(height.dp)
            .clip(RoundedCornerShape(17.dp)).background(c.input),
            contentAlignment=Alignment.Center){
            key(activeDemo,restartToken){
                OriginalSvg(activeDemo,Modifier.fillMaxWidth().fillMaxHeight(),
                    play=isRunning,speed=speed,mirror=mirror,progress=progress.coerceIn(0f,1f))
            }
        }
        if(showProgress){
            LinearProgressIndicator(progress={progress.coerceIn(0f,1f)},
                modifier=Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(6.dp)),
                color=c.accent,trackColor=c.line)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            HText(name,size=14,color=c.text,maxLines=1,modifier=Modifier.weight(1f))
            HText("${speed}×  ${if(mirror)"ESPEJO" else "NORMAL"}",
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
            HSelect("VELOCIDAD",speed.toString(),
                listOf("0.5" to "0,5×","1.0" to "1×","1.5" to "1,5×","2.0" to "2×"),
                Modifier.weight(1f)){onSpeed(it.toFloat())}
            HButton(if(mirror)"ESPEJO ✓" else "⇄ ESPEJO",Modifier.weight(1f)){onMirror()}
        }
    }
}
