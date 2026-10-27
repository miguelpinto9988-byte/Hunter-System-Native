package com.huntersystem.app

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fondos originales de fantasía oscura, dibujados en Compose Canvas.
 * Siete pantallas principales, estados de recuperación y modales.
 * Sin imágenes remotas, sin WebView y sin alterar el contenido de la app.
 */
private data class RealmPalette(
    val sky:Color,
    val depth:Color,
    val glow:Color,
    val edge:Color,
    val motif:Int
)

private fun realmRoute(route:String):String {
    if(!route.startsWith("overlay-")) return route
    return when(route.removePrefix("overlay-")) {
        "exercise","session","video","series","body" -> "book"
        "mission","mission-calendar","confirm-finish","record","review" -> "plan"
        "routine","assign","routine-editor","custom" -> "routines"
        "evolution","history" -> "stats"
        "goal-edit","delete-goal" -> "objectives"
        "coach","creator","academy" -> "profile"
        else -> "overlay"
    }
}

private fun realmPalette(route:String):RealmPalette=when(realmRoute(route)){
    "home"->RealmPalette(Color(0xFF060D21),Color(0xFF0A1330),Color(0xFF4B71F5),Color(0xFF61DCFF),0)
    "plan"->RealmPalette(Color(0xFF0B0925),Color(0xFF151034),Color(0xFF8B50E6),Color(0xFFA890FF),1)
    "routines"->RealmPalette(Color(0xFF140B20),Color(0xFF1B0D2B),Color(0xFFBD5488),Color(0xFFF49FD3),2)
    "book"->RealmPalette(Color(0xFF031722),Color(0xFF061D2C),Color(0xFF138EAE),Color(0xFF5FECF2),3)
    "stats"->RealmPalette(Color(0xFF051923),Color(0xFF0A1C2A),Color(0xFF28BBA7),Color(0xFF80EDCB),4)
    "objectives"->RealmPalette(Color(0xFF151021),Color(0xFF1C1630),Color(0xFFD5A555),Color(0xFFFFD893),5)
    "profile"->RealmPalette(Color(0xFF0A0C25),Color(0xFF141332),Color(0xFF7D6BDD),Color(0xFFB4AAFF),6)
    "recovery"->RealmPalette(Color(0xFF1A111A),Color(0xFF140B18),Color(0xFFDF6C83),Color(0xFFFFB4BE),7)
    else->RealmPalette(Color(0xFF090D23),Color(0xFF0F1731),Color(0xFF6559A5),Color(0xFFA1A3E9),7)
}

/** Aplica arte detrás de los controles sin capturar gestos ni modificar estado. */
fun Modifier.hunterSceneBackdrop(route:String):Modifier=this.drawBehind {
    val c=realmPalette(route)
    val w=size.width
    val h=size.height
    if(w<=0f || h<=0f) return@drawBehind
    val unit=w.coerceAtMost(h)
    val center=Offset(w*.73f,h*.23f)

    drawRect(brush=Brush.verticalGradient(listOf(c.sky,c.depth,Color(0xFF050A17))))
    drawCircle(
        brush=Brush.radialGradient(
            colors=listOf(c.glow.copy(alpha=.42f),c.glow.copy(alpha=.08f),Color.Transparent),
            center=center,
            radius=unit*.82f
        ),
        radius=unit*.82f,
        center=center
    )

    // Skyline oscuro; los contornos difieren por el motivo de cada sala.
    for(i in 0..20){
        val x=w*i/20f
        val crest=((i*31+11)%13)/13f
        val start=h*(.45f-.18f*crest)
        val tower=unit*(.016f+.023f*((i*7)%5))
        drawRect(color=c.sky.copy(alpha=.72f),topLeft=Offset(x,start),
            size=Size(tower,h-start))
        if(i%4==0){
            val roof=Path().apply{
                moveTo(x-tower*.35f,start)
                lineTo(x+tower*.55f,start-unit*.085f)
                lineTo(x+tower*1.35f,start)
                close()
            }
            drawPath(roof,color=c.sky.copy(alpha=.88f))
        }
    }

    when(c.motif){
        0,1,6->{
            drawCircle(color=c.edge.copy(alpha=.34f),radius=unit*.31f,center=center,
                style=Stroke(width=unit*.004f))
            drawCircle(color=c.glow.copy(alpha=.32f),radius=unit*.21f,center=center,
                style=Stroke(width=unit*.0025f))
            for(i in 0 until 8){
                val angle=i*6.283185f/8f
                drawCircle(color=c.edge.copy(alpha=.39f),radius=unit*.0045f,
                    center=Offset(center.x+unit*.31f*cos(angle),
                        center.y+unit*.31f*sin(angle)))
            }
            if(c.motif==1){
                val rune=Path().apply{
                    moveTo(center.x,center.y-unit*.18f)
                    lineTo(center.x+unit*.13f,center.y)
                    lineTo(center.x,center.y+unit*.18f)
                    lineTo(center.x-unit*.13f,center.y)
                    close()
                }
                drawPath(rune,color=c.edge.copy(alpha=.35f),
                    style=Stroke(width=unit*.003f))
            }
        }
        2->{
            // Forja y armería: armas simbólicas cruzadas.
            drawLine(c.edge.copy(alpha=.37f),
                Offset(center.x-unit*.18f,center.y-unit*.22f),
                Offset(center.x+unit*.17f,center.y+unit*.23f),strokeWidth=unit*.009f)
            drawLine(c.edge.copy(alpha=.30f),
                Offset(center.x+unit*.18f,center.y-unit*.23f),
                Offset(center.x-unit*.17f,center.y+unit*.18f),strokeWidth=unit*.009f)
        }
        3->{
            // Archivo de ejercicios: anaqueles de luz.
            for(i in 0..6){
                val x=w*(.43f+i*.077f)
                drawLine(c.edge.copy(alpha=.26f),Offset(x,h*.08f),
                    Offset(x,h*.53f),strokeWidth=unit*.003f)
            }
            for(i in 0..4){
                val y=h*(.15f+i*.083f)
                drawLine(c.edge.copy(alpha=.24f),Offset(w*.43f,y),
                    Offset(w*.91f,y),strokeWidth=unit*.003f)
            }
        }
        4->{
            // Progreso: observatorio y órbitas.
            for(i in 0..3){
                drawCircle(color=c.edge.copy(alpha=.30f-i*.04f),
                    radius=unit*(.14f+i*.075f),center=center,
                    style=Stroke(width=unit*.0025f))
            }
            for(i in 0..8){
                val x=w*(.48f+i*.05f)
                val y=h*(.36f-.025f*(i%3))
                drawLine(c.edge.copy(alpha=.26f),Offset(x,h*.48f),
                    Offset(x,y),strokeWidth=unit*.013f)
            }
        }
        5->{
            // Objetivos: constelación dorada.
            val points=listOf(.48f to .14f,.66f to .19f,.81f to .13f,
                .90f to .29f,.76f to .39f,.53f to .31f)
            for(i in points.indices){
                val a=points[i]
                val b=points[(i+1)%points.size]
                drawLine(c.edge.copy(alpha=.33f),
                    Offset(w*a.first,h*a.second),
                    Offset(w*b.first,h*b.second),strokeWidth=unit*.0025f)
                drawCircle(c.edge.copy(alpha=.53f),radius=unit*.004f,
                    center=Offset(w*a.first,h*a.second))
            }
        }
        else->{
            drawCircle(color=c.edge.copy(alpha=.26f),radius=unit*.30f,
                center=center,style=Stroke(width=unit*.004f))
        }
    }

    // Partículas fijas: consistentes entre recomposiciones, sin coste de animación.
    for(i in 0 until 55){
        val fx=((i*79+19)%101)/101f
        val fy=((i*43+31)%97)/97f
        drawCircle(color=c.edge.copy(alpha=.12f+.18f*(i%3)/3f),
            radius=unit*(.001f+(i%3)*.0005f),
            center=Offset(w*fx,h*(.025f+fy*.55f)))
    }
    // Legibilidad ante todo: se oscurece el lienzo detrás de tarjetas y texto.
    drawRect(brush=Brush.verticalGradient(
        listOf(Color(0x20040916),Color(0xA0060A19),Color(0xEE060915)),
        startY=h*.05f,endY=h))
}
