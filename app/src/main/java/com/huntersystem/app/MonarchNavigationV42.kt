package com.huntersystem.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Navegación Monarca V42.2. Capa de presentación, sin rutas de red ni nuevos estados persistidos. */
data class MonarchDestination(val id:String,val title:String)
object MonarchNavigation {
    val tabs=listOf(
        MonarchDestination("home","Panel"),
        MonarchDestination("plan","Misiones"),
        MonarchDestination("routines","Rutinas"),
        MonarchDestination("book","Ejercicios"),
        MonarchDestination("profile","Perfil")
    )
    val routes=tabs.map{it.id}.toSet()+setOf("objectives","stats")
    fun activeTab(route:String):String=if(route=="objectives"||route=="stats")"home" else route
    fun index(route:String):Int=tabs.indexOfFirst{it.id==activeTab(route)}.coerceAtLeast(0)
    const val tabletWidthDp=840
}

@Composable
fun MonarchBootScreen(error:String?,retry:()->Unit){
    val pulse=if(LocalMonarchMotion.current.reduced) 1f else {
        val infinite=rememberInfiniteTransition(label="monarch-boot")
        val alpha by infinite.animateFloat(
            initialValue=.58f,targetValue=1f,
            animationSpec=infiniteRepeatable(tween(1300,easing=FastOutSlowInEasing),RepeatMode.Reverse),
            label="boot-pulse"
        )
        alpha
    }
    Box(Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFF080D19),Color(0xFF111C31),Color(0xFF080D19))))
        .systemBarsPadding(),contentAlignment=Alignment.Center){
        Column(Modifier.fillMaxWidth().padding(horizontal=32.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(16.dp)){
            Box(Modifier.size(112.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(
                    Color(0xFF25C8FF).copy(alpha=.18f*pulse),
                    Color(0xFF7F5CFF).copy(alpha=.06f*pulse),
                    Color.Transparent))),
                contentAlignment=Alignment.Center){
                MonarchGlyph("plan",Modifier.size(65.dp),
                    color=Color(0xFF25C8FF).copy(alpha=pulse))
            }
            Text("HUNTER SYSTEM",color=Color(0xFFE8F1FF),
                fontFamily=Barlow,fontWeight=FontWeight.Bold,fontSize=32.sp,letterSpacing=1.sp)
            Text("SISTEMA MONARCA  •  V42",color=Color(0xFF25C8FF),
                fontFamily=Barlow,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
            Spacer(Modifier.height(14.dp))
            if(error==null){
                LinearProgressIndicator(
                    modifier=Modifier.fillMaxWidth(.65f).height(4.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color=Color(0xFF25C8FF),
                    trackColor=Color(0xFF24324B)
                )
                Text("Preparando tus datos locales…",color=Color(0xFFB4C6DB),
                    fontFamily=Barlow,fontSize=16.sp)
            }else{
                Text("NO SE PUDO INICIAR",color=Color(0xFFF3B0B8),
                    fontFamily=Barlow,fontWeight=FontWeight.SemiBold,fontSize=17.sp)
                Text(error,color=Color(0xFFE8F1FF),fontFamily=Barlow,fontSize=16.sp)
                Button(onClick=retry,colors=ButtonDefaults.buttonColors(
                    containerColor=Color(0xFF25C8FF),contentColor=Color(0xFF080D19))){
                    Text("Reintentar",fontFamily=Barlow,fontWeight=FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MonarchNavItem(destination:MonarchDestination,selected:Boolean,vertical:Boolean,onSelect:()->Unit){
    val c=LocalHunter.current
    val tint by animateColorAsState(
        if(selected)c.accent else c.muted,
        animationSpec=if(LocalMonarchMotion.current.reduced)snap() else tween(180),
        label="nav-tint")
    val shape=RoundedCornerShape(18.dp)
    val item=Modifier
        .clip(shape)
        .background(if(selected)c.selected else Color.Transparent)
        .border(1.dp,if(selected)c.accent.copy(alpha=.56f) else Color.Transparent,shape)
        .semantics(mergeDescendants=true){
            contentDescription=destination.title+(if(selected)", seleccionada" else "")
            this.selected=selected
        }
        .clickable(role=Role.Tab,onClick=onSelect)
    if(vertical){
        Column(item.width(76.dp).defaultMinSize(minHeight=76.dp).padding(vertical=10.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(6.dp)){
            MonarchGlyph(destination.id,Modifier.size(25.dp),color=tint)
            HText(destination.title,size=12,bold=selected,color=tint,maxLines=1)
        }
    }else{
        Column(item.fillMaxWidth().fillMaxHeight().padding(vertical=8.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(4.dp)){
            MonarchGlyph(destination.id,Modifier.size(24.dp),color=tint)
            HText(destination.title,size=11,bold=selected,color=tint,maxLines=1)
        }
    }
}

/** Barra inferior compacta. Respeta gesto de navegación Android y borde de ventana. */
@Composable
fun MonarchBottomNavigation(ui:NativeUi){
    val c=LocalHunter.current
    Row(Modifier.fillMaxWidth()
        .background(Brush.verticalGradient(listOf(c.surface,c.panel)))
        .border(1.dp,c.line)
        .navigationBarsPadding().height(75.dp).padding(horizontal=6.dp,vertical=6.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(3.dp)){
        for(destination in MonarchNavigation.tabs){
            Box(Modifier.weight(1f).fillMaxHeight()){
                MonarchNavItem(destination,
                    selected=MonarchNavigation.activeTab(ui.tab)==destination.id,
                    vertical=false,
                    onSelect={if(ui.tab!=destination.id)ui.go(destination.id)})
            }
        }
    }
}

/** Rail nativo para pantallas >= 840dp; evita estirar los cinco botones en tablets. */
@Composable
fun MonarchNavigationRail(ui:NativeUi){
    val c=LocalHunter.current
    Column(Modifier.width(96.dp).fillMaxHeight()
        .background(Brush.verticalGradient(listOf(c.surface,c.panel)))
        .border(1.dp,c.line).padding(horizontal=8.dp,vertical=16.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(14.dp)){
        MonarchGlyph("plan",Modifier.size(32.dp),color=c.accent)
        Spacer(Modifier.height(12.dp))
        for(destination in MonarchNavigation.tabs){
            MonarchNavItem(destination,
                selected=MonarchNavigation.activeTab(ui.tab)==destination.id,
                vertical=true,
                onSelect={if(ui.tab!=destination.id)ui.go(destination.id)})
        }
    }
}

/** Transiciones breves; AnimatedContent conserva la identidad de la ruta y evita parpadeos. */
@Composable
fun MonarchScreenTransition(
    destination:String,
    modifier:Modifier=Modifier,
    content:@Composable (String)->Unit
){
    val motion=LocalMonarchMotion.current
    AnimatedContent(
        targetState=destination,
        modifier=modifier,
        transitionSpec={
            if(motion.reduced) EnterTransition.None togetherWith ExitTransition.None
            else {
            val direction=if(MonarchNavigation.index(targetState)>=MonarchNavigation.index(initialState))1 else -1
            (fadeIn(tween(180))+slideInHorizontally(
                animationSpec=tween(210,easing=FastOutSlowInEasing),
                initialOffsetX={width->direction*(width/12)}
            )) togetherWith
            (fadeOut(tween(100))+slideOutHorizontally(
                animationSpec=tween(160),
                targetOffsetX={width->-direction*(width/12)}
            ))
            }
        },label="monarch-route"
    ){page->content(page)}
}

/** La recuperación mantiene el archivo defectuoso intacto hasta que se importa una copia válida. */
@Composable
fun MonarchRecoveryScreen(ui:NativeUi,reason:String){
    val c=LocalHunter.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal=20.dp,vertical=28.dp),
        verticalArrangement=Arrangement.spacedBy(20.dp)){
        Eyebrow("PROTECCIÓN DE DATOS · LOCAL")
        Heading("Recuperar tu perfil",size=32)
        HPanel{
            HText("No se pudo leer el archivo de guardado.",size=20,bold=true)
            HText(reason,size=16,color=c.muted)
            HText("Tus datos no se borrarán. Selecciona una copia de seguridad para validarla antes de restaurar.",
                size=15,color=c.muted)
            HButton("Elegir copia de seguridad",Modifier.fillMaxWidth(),primary=true){
                ui.import()
            }
        }
    }
}
