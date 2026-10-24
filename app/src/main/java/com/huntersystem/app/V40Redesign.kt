package com.huntersystem.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import org.json.JSONObject
import java.time.LocalDate

/** Nueva interfaz Compose; conserva el motor, datos, temas y recursos existentes. */
@Composable fun V40Header(ui:NativeUi){
 val c=LocalHunter.current
 val rank=rankKey(ui.store.stats().i("level",1))
 Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
  Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(c.accent.copy(alpha=.16f)).border(1.dp,c.accent.copy(alpha=.5f),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){HText("✦",size=27,color=c.accent,bold=true)}
  Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(1.dp)){HText("HUNTER SYSTEM",size=23,bold=true,maxLines=1);HText("SISTEMA DE SOMBRAS",size=10,color=c.muted,bold=true,maxLines=1)}
  Box(Modifier.clip(RoundedCornerShape(11.dp)).background(c.accent.copy(alpha=.13f)).clickable{ui.open("evolution")}.padding(horizontal=11.dp,vertical=9.dp)){HText("RANGO "+rank,size=12,color=c.accent,bold=true)}
  Avatar(ui.store.state.s("avatar"),ui.store.profile?.s("name")?:"C",Modifier.size(39.dp).clickable{ui.go("profile")})
 }
}

@Composable fun V40Tabs(ui:NativeUi){
 val c=LocalHunter.current
 Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.panel).border(1.dp,c.line,RoundedCornerShape(16.dp)).padding(4.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
  for((id,title)in listOf("home" to "Panel","plan" to "Misiones","objectives" to "Objetivos")){
   val active=ui.tab==id
   Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if(active)c.selected else Color.Transparent).then(if(active)Modifier.border(1.dp,c.accent.copy(alpha=.48f),RoundedCornerShape(12.dp)) else Modifier).clickable{ui.go(id)},contentAlignment=Alignment.Center){HText(title,size=15,color=if(active)c.text else c.muted,bold=active)}
  }
 }
}

@Composable fun V40Home(ui:NativeUi){
 val s=ui.store
 SceneColumn(ui){
  V40Tabs(ui)
  V40Hero(ui,s.stats())
  V40Metrics(s.stats())
  V40Mission(ui)
  V40Companion(ui)
  V40QuickLinks(ui)
  WeeklyPanel(ui)
  CyclePanel(ui)
  GoalsPanel(ui)
  AcademyTile(ui)
  HPanel(onClick={ui.open("reminders")},padding=17){
   Eyebrow("RECORDATORIOS")
   HText("Organiza tus entrenamientos  ↗",size=18,bold=true)
   HText("Configura tus notificaciones",size=14,color=LocalHunter.current.muted)
  }
 }
}

@Composable fun V40Hero(ui:NativeUi,stats:JSONObject){
 val s=ui.store;val p=s.profile?:JSONObject();val c=LocalHunter.current
 val rank=rankKey(stats.i("level",1)).lowercase()
 val appearance=s.state.o("appearance33")
 var collection=appearance.s("collection","hunter")
 if(collection=="alternate")collection=listOf("hunter","reader","regressor")[(LocalDate.now().toEpochDay()%3).toInt()]
 val char=if(collection=="hunter")"hunter-"+if(p.s("gender")=="female")"female" else "male" else collection
 val name=char+"-"+rank+".webp"
 val art=remember(name){if(runCatching{s.context.assets.open("ui34/"+name).close()}.isSuccess)"ui34/"+name else "ui33/"+name}
 val scene=when(appearance.s("scene","auto")){"sanctuary"->0;"journey"->1;else->((LocalDate.now().toEpochDay()-LocalDate.of(2026,1,1).toEpochDay())%2).toInt()}
 val level=stats.i("level",1);val xp=stats.i("xp")
 val shape=RoundedCornerShape(26.dp)
 Box(Modifier.fillMaxWidth().height(364.dp).clip(shape).background(Brush.linearGradient(listOf(c.surface,c.panel,c.bg))).border(1.dp,c.accent.copy(alpha=.45f),shape)){
  AssetImage("ui33/scene-"+rank+"-"+scene+".webp",Modifier.fillMaxSize(),ContentScale.Crop)
  Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(c.panel.copy(alpha=.97f),c.panel.copy(alpha=.88f),c.bg.copy(alpha=.18f)))))
  AssetImage(art,Modifier.align(Alignment.BottomEnd).fillMaxWidth(.61f).height(326.dp),ContentScale.Fit,"Cazador del sistema",Alignment.BottomEnd)
  Box(Modifier.fillMaxWidth().height(130.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent,c.panel.copy(alpha=.91f)))))
  Column(Modifier.fillMaxSize().padding(start=20.dp,end=18.dp,top=19.dp,bottom=19.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){Box(Modifier.size(7.dp).clip(CircleShape).background(c.accent));Eyebrow("SISTEMA ACTIVO")}
    HText("RANGO "+rank.uppercase(),size=12,color=c.accent,bold=true)
   }
   Spacer(Modifier.height(26.dp))
   Text("TU EVOLUCIÓN\nEMPIEZA HOY.",color=c.text,fontFamily=Barlow,fontWeight=FontWeight.SemiBold,fontSize=35.sp,lineHeight=39.sp,modifier=Modifier.fillMaxWidth(.79f))
   Spacer(Modifier.height(10.dp))
   HText("Una misión a la vez.\nUn nuevo límite por superar.",size=14,color=c.muted,modifier=Modifier.fillMaxWidth(.61f))
   Spacer(Modifier.weight(1f))
   Column(Modifier.fillMaxWidth(.64f),verticalArrangement=Arrangement.spacedBy(6.dp)){
    HText("NIVEL "+level.toString().padStart(2,'0'),size=20,bold=true)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
     HText("PROGRESO",size=11,color=c.muted,bold=true)
     HText((xp%300).toString()+" / 300 XP",size=11,color=c.accent,bold=true)
    }
    HProgress((xp%300)/300f)
    HText("VER EVOLUCIÓN  ↗",Modifier.clickable{ui.open("evolution")}.padding(top=8.dp),size=12,color=c.accent,bold=true)
   }
  }
 }
}

@Composable fun V40Metrics(stats:JSONObject){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){
  V40Metric("✧",stats.i("streak").toString(),"Días de racha",Modifier.weight(1f))
  V40Metric("◈",stats.i("exerciseCount").toString(),"Ejercicios",Modifier.weight(1f))
  V40Metric("↗",stats.i("reps").toString(),"Repeticiones",Modifier.weight(1f))
 }
}
@Composable private fun V40Metric(symbol:String,value:String,label:String,modifier:Modifier){
 val c=LocalHunter.current
 Column(modifier.height(110.dp).clip(RoundedCornerShape(19.dp)).background(Brush.verticalGradient(listOf(c.surface,c.panel))).border(1.dp,c.line,RoundedCornerShape(19.dp)).padding(horizontal=12.dp,vertical=9.dp),verticalArrangement=Arrangement.SpaceBetween){
  HText(symbol,size=16,color=c.accent,bold=true)
  HText(value,size=30,bold=true,maxLines=1)
  HText(label,size=12,color=c.muted,maxLines=1)
 }
}

@Composable fun V40Mission(ui:NativeUi){
 val s=ui.store;val day=s.day(today());val log=s.logs.optJSONObject(today())
 val total=day.a("items").length()+day.a("tasks").length()
 val progress=s.fraction(log).coerceIn(0.0,1.0).toFloat()
 HPanel(padding=19){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("✦  MISIÓN DEL DÍA");HText(((progress*100).toInt()).toString()+"%",size=14,color=LocalHunter.current.accent,bold=true)}
  HText(day.s("title","Misión diaria"),size=27,bold=true,maxLines=2)
  HText(total.toString()+" objetivos programados para hoy",size=14,color=LocalHunter.current.muted)
  HProgress(progress)
  HButton("ABRIR MISIÓN  ↗",Modifier.fillMaxWidth(),primary=true){ui.open("mission",obj("date" to today()))}
  Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){
   HText("Personalizar misión",Modifier.clickable{ui.open("custom",obj("date" to today()))}.padding(5.dp),size=14,color=LocalHunter.current.accent)
  }
 }
}

@Composable fun V40Companion(ui:NativeUi){
 val c=LocalHunter.current;val name=companionName(ui.store.profile)
 HPanel(onClick={ui.open("coach")},padding=12){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
   Box(Modifier.size(width=80.dp,height=82.dp).clip(RoundedCornerShape(13.dp)).background(c.selected)){
    AssetImage("ui33/companion-"+companionKey(ui.store.profile)+".webp",Modifier.fillMaxSize(),ContentScale.Fit,"Compañero "+name)
   }
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
    Eyebrow("TU ACOMPAÑANTE")
    HText(name,size=25,bold=true,maxLines=1)
    HText("Asistente de tus misiones",size=13,color=c.muted,maxLines=1)
   }
   HText("↗",size=22,color=c.accent)
  }
 }
}

@Composable fun V40QuickLinks(ui:NativeUi){
 val links=listOf(Triple("◈","Mis rutinas","routines"),Triple("◎","Objetivos","objectives"),Triple("▤","Ejercicios","book"),Triple("↗","Mi progreso","stats"))
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
  HText("ACCESOS RÁPIDOS",size=13,bold=true,color=LocalHunter.current.muted)
  for(row in links.chunked(2)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
    for((symbol,label,tab)in row){
     val c=LocalHunter.current
     Row(Modifier.weight(1f).height(66.dp).clip(RoundedCornerShape(17.dp)).background(c.panel).border(1.dp,c.line,RoundedCornerShape(17.dp)).clickable{ui.go(tab)}.padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
      HText(symbol,size=19,color=c.accent,bold=true)
      HText(label,Modifier.weight(1f),size=14,bold=true,maxLines=2)
      HText("›",size=17,color=c.muted)
     }
    }
   }
  }
 }
}

@Composable fun V40BottomNav(ui:NativeUi){
 val c=LocalHunter.current
 val tabs=listOf(Triple("home","Panel","⌂"),Triple("plan","Misiones","✦"),Triple("routines","Rutinas","◇"),Triple("book","Ejercicios","▤"),Triple("profile","Perfil","◯"))
 Row(Modifier.fillMaxWidth().background(c.panel).border(1.dp,c.line).navigationBarsPadding().height(73.dp).padding(horizontal=5.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)){
  for((id,label,symbol)in tabs){
   val selected=ui.tab==id
   Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(15.dp)).background(if(selected)c.selected else Color.Transparent).clickable{ui.go(id)}.padding(vertical=3.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
    HText(symbol,size=22,color=if(selected)c.accent else c.muted,bold=selected)
    HText(label,size=11,color=if(selected)c.text else c.muted,bold=selected,maxLines=1)
   }
  }
 }
}
