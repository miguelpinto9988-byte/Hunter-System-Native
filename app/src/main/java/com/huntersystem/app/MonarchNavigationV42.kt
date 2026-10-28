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
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
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

/**
 * Pantalla de carga 100% nativa. Sustituye la composición de arranque Monarca V42
 * por la ilustración animada aprobada; conserva la vía de recuperación al fallar.
 * La barra que aparece dentro del arte es decorativa, no un porcentaje falso.
 */
@Composable
fun MonarchBootScreen(error:String?,retry:()->Unit){
    Box(Modifier.fillMaxSize().background(Color(0xFF06091A))){
        HunterAnimatedBootArtwork(
            Modifier.fillMaxSize().semantics {
                contentDescription = "Hunter System, Sistema Monarca. " +
                    if(error == null) "Preparando datos locales" else "Error al iniciar"
                if(error == null) progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
        )
        if(error != null) {
            Column(
                Modifier.align(Alignment.BottomCenter)
                    .systemBarsPadding().padding(start=26.dp,end=26.dp,bottom=30.dp)
                    .fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Color(0xF2111627))
                    .border(1.dp,Color(0xFF2C98CE),RoundedCornerShape(18.dp))
                    .padding(20.dp),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(12.dp)
            ){
                Text("NO SE PUDO INICIAR",color=Color(0xFFF3B0B8),
                    fontFamily=Barlow,fontWeight=FontWeight.SemiBold,fontSize=17.sp)
                Text(error,color=Color(0xFFE8F1FF),fontFamily=Barlow,fontSize=15.sp)
                Button(onClick=retry,colors=ButtonDefaults.buttonColors(
                    containerColor=Color(0xFF25C8FF),contentColor=Color(0xFF080D19))){
                    Text("Reintentar",fontFamily=Barlow,fontWeight=FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun monarchPngDestination(id:String):String=when(id){
 "home"->"panel";"plan"->"missions";"routines"->"routines";"book"->"exercises";
 "stats"->"progress";"objectives"->"goals";else->"profile"
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
            HunterPngIcon(monarchPngDestination(destination.id),Modifier.size(45.dp),destination.title)
            HText(destination.title,size=12,bold=selected,color=tint,maxLines=1)
        }
    }else{
        Column(item.fillMaxWidth().fillMaxHeight().padding(vertical=5.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(4.dp)){
            HunterPngIcon(monarchPngDestination(destination.id),Modifier.size(44.dp),destination.title)
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
        .navigationBarsPadding().height(86.dp).padding(horizontal=6.dp,vertical=5.dp),
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
        HunterPngIcon("missions",Modifier.size(53.dp),"Misiones")
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
