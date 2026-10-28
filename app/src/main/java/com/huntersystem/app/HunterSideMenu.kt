package com.huntersystem.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Menú lateral Monarca. Todos los pictogramas son PNG reales de la biblioteca,
 * las rutas son las mismas que usa la navegación inferior.
 */
@Composable
fun HunterSideMenu(ui:NativeUi,onClose:()->Unit){
 val c=LocalHunter.current
 Column(Modifier.width(304.dp).fillMaxHeight().background(Color(0xFF050A1B))){
  Box(Modifier.fillMaxWidth().height(218.dp)){
   AssetImage("hunter-png/wallpapers/profile.png",Modifier.fillMaxSize(),
      scale=ContentScale.Crop)
   Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
      listOf(Color(0x62040819),Color(0xE3071025)))))
   Column(Modifier.align(Alignment.BottomStart).padding(horizontal=20.dp,vertical=17.dp),
     verticalArrangement=Arrangement.spacedBy(7.dp)){
    Row(verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(10.dp)){
     HunterPngIcon("profile",Modifier.size(66.dp),"Perfil del cazador")
     Column {
      Eyebrow("HUNTER SYSTEM")
      Heading("SISTEMA MONARCA",size=21)
     }
    }
    HText(ui.store.profile?.s("name","Cazador")?:"Cazador",size=19,bold=true)
    HText("Nivel ${ui.store.stats().i("level",1)} · Rango ${rankKey(ui.store.stats().i("level",1))}",
      size=14,color=Color(0xFFB7D9FA))
   }
  }
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
   verticalArrangement=Arrangement.spacedBy(5.dp)){
   val routes=listOf(
      Triple("home","Panel","panel"),
      Triple("plan","Misiones","missions"),
      Triple("routines","Rutinas","routines"),
      Triple("book","Ejercicios","exercises"),
      Triple("stats","Progreso y estadísticas","progress"),
      Triple("objectives","Mis metas","goals"),
      Triple("profile","Perfil","profile"))
   for((route,title,icon) in routes){
     HunterSideMenuItem(title,icon,ui.tab==route){
       ui.go(route);onClose()
     }
   }
   Spacer(Modifier.height(7.dp))
   HorizontalDivider(color=c.line.copy(alpha=.72f))
   Spacer(Modifier.height(7.dp))
   HunterSideMenuItem("Academia","academy",false){
      ui.open("academy");onClose()
   }
   HunterSideMenuItem("Recordatorios","reminders",false){
      ui.open("reminders");onClose()
   }
   HunterSideMenuItem("Configuración y datos","settings",false){
      ui.go("profile");onClose()
   }
   HText("Fondos PNG · Íconos PNG con transparencia",
      Modifier.padding(start=10.dp,top=15.dp,bottom=10.dp),
      size=12,color=c.muted)
  }
 }
}

@Composable
private fun HunterSideMenuItem(title:String,icon:String,selected:Boolean,onClick:()->Unit){
 val c=LocalHunter.current
 val shape=RoundedCornerShape(13.dp)
 Row(
  Modifier.fillMaxWidth().clip(shape)
    .background(if(selected)c.accent.copy(alpha=.20f) else Color.Transparent)
    .border(1.dp,if(selected)c.accent.copy(alpha=.65f) else Color.Transparent,shape)
    .clickable(role=Role.Button,onClick=onClick)
    .padding(horizontal=11.dp,vertical=6.dp),
  verticalAlignment=Alignment.CenterVertically,
  horizontalArrangement=Arrangement.spacedBy(12.dp)
 ){
  HunterPngIcon(icon,Modifier.size(62.dp),title)
  HText(title,Modifier.weight(1f),size=16,bold=selected,
    color=if(selected)c.accent else c.text)
 }
}
