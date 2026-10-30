package com.huntersystem.app

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Galería de alarmas, configuración y visualización del próximo aviso. */
@Composable
fun HunterAlarmScreen(ui:NativeUi){
 val store=ui.store
 val context=LocalContext.current
 val colors=LocalHunter.current
 val clock=remember{DateTimeFormatter.ofPattern("HH:mm")}
 val alarms=remember{mutableStateListOf<HunterAlarm>().apply{addAll(HunterAlarms.list(store))}}
 var editId by remember{mutableStateOf<String?>(null)}
 var editing by remember{mutableStateOf(false)}
 var title by remember{mutableStateOf("Revisar mis misiones")}
 var time by remember{mutableStateOf(LocalTime.of(18,0))}
 var tone by remember{mutableStateOf("energetic")}
 var detail by remember{mutableStateOf("")}
 val days=remember{mutableStateListOf<Int>()}
 var deleteId by remember{mutableStateOf<String?>(null)}
 val dayLabels=listOf("L","M","X","J","V","S","D")
 val now=ZonedDateTime.now()
 fun edit(item:HunterAlarm?=null){
  editId=item?.id
  title=item?.title?:"Revisar mis misiones"
  time=item?.time?:LocalTime.of(18,0)
  tone=item?.tone?:"energetic"
  detail=item?.detail?:""
  days.clear();days.addAll(item?.days?.sorted()?:listOf(1,2,3,4,5,6,7))
  editing=true
 }
 fun saveAlarms(next:List<HunterAlarm>){
  ui.attempt{
   HunterAlarms.persist(store,next)
   alarms.clear();alarms.addAll(next)
  }
 }
 Eyebrow("SISTEMA MONARCA · RELOJ DEL CAZADOR")
 Heading("Alarmas del cazador",size=32)
 HText("Tus momentos importantes, a tu ritmo. Sin conexión y con control individual.",size=15,color=colors.muted)
 val upcoming=alarms.mapNotNull{a->HunterAlarms.next(a,now)?.let{a to it}}.minByOrNull{it.second.toInstant()}
 HPanel(padding=19){
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
   Box(Modifier.size(43.dp).background(
      Brush.linearGradient(listOf(colors.accent.copy(alpha=.27f),colors.secondary.copy(alpha=.18f))),
      RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center){
    HText("◷",size=28,bold=true,color=colors.accent)
   }
   Column {
    Eyebrow("PRÓXIMO AVISO")
    Heading(upcoming?.first?.time?.format(clock)?:"— — : — —",size=31)
   }
  }
  if(upcoming!=null){
   val at=upcoming.second
   HText(upcoming.first.title,size=17,bold=true)
   HText(at.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL,Locale("es"))+
     " · "+at.toLocalDate(),size=14,color=colors.muted)
  }else HText("Añade una alarma o activa una de las existentes.",size=15,color=colors.muted)
  HText(if(HunterAlarms.exactAllowed(context))"Precisión exacta disponible"
     else "Modo aproximado · Android puede retrasar el aviso",
    size=13,color=colors.muted)
 }
 if(Build.VERSION.SDK_INT>=31&&!HunterAlarms.exactAllowed(context)){
  HPanel(padding=16){
   HText("¿Quieres que suene a la hora exacta?",size=17,bold=true)
   HText("Android requiere autorizar «Alarmas y recordatorios». Es opcional: los avisos aproximados seguirán funcionando.",
    size=14,color=colors.muted)
   HButton("Permitir alarmas exactas",Modifier.fillMaxWidth()){
    ui.attempt{
     context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
       Uri.parse("package:"+context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
   }
  }
 }
 HPanel(padding=15){
  HText("Avisos enérgicos o suaves",size=17,bold=true)
  HText("Elige el estilo de cada alarma y ajusta su sonido desde los canales de notificación de Android.",
    size=14,color=colors.muted)
  HButton("Autorizar notificaciones",Modifier.fillMaxWidth()){ui.requestNotifications()}
 }
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
  verticalAlignment=Alignment.CenterVertically){
  Heading("Mis alarmas ("+alarms.size+"/12)",size=23)
  HButton("+ Nueva",primary=true,enabled=alarms.size<12){edit()}
 }
 if(alarms.isEmpty()&&!editing){
  HPanel(padding=18){
   HText("Todavía no tienes alarmas.",bold=true)
   HText("Configura avisos para misiones, descansos o para revisar tus logros.",size=15,color=colors.muted)
   HButton("Crear mi primera alarma",Modifier.fillMaxWidth(),primary=true){edit()}
  }
 }
 for(item in alarms){
  val next=HunterAlarms.next(item,now)
  HPanel(padding=15,decor=false){
   Row(verticalAlignment=Alignment.CenterVertically,
    horizontalArrangement=Arrangement.spacedBy(12.dp)){
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
     HText(item.time.format(clock),size=33,bold=true,
      color=if(item.enabled)colors.text else colors.muted)
     HText(item.title,size=16,bold=true,maxLines=2)
     HText(item.days.sorted().joinToString(" · "){dayLabels[it-1]}+
       "  ·  "+(if(item.tone=="soft")"Suave" else "Enérgico"),
       size=12,color=colors.muted)
    }
    HCheck("Activa",item.enabled,Modifier.width(100.dp)){
     saveAlarms(alarms.map{if(it.id==item.id)it.copy(enabled=!it.enabled) else it})
    }
   }
   HText(if(item.enabled&&next!=null)
     "Próxima: "+next.toLocalDate()+" · "+next.toLocalTime().format(clock)
     else "Pausada",size=13,color=colors.muted)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    HButton("Editar",Modifier.weight(1f)){edit(item)}
    HButton("Eliminar",Modifier.weight(1f)){deleteId=item.id}
   }
  }
 }
 if(editing){
  HPanel(padding=17){
   Eyebrow(if(editId==null)"NUEVA ALARMA" else "EDITAR ALARMA")
   Heading("Configura tu aviso",size=25)
   HInput(title,{title=it.take(70)},"Nombre de la alarma")
   HButton("◷  "+time.format(clock)+"  ·  Cambiar hora",Modifier.fillMaxWidth()){
    TimePickerDialog(context,{_,hour,minute->time=LocalTime.of(hour,minute)},
      time.hour,time.minute,true).show()
   }
   HText("Días de repetición",size=16,bold=true)
   Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement=Arrangement.spacedBy(5.dp)){
    for((index,label) in dayLabels.withIndex()){
     val day=index+1
     HChip(label,day in days){
      if(day in days)days.remove(day) else days.add(day)
     }
    }
   }
   HSelect("Intensidad del aviso",tone,listOf(
    "energetic" to "Enérgico · alarma",
    "soft" to "Suave · notificación"
   )){tone=it}
   HInput(detail,{detail=it.take(140)},"Nota opcional",multiline=true)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    HButton("Cancelar",Modifier.weight(1f)){editing=false}
    HButton("Guardar",Modifier.weight(1f),primary=true){
     ui.attempt{
      require(title.trim().isNotBlank()){"Escribe un nombre para la alarma"}
      require(days.isNotEmpty()){"Elige al menos un día"}
      val selected=days.toSet()
      val result=if(editId==null)HunterAlarms.newAlarm(title,time,selected,tone,detail)
       else HunterAlarm(editId!!,title.trim().take(70),time,selected,
        alarms.firstOrNull{it.id==editId}?.enabled?:true,tone,detail)
      val updated=if(editId==null)alarms.toList()+result
       else alarms.map{if(it.id==editId)result else it}
      HunterAlarms.persist(store,updated)
      alarms.clear();alarms.addAll(updated)
      editing=false
      if(Build.VERSION.SDK_INT>=33)ui.requestNotifications()
      ui.message("Alarma guardada")
     }
    }
   }
  }
 }
 if(deleteId!=null){
  AlertDialog(onDismissRequest={deleteId=null},
   title={HText("Eliminar alarma",size=23,bold=true)},
   text={HText("Dejará de sonar sin alterar tus misiones ni tus registros.",size=15)},
   confirmButton={TextButton(onClick={
    val id=deleteId!!
    saveAlarms(alarms.filterNot{it.id==id})
    if(editId==id)editing=false
    deleteId=null
   }){HText("Eliminar",size=16,color=colors.accent)}},
   dismissButton={TextButton(onClick={deleteId=null}){HText("Cancelar",size=16)}})
 }
 HText("Sin permiso de notificaciones no se mostrarán avisos. En modo aproximado, Android puede retrasar su entrega.",
  size=13,color=colors.muted)
}
