package com.huntersystem.app

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Sección de Inicio con reloj real y registro local, sin WebView. */
@Composable
fun MonarchSpecialMissionsPanel(ui:NativeUi){
 val store=ui.store
 store.revision.intValue
 val colors=LocalHunter.current
 var picked by rememberSaveable{mutableStateOf("guardia")}
 var accepted by rememberSaveable(picked){mutableStateOf(false)}
 val run=store.state.optJSONObject("monarchSpecialMission")
 var now by remember{mutableLongStateOf(System.currentTimeMillis())}
 val status=MonarchMissionCatalog.status(run,now)
 val active=status=="active"
 LaunchedEffect(run?.optLong("startedAt"),run?.s("status")){
  while(true){
   delay(if(active)1000L else 60000L)
   now=System.currentTimeMillis()
  }
 }
 val selected=if(active&&run!=null)run.s("id") else picked
 val mission=MonarchMissionCatalog.spec(selected)
 val seals=store.state.a("monarchSpecialHistory").objects().count{it.s("status")=="completed"}+
  if(status=="completed")1 else 0
 HPanel(padding=18){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
   verticalAlignment=Alignment.CenterVertically){
   Eyebrow("EVENTO ESPECIAL  ·  MISIÓN MONARCA")
   HText("✦ "+seals+" sellos",size=14,color=colors.accent,bold=true)
  }
  Heading("La prueba del Monarca",size=28)
  HText("Retos voluntarios con cuenta regresiva y registro manual. No sustituyen tu misión diaria.",size=15,color=colors.muted)
  if(active&&run!=null){
   val seconds=MonarchMissionCatalog.remaining(run,now)/1000L
   val formatted="%02d:%02d:%02d".format(seconds/3600,seconds/60%60,seconds%60)
   Heading(mission.title,size=24)
   HText("TIEMPO RESTANTE  "+formatted,size=19,bold=true,color=colors.accent)
   HProgress(MonarchMissionCatalog.progress(run,mission))
   mission.goals.forEach{goal->
    val exercise=store.catalog.byId[goal.exerciseId]
    val quantity=run.o("counts").i(goal.exerciseId)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
     verticalAlignment=Alignment.CenterVertically){
     Column(Modifier.weight(1f)){
      HText(exercise?.name?:goal.exerciseId,size=16,bold=true)
      HText(quantity.toString()+" / "+goal.target+" "+if(exercise?.unit=="sec")"segundos" else "repeticiones",size=13,color=colors.muted)
     }
     HButton("Ver técnica"){ui.open("exercise",obj("id" to goal.exerciseId))}
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
     HButton("−1",Modifier.weight(1f),enabled=quantity>0){
      ui.attempt{store.recordMonarchProgress(goal.exerciseId,-1)}
     }
     HButton("+1",Modifier.weight(1f),enabled=quantity<goal.target){
      ui.attempt{store.recordMonarchProgress(goal.exerciseId,1)}
     }
     HButton("+5",Modifier.weight(1f),enabled=quantity<goal.target){
      ui.attempt{store.recordMonarchProgress(goal.exerciseId,5)}
     }
     HButton("+10",Modifier.weight(1f),enabled=quantity<goal.target){
      ui.attempt{store.recordMonarchProgress(goal.exerciseId,10)}
     }
    }
   }
   HText("Las cantidades son declaradas por ti. Descansa entre series; detén el reto si aparece dolor, mareo o malestar.",size=14,color=colors.muted)
   val allDone=mission.goals.all{run.o("counts").i(it.exerciseId)>=it.target}
   HButton("RECLAMAR SELLO MONARCA",Modifier.fillMaxWidth(),primary=true,enabled=allDone){
    ui.attempt{store.completeMonarchMission()}
   }
   HButton("Detener sin penalización",Modifier.fillMaxWidth()){
    ui.attempt{store.stopMonarchMission()}
   }
  }else{
   if(run!=null){
    val note=when(status){
     "completed"->"Misión lograda: "+MonarchMissionCatalog.spec(run.s("id")).title+". Sello guardado."
     "expired"->"Se agotó el tiempo del último desafío. Tu progreso se conserva y no hay penalización."
     "stopped"->"Desafío detenido. El registro se conserva sin penalización."
     else->""
    }
    if(note.isNotBlank())HText(note,size=15,color=colors.muted)
   }
   Chips(MonarchMissionCatalog.missions.map{it.id to it.title},picked){picked=it}
   Heading(mission.title,size=23)
   HText(mission.story,size=15,color=colors.muted)
   HText("Límite: "+mission.minutes+" min. Próxima misión: a partir de "+mission.cooldownDays+" días.",size=14,bold=true)
   mission.goals.forEach{goal->
    val exercise=store.catalog.byId[goal.exerciseId]
    HText("• "+goal.target+" "+(if(exercise?.unit=="sec")"segundos" else "repeticiones")+" · "+(exercise?.name?:goal.exerciseId),size=15)
   }
   val why=MonarchMissionCatalog.eligibleReason(store,mission,now)
   if(why!=null)HText(why,size=14,color=colors.muted)
   if(why==null)HCheck(
    "Participo por decisión propia. Puedo descansar y detenerme sin penalización.",
    accepted
   ){accepted=!accepted}
   HButton("INICIAR MISIÓN MONARCA",Modifier.fillMaxWidth(),primary=true,enabled=accepted&&why==null){
    ui.attempt{store.startMonarchMission(mission.id)}
   }
  }
 }
}