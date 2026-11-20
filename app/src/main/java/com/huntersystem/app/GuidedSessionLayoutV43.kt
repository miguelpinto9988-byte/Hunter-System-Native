package com.huntersystem.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Formateo independiente del texto, seguro para valores de temporizador restaurados. */
internal fun guidedSessionClock(seconds:Int):String {
    val safe = seconds.coerceAtLeast(0)
    return "${safe/60}:${(safe%60).toString().padStart(2,'0')}"
}

internal fun guidedSessionProgress(total:Int,remaining:Int):Float =
    if(total<=0)0f else (1f-remaining.coerceIn(0,total).toFloat()/total).coerceIn(0f,1f)

internal val GuidedSessionFrame = GenericShape { size, _ ->
    val cut = size.minDimension * .16f
    val corner = cut.coerceAtMost(12f)
    moveTo(corner, 0f)
    lineTo(size.width - corner, 0f)
    lineTo(size.width, corner)
    lineTo(size.width, size.height - corner)
    lineTo(size.width - corner, size.height)
    lineTo(corner, size.height)
    lineTo(0f, size.height - corner)
    lineTo(0f, corner)
    close()
}

private val HunterColors.sessionGold:Color
    get()=if(isLight) Color(0xFF896011) else Color(0xFFFFCB72)
private val HunterColors.sessionBlue:Color
    get()=if(isLight) Color(0xFF1C55C1) else Color(0xFF66CBFF)

internal enum class GuidedSessionGlyph(val path:String) {
    BACK("M15 5l-7 7 7 7"), NEXT("M9 5l7 7-7 7"), CLOSE("M6 6l12 12M18 6L6 18"),
    PLAY("M8 4l12 8-12 8z"), PAUSE("M9 5v14m6-14v14"),
    RESET("M4 11a8 8 0 1 1 2.2 6M4 5v6h6"),
    MIRROR("M12 3v18M8 6l-5 6 5 6zM16 6l5 6-5 6z"),
    BOOK("M12 6C9 3 5 3 3 4v14c3-1 6-1 9 2 3-3 6-3 9-2V4c-2-1-6-1-9 2v14"),
    RECORD("M5 12l5 5L20 7"),
    STOP("M12 3l8 4v5c0 5-6 8-8 9-2-1-8-4-8-9V7zM12 8v7m-3-3.5h6"),
    CLOCK("M12 7v5l3 2m6-2a9 9 0 1 1-18 0 9 9 0 0 1 18 0z"),
    PLAN("M12 2l2.5 7.5L22 12l-7.5 2.5L12 22l-2.5-7.5L2 12l7.5-2.5z")
}

@Composable
private fun SessionGlyph(glyph:GuidedSessionGlyph,color:Color,size:Int=18){
    val path=remember(glyph){PathParser().parsePathString(glyph.path).toPath()}
    Canvas(Modifier.size(size.dp)){
        scale(this.size.width/24f,this.size.height/24f,pivot=Offset.Zero){
            drawPath(path,color,style=Stroke(1.65f,cap=StrokeCap.Round,join=StrokeJoin.Round))
        }
    }
}

@Composable
internal fun GuidedSessionButton(
    label:String,glyph:GuidedSessionGlyph,modifier:Modifier=Modifier,
    primary:Boolean=false,enabled:Boolean=true,ink:Color=LocalHunter.current.sessionBlue,
    iconOnly:Boolean=false,onClick:()->Unit
){
    val c=LocalHunter.current
    val textColor=when{
        !enabled->c.muted
        primary->Color.White
        else->c.text
    }
    val fill=if(primary)listOf(Color(0xFF2358A1),Color(0xFF143266))
        else listOf(c.surface,c.panel)
    val edge=if(primary)c.sessionGold else ink
    val resolvedModifier=if(iconOnly)modifier.size(48.dp) else modifier.heightIn(min=48.dp)
    Row(resolvedModifier.clip(GuidedSessionFrame)
        .background(Brush.verticalGradient(fill))
        .border(1.dp,edge.copy(alpha=if(!enabled).24f else if(primary).95f else .58f),GuidedSessionFrame)
        .clickable(enabled=enabled,role=Role.Button,onClick=onClick)
        .then(if(iconOnly)Modifier.semantics { contentDescription=label } else Modifier)
        .padding(horizontal=if(iconOnly)0.dp else 7.dp,vertical=if(iconOnly)0.dp else 5.dp),
        horizontalArrangement=Arrangement.spacedBy(6.dp,Alignment.CenterHorizontally),
        verticalAlignment=Alignment.CenterVertically){
        SessionGlyph(glyph,if(primary)Color(0xFFFFDF98) else ink.copy(alpha=if(enabled)1f else .6f))
        if(!iconOnly)Text(label,color=textColor,fontFamily=Barlow,fontWeight=FontWeight.SemiBold,
            fontSize=15.sp,lineHeight=17.sp,textAlign=TextAlign.Center)
    }
}

@Composable
internal fun GuidedSessionToolbar(onBack:()->Unit,onClose:()->Unit){
    val c=LocalHunter.current
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)){
            GuidedSessionButton("Volver",GuidedSessionGlyph.BACK,iconOnly=true,onClick=onBack)
            SessionGlyph(GuidedSessionGlyph.PLAN,c.sessionGold,size=20)
            GuidedSessionTitle("Sesión paso a paso",Modifier.weight(1f),size=19)
            GuidedSessionButton("Cerrar",GuidedSessionGlyph.CLOSE,iconOnly=true,onClick=onClose)
        }
        Box(Modifier.fillMaxWidth().height(1.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent,c.sessionGold.copy(alpha=.8f),Color.Transparent))))
    }
}

@Composable
private fun GuidedSessionTitle(label:String,modifier:Modifier=Modifier,size:Int=26){
    Text(label,modifier,color=LocalHunter.current.text,fontFamily=FontFamily.Serif,
        fontSize=size.sp,lineHeight=(size*1.12f).sp,fontWeight=FontWeight.Bold)
}

@Composable
internal fun ColumnScope.GuidedSessionContent(content:@Composable ColumnScope.()->Unit){
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
        .padding(top=10.dp,bottom=8.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
}

@Composable
internal fun GuidedSessionFooter(content:@Composable ColumnScope.()->Unit){
    val c=LocalHunter.current
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
    Column(Modifier.fillMaxWidth().padding(top=8.dp),
        verticalArrangement=Arrangement.spacedBy(6.dp),content=content)
}

@Composable
internal fun GuidedSessionHeader(preview:Boolean,title:String,index:Int,total:Int){
    val c=LocalHunter.current
    val safeTotal=total.coerceAtLeast(1)
    val current=index.coerceIn(1,safeTotal)
    Column(Modifier.fillMaxWidth().padding(top=8.dp).clip(GuidedSessionFrame)
        .background(Brush.horizontalGradient(listOf(c.surface,c.panel))).monarchOutline(GuidedSessionFrame,active=true)
        .padding(horizontal=12.dp,vertical=8.dp),
        verticalArrangement=Arrangement.spacedBy(6.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)){
            SessionGlyph(GuidedSessionGlyph.PLAN,c.sessionGold)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)){
                if(preview)Eyebrow("VISTA PREVIA")
                GuidedSessionTitle(title,size=16)
            }
            HText("Paso $current de $safeTotal",size=14,bold=true,color=c.sessionBlue)
        }
        HProgress(index.toFloat()/safeTotal)
    }
}

@Composable
internal fun GuidedSessionStep(title:String,summary:String,kind:String){
    val c=LocalHunter.current
    Column(verticalArrangement=Arrangement.spacedBy(3.dp)){
        Eyebrow(when(kind){
            "preparation"->"CALENTAMIENTO"
            "cooldown"->"VUELTA A LA CALMA"
            "rest"->"RECUPERACIÓN"
            else->"EJERCICIO"
        })
        GuidedSessionTitle(title,size=if(kind=="rest")30 else 26)
        HText(summary,size=15,bold=true,color=c.muted)
    }
}

@Composable
internal fun GuidedSessionDemo(
    exercise:Exercise,mirror:Boolean,playing:Boolean,onToggle:()->Unit
){
    val screenHeight=LocalConfiguration.current.screenHeightDp
    BoxWithConstraints(Modifier.fillMaxWidth()){
        val demoHeight=when {
            screenHeight<700 -> 156
            maxWidth<350.dp -> 210
            maxWidth<440.dp -> 232
            else -> 270
        }
        MonarchExerciseStage(exercise,mirror,height=demoHeight,play=playing,
            showExerciseName=false,onTogglePlayback=onToggle)
    }
}

@Composable
internal fun GuidedSessionNext(title:String,series:String,exercise:Exercise?,mirror:Boolean){
    val c=LocalHunter.current
    Row(Modifier.fillMaxWidth().clip(GuidedSessionFrame).background(c.surface)
        .monarchOutline(GuidedSessionFrame).padding(8.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
        if(exercise!=null)Box(Modifier.width(92.dp)){
            MonarchExerciseStage(exercise,mirror,height=78,play=false,showExerciseName=false,thumbnail=true)
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
            Eyebrow("A CONTINUACIÓN")
            GuidedSessionTitle(title,size=18)
            HText(series,size=14,bold=true,color=c.muted)
        }
    }
}

@Composable
internal fun GuidedSessionTimer(
    remaining:Int,totalSeconds:Int,running:Boolean,emphasized:Boolean,
    onToggle:()->Unit,onRestart:()->Unit
){
    val c=LocalHunter.current
    val progress=guidedSessionProgress(totalSeconds,remaining)
    val label=when{
        running->"Pausar reloj"
        remaining==0->"Reiniciar reloj"
        remaining<totalSeconds->"Reanudar reloj"
        else->"Iniciar reloj"
    }
    val glyph=when{
        running->GuidedSessionGlyph.PAUSE
        remaining==0->GuidedSessionGlyph.RESET
        else->GuidedSessionGlyph.PLAY
    }
    val onAction=if(remaining==0)onRestart else onToggle
    Column(Modifier.fillMaxWidth().clip(GuidedSessionFrame)
        .background(Brush.verticalGradient(listOf(c.surface,c.panel)))
        .monarchOutline(GuidedSessionFrame,active=true).padding(8.dp),
        verticalArrangement=Arrangement.spacedBy(6.dp),
        horizontalAlignment=Alignment.CenterHorizontally){
        if(emphasized){
            BoxWithConstraints(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){
                val compact=LocalConfiguration.current.screenHeightDp<720
                val diameter=minOf(maxWidth,if(compact)184.dp else 244.dp)
                Box(Modifier.size(diameter),contentAlignment=Alignment.Center){
                    GuidedClockDial(progress,Modifier.matchParentSize(),large=true)
                    Column(horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.spacedBy(3.dp)){
                        Eyebrow("TIEMPO RESTANTE")
                        GuidedSessionTitle(guidedSessionClock(remaining),size=if(compact)38 else 52)
                        GuidedSessionButton(label,glyph,iconOnly=true,primary=true,onClick=onAction)
                    }
                }
            }
        }else{
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(7.dp)){
                Box(Modifier.size(57.dp),contentAlignment=Alignment.Center){
                    GuidedClockDial(progress,Modifier.matchParentSize(),large=false)
                    GuidedSessionTitle(guidedSessionClock(remaining),size=15)
                }
                Eyebrow("TIEMPO RESTANTE",Modifier.weight(1f))
                GuidedSessionButton(label,glyph,primary=true,onClick=onAction)
            }
        }
        if(remaining in 1 until totalSeconds)
            GuidedSessionButton("Reiniciar reloj",GuidedSessionGlyph.RESET,onClick=onRestart)
    }
}

@Composable
private fun GuidedClockDial(progress:Float,modifier:Modifier,large:Boolean){
    val c=LocalHunter.current
    val visibleProgress by animateFloatAsState(progress,LocalMonarchMotion.current.spec(350),label="reloj-circular")
    val blue=c.sessionBlue
    val gold=c.sessionGold
    Canvas(modifier.clip(CircleShape).semantics { progressBarRangeInfo=ProgressBarRangeInfo(progress,0f..1f) }){
        val radius=size.minDimension/2f
        val ring=radius-if(large)11.dp.toPx() else 4.dp.toPx()
        val stroke=if(large)6.dp.toPx() else 3.dp.toPx()
        val corner=center-Offset(ring,ring)
        val span=Size(ring*2,ring*2)
        drawCircle(Brush.radialGradient(listOf(c.bg,c.panel,c.input),center,radius))
        drawCircle(blue.copy(alpha=.22f),ring,style=Stroke(stroke))
        if(large){
            drawCircle(gold.copy(alpha=.57f),ring+5.dp.toPx(),style=Stroke(1.dp.toPx()))
            repeat(48){ i->
                val angle=(i*2*PI/48-PI/2).toFloat()
                val unit=Offset(cos(angle),sin(angle))
                val outside=center+unit*(ring-8.dp.toPx())
                val inside=outside-unit*(if(i%4==0)8.dp.toPx() else 4.dp.toPx())
                drawLine(if(i%12==0)gold else blue.copy(alpha=.66f),inside,outside,1.5.dp.toPx(),StrokeCap.Round)
            }
        }
        val sweep=360f*visibleProgress
        if(sweep>0f)drawArc(Brush.sweepGradient(listOf(blue,gold,blue),center),
            -90f,sweep,false,topLeft=corner,size=span,style=Stroke(stroke,cap=StrokeCap.Round))
        val angle=((sweep-90f)*PI/180).toFloat()
        val cursor=center+Offset(cos(angle),sin(angle))*ring
        if(large)drawCircle(blue.copy(alpha=.14f),stroke*1.5f,cursor)
        drawCircle(if(sweep==0f)gold else blue,stroke*.62f,cursor)
    }
}
