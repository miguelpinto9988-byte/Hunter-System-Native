package com.huntersystem.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Los ciclos heredados quedan consultables y se pueden cerrar sin perder sus registros. */
@Composable
fun MonarchSpecialMissionsPanel(ui:NativeUi) {
 val store=ui.store
 store.revision.intValue
 val legacy=store.state.optJSONObject("monarchSpecialMission")
 if(legacy?.s("status")=="active"){
  val id=legacy.s("id")
  val mission=MonarchMissionCatalog.fromRun(legacy)
  MonarchBossMissionCard(id){
   MonarchBossMissionHeading(id,mission.title,legacy.i("cycleWeek"),"active")
   HText("Se conserva lo que ya registraste. Puedes cerrar el desafío anterior sin penalización.")
   var detailsExpanded by rememberSaveable(id){mutableStateOf(false)}
   HButton(if(detailsExpanded) "Ocultar resultados" else "Ver resultados anteriores",
    Modifier.fillMaxWidth()){detailsExpanded=!detailsExpanded}
   if(detailsExpanded){
    val counts=legacy.o("counts").entries()
    if(counts.isEmpty())HText("Sin resultados declarados.")
    for((variant, count) in counts)
     HText("${store.catalog.byId[variant]?.name ?: variant} · $count")
   }
   HButton("Cerrar desafío anterior",Modifier.fillMaxWidth()){
    ui.attempt{
     val now=System.currentTimeMillis()
     val status=MonarchMissionCatalog.status(legacy,now)
     legacy.put("status",if(status=="expired") "expired" else "stopped")
     legacy.put("finishedAt",now)
     store.save()
    }
   }
  }
 } else MonarchFiveDayChallenge(ui)
}

/**
 * El arte del jefe forma parte del fondo de la MISMA tarjeta: no existe una
 * subventana de imagen independiente. El degradado inferior se funde con la
 * superficie del panel y mantiene legibles los controles y el texto.
 *
 * Se usan los tres JPG ya empaquetados: guardia (Igris), fortaleza (Beru)
 * y trono (Baran). No se modifican los datos ni la rotación de misiones.
 */
@Composable
private fun MonarchBossMissionCard(
 id:String,
 content:@Composable ColumnScope.()->Unit
){
 val colors=LocalHunter.current
 val shape=RoundedCornerShape(HunterDesignSystem.Dimensions.cornerCard)
 Box(Modifier.fillMaxWidth().clip(shape)
  .background(Brush.verticalGradient(listOf(colors.surface,colors.panel)))
  .monarchOutline(shape)){
  // Todas las misiones (Igris, Beru y Baran) comparten el fondo a
  // tamaño COMPLETO. matchParentSize toma la altura final del contenido,
  // incluso cuando se despliegan los objetivos; no deja cortes a mitad.
  AssetImage(
   "monarch-boss-original/"+id+".jpg",
   Modifier.matchParentSize(),
   scale=ContentScale.Crop,
   alignment=Alignment.BottomCenter,
   description=null
  )
  // La imagen continúa hasta el borde inferior de la tarjeta.
  // Los degradados son semitransparentes para que el arte siga visible.
  Box(Modifier.matchParentSize().background(
   Brush.horizontalGradient(listOf(
    Color(0xB8080D1B),Color(0x97080D1B),Color(0x60080D1B)))))
  Box(Modifier.matchParentSize().background(
   Brush.verticalGradient(listOf(
    Color(0x65080D1B),Color(0x42080D1B),
    colors.panel.copy(alpha=.53f),
    colors.panel.copy(alpha=.72f)))))
  Column(Modifier.fillMaxWidth().padding(16.dp),
   verticalArrangement=Arrangement.spacedBy(HunterDesignSystem.Dimensions.spacing),
   content=content)
 }
}

/** Solo textos sobre el fondo compartido, sin bordes ni contenedor de imagen. */
@Composable
private fun MonarchBossMissionHeading(id:String,title:String,week:Int,status:String){
 val boss=when(id){
  "guardia"->"IGRIS · COMANDANTE CARMESÍ"
  "fortaleza"->"BERU · REY HORMIGA"
  else->"BARAN · MONARCA DE LAS LLAMAS BLANCAS"
 }
 val accent=when(id){
  "guardia"->Color(0xFFFF809A)
  "fortaleza"->Color(0xFFA999FF)
  else->Color(0xFFC6B2FF)
 }
 Box(Modifier.fillMaxWidth().heightIn(min=188.dp)){
  Column(Modifier.align(Alignment.TopStart).fillMaxWidth(.72f),
   verticalArrangement=Arrangement.spacedBy(6.dp)){
   Text("JEFE SEMANAL  ·  CICLO "+week+"/4",color=accent,
    fontFamily=Barlow,fontSize=11.sp,fontWeight=FontWeight.Bold)
   Text(title,color=Color(0xFFF5F7FF),fontFamily=Barlow,fontSize=25.sp,
    lineHeight=27.sp,fontWeight=FontWeight.Bold)
   Text("ARTE PROMOCIONAL · "+boss,color=Color(0xFFCED8F3),fontFamily=Barlow,
    fontSize=11.sp,lineHeight=14.sp,maxLines=2)
  }
  Text(when(status){
   "active"->"EN CURSO"
   "completed"->"COMPLETADA"
   "expired"->"FINALIZADA"
   "stopped"->"DETENIDA"
   else->"DISPONIBLE"
  },
   modifier=Modifier.align(Alignment.BottomEnd)
    .clip(RoundedCornerShape(9.dp)).background(Color(0xDD0A1027))
    .padding(horizontal=9.dp,vertical=5.dp),
   fontFamily=Barlow,fontSize=10.sp,fontWeight=FontWeight.Bold,color=accent)
 }
}
