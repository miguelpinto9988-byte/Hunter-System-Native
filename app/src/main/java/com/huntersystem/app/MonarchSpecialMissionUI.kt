package com.huntersystem.app

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.ZoneId

/** Una sola misión semanal rotativa, visible en Inicio y renovada los domingos. */
@Composable
fun MonarchSpecialMissionsPanel(ui:NativeUi) {
 val store=ui.store
 store.revision.intValue
 val colors=LocalHunter.current
 val zone=remember{ZoneId.systemDefault()}
 var now by remember{mutableLongStateOf(System.currentTimeMillis())}
 val sunday=MonarchMissionCatalog.weekStart(now,zone)
 LaunchedEffect(sunday){
  ui.attempt{store.syncMonarchWeek(now,zone)}
 }
 LaunchedEffect(Unit){
  while(true){delay(30000L);now=System.currentTimeMillis()}
 }
 val current=MonarchMissionCatalog.missionFor(sunday,level(store.adjusted()),zone)
 val run=store.state.optJSONObject("monarchSpecialMission")
 val weekly=run?.s("weekStart")==current.weekStart
 val status=if(weekly)MonarchMissionCatalog.status(run,now) else "none"
 val active=status=="active"
 val mission=if(weekly&&run!=null)runCatching{MonarchMissionCatalog.fromRun(run)}.getOrDefault(current) else current
 val history=store.state.a("monarchSpecialHistory").objects()
 val seals=history.count{it.s("status")=="completed"}+if(status=="completed")1 else 0
 val end=MonarchMissionCatalog.deadline(sunday,zone)
 val ms=maxOf(0L,end-now)
 val d=ms/86400000L
 val h=(ms/3600000L)%24
 val m=(ms/60000L)%60
 val deadlineText=d.toString()+" d  "+h.toString()+" h  "+m.toString()+" min"
 HPanel(padding=18){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
   verticalAlignment=Alignment.CenterVertically){
   Eyebrow("EVENTO SEMANAL · MISIÓN MONARCA")
   HText("✦ "+seals+" sellos",size=14,color=colors.accent,bold=true)
  }
  Heading(mission.title,size=27)
  HText("Ciclo Monarca · semana "+current.cycleWeek+" de 4",size=14,bold=true,color=colors.accent)
  HText(mission.story,size=15,color=colors.muted)
  HText("CIERRE AUTOMÁTICO EL DOMINGO · TIEMPO RESTANTE",size=13,color=colors.muted,bold=true)
  Heading(deadlineText,size=24)
  HText("Metas acumuladas para toda la semana, no para una sola sesión. Nueva combinación cada domingo.",size=15,color=colors.muted)
  if(weekly&&run!=null)HProgress(MonarchMissionCatalog.progress(run,mission))
  mission.goals.forEach{goal->
   val exercise=store.catalog.byId[goal.exerciseId]
   val count=if(weekly&&run!=null)run.o("counts").i(goal.exerciseId) else 0
   val unit=if(exercise?.unit=="sec")"segundos" else "repeticiones"
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
    verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){
     HText(exercise?.name?:goal.exerciseId,size=16,bold=true)
     HText(count.toString()+" / "+goal.target+" "+unit,size=14,color=colors.muted)
    }
    HButton("Técnica"){ui.open("exercise",obj("id" to goal.exerciseId))}
   }
   if(active)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
    HButton("−1",Modifier.weight(1f),enabled=count>0){
     ui.attempt{store.recordMonarchProgress(goal.exerciseId,-1)}
    }
    HButton("+1",Modifier.weight(1f),enabled=count<goal.target){
     ui.attempt{store.recordMonarchProgress(goal.exerciseId,1)}
    }
    HButton("+5",Modifier.weight(1f),enabled=count<goal.target){
     ui.attempt{store.recordMonarchProgress(goal.exerciseId,5)}
    }
    HButton("+10",Modifier.weight(1f),enabled=count<goal.target){
     ui.attempt{store.recordMonarchProgress(goal.exerciseId,10)}
    }
   }
  }
  when(status){
   "active"->{
    HText("Registra manualmente solo lo que realizaste. Distribuye el esfuerzo, descansa y detente ante dolor o mareo.",size=14,color=colors.muted)
    val reached=mission.goals.all{(run?.o("counts")?.i(it.exerciseId)?:0)>=it.target}
    HButton("RECLAMAR SELLO MONARCA",Modifier.fillMaxWidth(),primary=true,enabled=reached){
     ui.attempt{store.completeMonarchMission()}
    }
    HButton("Detener sin penalización",Modifier.fillMaxWidth()){ui.attempt{store.stopMonarchMission()}}
   }
   "completed"->HText("Misión completada: sello obtenido. La siguiente se renovará automáticamente el domingo.",size=15,color=colors.accent)
   "stopped"->HText("Misión detenida sin penalización. Se conserva tu avance; la siguiente llegará el domingo.",size=15,color=colors.muted)
   "expired"->HText("La ventana semanal concluyó; no hay penalización. El domingo comienza una nueva.",size=15,color=colors.muted)
   else->{
    val reason=MonarchMissionCatalog.eligibleReason(store,mission,now)
    if(reason!=null)HText(reason,size=14,color=colors.muted)
    // El botón solo crea la misión: no comienza automáticamente el ejercicio.
    var consent by remember(current.weekStart){mutableStateOf(false)}
    if(reason==null)HCheck("Acepto voluntariamente el desafío. Puedo descansar o detenerme sin penalización.",consent){
     consent=!consent
    }
    HButton("ACTIVAR MISIÓN DE ESTA SEMANA",Modifier.fillMaxWidth(),primary=true,enabled=reason==null&&consent){
     ui.attempt{store.startMonarchMission()}
    }
   }
  }
 }
}