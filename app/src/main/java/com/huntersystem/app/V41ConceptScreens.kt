package com.huntersystem.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.*
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * V41: Interfaces visuales inspiradas en los bocetos aprobables.
 * Toda interacción llama los controladores nativos existentes.
 * Ningún ejercicio, XP o sesión está codificado como dato simulado.
 */
@Composable fun V41Banner(title:String,subtitle:String,symbol:String="✦",art:String?=null,modifier:Modifier=Modifier){
 val c=LocalHunter.current
 val shape=RoundedCornerShape(23.dp)
 Box(modifier.fillMaxWidth().height(if(art==null)116.dp else 154.dp)
  .clip(shape).background(Brush.horizontalGradient(listOf(c.surface,c.panel,c.bg)))
  .border(1.dp,c.accent.copy(alpha=.42f),shape)){
  if(art!=null)AssetImage(art,Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.43f),ContentScale.Fit)
  Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(c.panel.copy(alpha=.95f),c.panel.copy(alpha=.62f),Color.Transparent))))
  Column(Modifier.align(Alignment.CenterStart).padding(18.dp).fillMaxWidth(if(art==null)1f else .70f),verticalArrangement=Arrangement.spacedBy(6.dp)){
   Eyebrow(symbol+"  HUNTER SYSTEM")
   HText(title,size=26,bold=true,maxLines=2)
   HText(subtitle,size=14,color=c.muted,maxLines=2)
  }
 }
}

@Composable fun V41SectionTabs(options:List<Pair<String,String>>,selected:String,onSelect:(String)->Unit){
 val c=LocalHunter.current
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
  for((key,label)in options){
   val active=key==selected
   Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp))
     .background(if(active)Brush.horizontalGradient(listOf(c.accent,c.secondary)) else Brush.verticalGradient(listOf(c.surface,c.panel)))
     .border(1.dp,if(active)c.accent else c.line,RoundedCornerShape(12.dp))
     .clickable{onSelect(key)},contentAlignment=Alignment.Center){
     HText(label,size=14,bold=active,color=if(active)c.bg else c.muted,maxLines=1)
   }
  }
 }
}

@Composable fun V41MissionScreen(ui:NativeUi){
 val s=ui.store
 var section by remember{mutableStateOf("daily")}
 val todayData=s.day(today())
 SceneColumn(ui){
  V41Banner("MISIONES","Cada desafío te acerca a tu siguiente nivel.","✦","ui33/companion-"+companionKey(s.profile)+".webp")
  V41SectionTabs(listOf("daily" to "Diarias","week" to "Semanales","special" to "Especiales"),section){section=it}
  when(section){
   "daily"->{
    HText("MISIÓN PRINCIPAL",size=14,color=LocalHunter.current.muted,bold=true)
    HPanel(padding=17){
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
      Column(Modifier.weight(1f)){
       Eyebrow("HOY · "+today())
       HText(todayData.s("title","Entrenamiento del día"),size=22,bold=true,maxLines=2)
      }
      HText("✦",size=32,color=LocalHunter.current.accent)
     }
     val total=todayData.a("items").length()+todayData.a("tasks").length()
     val fraction=s.fraction(s.logs.optJSONObject(today())).coerceIn(0.0,1.0).toFloat()
     HText((fraction*total).roundToInt().toString()+" / "+total+" objetivos",size=14,color=LocalHunter.current.muted)
     HProgress(fraction)
     HButton("ENTRAR A MISIÓN  ▶",Modifier.fillMaxWidth(),primary=true){ui.open("mission",obj("date" to today()))}
    }
    MissionContent(ui,today())
    HPanel(padding=16){
     Eyebrow("SESIÓN GUIADA")
     HText("Entrenamiento paso a paso",size=21,bold=true)
     HText("Demostraciones sin conexión, series y temporizador.",size=14,color=LocalHunter.current.muted)
     HButton("Iniciar sesión guiada  ▶",Modifier.fillMaxWidth(),primary=true){ui.open("session",obj("date" to today()))}
    }
    TrainingMode(ui)
   }
   "week"->{CyclePanel(ui);WeeklyPanel(ui);CalendarPanel(ui)}
   else->{GoalsPanel(ui);HPanel{Eyebrow("PERSONALIZACIÓN");HText("Adapta las misiones a tus objetivos.",bold=true);HButton("Ir a objetivos",Modifier.fillMaxWidth()){ui.go("objectives")}}}
  }
 }
}

@Composable fun V41MissionChecklist(ui:NativeUi,date:String){
 val s=ui.store;val day=s.day(date);val l=s.logs.optJSONObject(date)
 val items=l?.a("items")?.objects()?:day.a("items").objects()
 val tasks=l?.a("missionTasks")?.objects()?:day.a("tasks").objects()
 val total=items.size+tasks.size
 val closed=l?.b("complete")==true||l?.b("pain")==true||date!=today()
 val fraction=s.fraction(l).coerceIn(0.0,1.0).toFloat()
 val c=LocalHunter.current
 HPanel(padding=17){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
   Eyebrow("✦  MISIÓN DIARIA")
   HText(((fraction*100).toInt()).toString()+"%",size=14,color=c.accent,bold=true)
  }
  HText(day.s("title"),size=24,bold=true,maxLines=2)
  HText((fraction*total).roundToInt().toString()+" / "+total+" tareas completadas",size=14,color=c.muted)
  HProgress(fraction)
  day.s("blocked381").takeIf{it.isNotBlank()}?.let{HNotice(it)}
  for((index,x)in items.withIndex()){
   val exercise=s.catalog.byId[x.s("id")]?:continue
   val done=x.b("missionState")||x.b("selfReported")||x.a("actual").objects().count{it.b("done")}>=x.i("sets")
   Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(if(done)c.accent.copy(alpha=.13f) else c.input).padding(horizontal=10.dp,vertical=8.dp),
    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
    Box(Modifier.size(33.dp).clip(RoundedCornerShape(10.dp)).background(if(done)c.accent else c.surface).clickable(enabled=!closed){ui.attempt{s.toggleMission(date,index)}},contentAlignment=Alignment.Center){
     HText(if(done)"✓" else "○",size=19,bold=true,color=if(done)c.bg else c.muted)
    }
    Column(Modifier.weight(1f).clickable{ui.open("exercise",obj("id" to exercise.id))}){
     HText(exercise.name,size=16,bold=true,maxLines=2)
     HText(x.i("sets").toString()+" series × "+x.i("target")+" "+if(exercise.unit=="sec")"segundos" else "repeticiones",size=12,color=c.muted,maxLines=1)
    }
    HText("›",Modifier.clickable{ui.open("exercise",obj("id" to exercise.id))}.padding(5.dp),size=25,color=c.accent)
   }
  }
  for((index,x)in tasks.withIndex()){
   val done=x.b("done")
   Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(if(done)c.accent.copy(alpha=.13f) else c.input).clickable(enabled=!closed){ui.attempt{s.toggleTask(date,index)}}.padding(12.dp),
    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
    HText(if(done)"☑" else "□",size=23,color=c.accent)
    HText(x.s("text"),Modifier.weight(1f),size=15,maxLines=3)
   }
  }
  HText(if(l?.b("complete")==true)"MISIÓN COMPLETADA" else if(l?.b("pain")==true)"SESIÓN DETENIDA" else "MISIÓN EN CURSO",size=13,color=c.accent,bold=true)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   HButton("Editar",Modifier.weight(1f)){ui.open("custom",obj("date" to date))}
   HButton("Registrar series",Modifier.weight(1f),enabled=date==today()){ui.open("record",obj("date" to date))}
  }
  HText("Marca solo tareas realizadas. Para contar series y repeticiones utiliza el registro detallado.",size=12,color=c.muted)
 }
}

@Composable fun V41RoutinesScreen(ui:NativeUi){
 val s=ui.store
 if(ui.library.isNotBlank()){RoutineLibrary(ui,ui.library);return}
 val p=s.profile?:JSONObject()
 val companion="ui33/companion-"+companionKey(p)+".webp"
 SceneColumn(ui){
  V41Banner("RUTINAS","Tu próxima transformación comienza aquí.","◈",companion)
  V41SectionTabs(listOf("0" to "Básico","1" to "Intermedio","2" to "Avanzado"),ui.routineLevel.toString()){selected->ui.routineLevel=selected.toInt()}
  HPanel(onClick={ui.library="offline"},padding=0){
   Box(Modifier.fillMaxWidth().height(172.dp).clip(RoundedCornerShape(21.dp))){
    AssetImage("ui33/hunter-male-e.webp",Modifier.align(Alignment.CenterEnd).fillMaxWidth(.49f).fillMaxHeight(),ContentScale.Fit)
    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(LocalHunter.current.panel,LocalHunter.current.panel.copy(alpha=.88f),Color.Transparent))))
    Column(Modifier.align(Alignment.CenterStart).padding(18.dp).fillMaxWidth(.68f),verticalArrangement=Arrangement.spacedBy(8.dp)){
     Eyebrow("PROGRESIÓN DE TRES SEMANAS")
     HText("Conquista tu siguiente nivel",size=25,bold=true)
     HText("Fuerza · cardio · movilidad",size=13,color=LocalHunter.current.muted)
     HText("ABRIR BIBLIOTECA  ↗",size=14,color=LocalHunter.current.accent,bold=true)
    }
   }
  }
  HPanel(onClick={ui.library="offline"},padding=17){
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
    Box(Modifier.size(59.dp).clip(RoundedCornerShape(15.dp)).background(LocalHunter.current.selected),contentAlignment=Alignment.Center){HText("✦",size=30,color=LocalHunter.current.accent)}
    Column(Modifier.weight(1f)){HText("Generador dinámico",size=22,bold=true);HText("Crea una rutina según zona, duración, equipo y nivel",size=14,color=LocalHunter.current.muted)}
    HText("›",size=27,color=LocalHunter.current.accent)
   }
  }
  Eyebrow("EXPLORA LOS ENTRENAMIENTOS")
  for((label,desc,symbol)in listOf(Triple("Tren superior","Fuerza y control de la parte superior","▲"),Triple("Tren inferior","Piernas, glúteos y potencia","▼"),Triple("Cardio y resistencia","Energía, agilidad y capacidad aeróbica","♥"),Triple("Movilidad","Movimiento y recuperación","✧"))){
   HPanel(onClick={ui.library="offline"},padding=13){
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)){
     Box(Modifier.size(47.dp).clip(RoundedCornerShape(14.dp)).background(LocalHunter.current.selected),contentAlignment=Alignment.Center){HText(symbol,size=25,color=LocalHunter.current.accent)}
     Column(Modifier.weight(1f)){HText(label,size=18,bold=true);HText(desc,size=13,color=LocalHunter.current.muted)}
     HText("›",size=24,color=LocalHunter.current.accent)
    }
   }
  }
  HButton("Rutinas para gimnasio  ↗",Modifier.fillMaxWidth()){ui.library="gym"}
  TrainingMode(ui)
  Eyebrow("ENTRENADORES DE REFERENCIA")
  for(ch in s.catalog.editorial.a("channels").objects().take(5))
   HPanel(onClick={ui.open("creator",obj("key" to ch.s("key")))},padding=14){
    HText(ch.s("name")+"  ↗",size=18,bold=true)
   }
  HButton("+ Crear rutina propia",Modifier.fillMaxWidth()){ui.open("routine-editor")}
 }
}

@Composable fun V41RoutineCard(ui:NativeUi,r:JSONObject){
 val s=ui.store
 val cover=s.catalog.byId[r.a("items").optJSONObject(0)?.s("id")?:""]
 HPanel(padding=14){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
   Eyebrow(r.s("category").uppercase())
   HText("≈ "+r.i("minutes",s.planner.minutes(r,s.adjusted())).toString()+" MIN",size=12,color=LocalHunter.current.muted,bold=true)
  }
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
   if(cover!=null)V39Artwork(cover,Modifier.size(100.dp))
   Column(Modifier.weight(1f)){
    HText(r.s("name"),size=21,bold=true,maxLines=2)
    HText(r.a("items").length().toString()+" ejercicios · "+r.a("items").objects().sumOf{it.i("sets")}+" series",size=14,color=LocalHunter.current.muted)
    HText("VER SECUENCIA  ↗",Modifier.clickable{ui.open("routine",obj("id" to r.s("id")))}.padding(top=10.dp),size=13,color=LocalHunter.current.accent,bold=true)
   }
  }
  HButton("Abrir rutina",Modifier.fillMaxWidth(),primary=true){ui.open("routine",obj("id" to r.s("id")))}
 }
}

@Composable fun V41ExercisesScreen(ui:NativeUi){
 val s=ui.store
 var query by remember{mutableStateOf("")}
 var category by remember{mutableStateOf("")}
 var group by remember{mutableStateOf("")}
 var difficulty by remember{mutableStateOf("")}
 var gear by remember{mutableStateOf("")}
 var variants by remember{mutableStateOf(false)}
 var restricted by remember{mutableStateOf(false)}
 val results=remember(query,category,group,difficulty,gear,variants,restricted,s.revision.intValue){
  s.catalog.exercises.filter{e->
   (query.isBlank()||normalize(e.name).contains(normalize(query)))&&
   (category.isBlank()||e.category==category)&&
   (group.isBlank()||e.group==group)&&
   (difficulty.isBlank()||e.raw.i("level").toString()==difficulty)&&
   (variants||e.raw.s("variant")=="base")&&
   (!restricted||s.profile!=null&&s.rules.compatible(e,s.adjusted()))&&
   (gear.isBlank()||when(gear){"body"->e.equipment=="body"&&!e.gym;"db"->e.equipment=="db";"gym"->e.gym;else->gear in e.raw.a("materialRequirements").strings()})
  }
 }
 val c=LocalHunter.current
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(top=20.dp,bottom=30.dp)){
  item{Header(ui)}
  item{V41Banner("EJERCICIOS","Biblioteca completa con demostraciones sin conexión.","▤")}
  item{HInput(query,{query=it},"Buscar ejercicio")}
  item{Chips(listOf("" to "Todos","Fuerza" to "Fuerza","Cardio" to "Cardio","Movilidad" to "Movilidad"),category){category=it}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
   HSelect("Zona",group,listOf("" to "Todas")+s.catalog.exercises.map{it.group}.distinct().map{it to it},Modifier.weight(1f)){group=it}
   HSelect("Nivel",difficulty,listOf("" to "Todos","0" to "Básico","1" to "Intermedio","2" to "Avanzado"),Modifier.weight(1f)){difficulty=it}
  }}
  item{HSelect("Equipo",gear,listOf("" to "Todos","body" to "Sin equipo","db" to "Mancuernas","gym" to "Gimnasio","bench" to "Banco","step" to "Plataforma")){gear=it}}
  item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
   HText(results.size.toString()+" EJERCICIOS",size=13,color=c.accent,bold=true)
   HText("TOCA UNA FICHA  ↗",size=12,color=c.muted)
  }}
  item{HCheck("Mostrar variantes técnicas",variants){variants=!variants};HCheck("Aplicar restricciones de Perfil",restricted){restricted=!restricted}}
  items(results,key={it.id}){e->
   HPanel(onClick={ui.open("exercise",obj("id" to e.id))},padding=11){
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
     V39Artwork(e,Modifier.width(110.dp).height(102.dp))
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
      HText(e.name,size=18,bold=true,maxLines=2)
      HText(e.group+" · "+listOf("Básico","Intermedio","Avanzado")[e.raw.i("level").coerceIn(0,2)],size=13,color=c.muted,maxLines=2)
      HText(if(e.impact)"Con saltos  ·  ▶" else "▶ Ver demostración",size=12,color=c.accent,bold=true)
     }
     HText("›",size=25,color=c.accent)
    }
   }
  }
  if(results.isEmpty())item{HNotice("Sin coincidencias para estos filtros. Cambia el nivel, la zona o el equipo.")}
 }
}

@Composable fun V41ObjectivesScreen(ui:NativeUi){
 val s=ui.store
 SceneColumn(ui){
  V41Banner("OBJETIVOS","Escoge el propósito de tu entrenamiento.","◎")
  HButton("+ Añadir objetivo",Modifier.fillMaxWidth(),primary=true){ui.open("goal-edit")}
  val goals=s.state.a("goals").objects()
  if(goals.isEmpty())HNotice("Aún no tienes objetivos registrados.")
  for(g in goals){
   val completed=today() in g.a("dates").strings()
   HPanel(padding=15){
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
     Box(Modifier.size(43.dp).clip(RoundedCornerShape(14.dp)).background(LocalHunter.current.selected),contentAlignment=Alignment.Center){HText(if(completed)"✓" else "✦",size=25,color=LocalHunter.current.accent)}
     Column(Modifier.weight(1f)){
      HText(g.s("title",g.s("text")),size=18,bold=true,maxLines=2)
      HText(if(completed)"Completado hoy" else if(g.s("repeat")=="daily")"Objetivo diario" else "Objetivo personal",size=13,color=LocalHunter.current.muted)
     }
     HText(if(completed)"☑" else "□",Modifier.clickable{s.toggleGoal(g.s("id"))},size=27,color=LocalHunter.current.accent)
    }
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
     HButton("Editar",Modifier.weight(1f)){ui.open("goal-edit",g)}
     HButton("Eliminar",Modifier.weight(1f)){ui.open("delete-goal",g)}
    }
   }
  }
 }
}

@Composable fun V41ProfileBanner(ui:NativeUi,name:String,avatar:String){
 val s=ui.store;val st=s.stats();val c=LocalHunter.current
 val rank=rankKey(st.i("level",1))
 HPanel(padding=15){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)){
   Avatar(avatar,name,Modifier.size(68.dp).border(1.dp,c.accent.copy(alpha=.5f),CircleShape))
   Column(Modifier.weight(1f)){
    Eyebrow("PERFIL DEL CAZADOR")
    HText(name.ifBlank{"Cazador"},size=23,bold=true,maxLines=1)
    HText("RANGO "+rank+"  ·  NIVEL "+st.i("level",1),size=14,color=c.muted)
   }
  }
  HProgress((st.i("xp")%300)/300f)
  HText((st.i("xp")%300).toString()+" / 300 XP · Próximo nivel",size=13,color=c.muted)
 }
}

@Composable fun V41ThemePicker(value:String,onSelect:(String)->Unit){
 val themes=listOf(
  Triple("eclipse","Azul",Color(0xFF3B82F6)),
  Triple("aurora","Rosa",Color(0xFFE957AF)),
  Triple("nebula","Morado",Color(0xFF8B5CF6)),
  Triple("hielo","Hielo",Color(0xFF5BCDF9)),
  Triple("ceniza","Oscuro",Color(0xFFAEBCCD)),
  Triple("amanecer","Dorado",Color(0xFFFFBF5D)),
  Triple("esmeralda","Esmeralda",Color(0xFF17D49B)),
  Triple("rojo","Rojo",Color(0xFFF45C70)),
  Triple("menta","Menta",Color(0xFF43D5C9)),
  Triple("lavanda","Lavanda",Color(0xFFB9A1FF))
 )
 Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
  Eyebrow("APARIENCIA · ELIGE TU TEMA")
  for(row in themes.chunked(2)){
   Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){
    for((key,label,color)in row){
     val selected=value==key
     Row(Modifier.weight(1f).height(51.dp).clip(RoundedCornerShape(14.dp))
       .background(Brush.horizontalGradient(listOf(color.copy(alpha=.28f),LocalHunter.current.panel)))
       .border(if(selected)2.dp else 1.dp,if(selected)color else LocalHunter.current.line,RoundedCornerShape(14.dp))
       .clickable{onSelect(key)}.padding(horizontal=10.dp),
       verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
       Box(Modifier.size(24.dp).clip(RoundedCornerShape(9.dp)).background(color))
       HText(label,Modifier.weight(1f),size=15,bold=selected)
       if(selected)HText("✓",size=15,color=color,bold=true)
     }
    }
   }
  }
  HText("El color se aplicará al guardar el perfil. Mantiene el contenido sin conexión.",size=13,color=LocalHunter.current.muted)
 }
}

@Composable fun V41ProgressBanner(ui:NativeUi){
 val s=ui.store;val stats=s.stats()
 V41Banner("TU PROGRESO","Nivel "+stats.i("level",1)+" · "+stats.i("xp")+" XP acumulados","↗","ui33/companion-"+companionKey(s.profile)+".webp")
}
