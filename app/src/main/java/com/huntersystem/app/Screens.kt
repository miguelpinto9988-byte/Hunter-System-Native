package com.huntersystem.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONArray
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

data class Overlay(val type:String,val data:JSONObject=JSONObject())
class NativeUi(val store:NativeStore){
 var tab by mutableStateOf(if(store.profile==null)"profile" else "home")
 var routineLevel by mutableIntStateOf(level(store.profile?:JSONObject()))
 var top by mutableStateOf("home");var library by mutableStateOf("")
 val stack=mutableStateListOf<Overlay>();val generated=mutableStateMapOf<String,List<JSONObject>>();var serial=0
 var pendingAvatar by mutableStateOf<String?>(null)
 var message:(String)->Unit={};var export:()->Unit={};var import:()->Unit={};var chooseAvatar:()->Unit={};var openUrl:(String)->Unit={};var requestNotifications:()->Unit={}
 fun go(t:String){tab=t;library="";stack.clear()}
 fun open(type:String,data:JSONObject=JSONObject()){stack+=Overlay(type,data)}
 fun back(){if(stack.isNotEmpty())stack.removeAt(stack.lastIndex) else if(library.isNotBlank())library="" else if(tab!="home")tab="home"}
 fun attempt(block:()->Unit){try{block()}catch(e:Exception){message(e.message?:"No se pudo completar la acción")}}
 fun routine(id:String)=generated.values.flatten().firstOrNull{it.s("id")==id}?:store.state.a("customRoutines").objects().firstOrNull{it.s("id")==id}?:store.catalog.routines.firstOrNull{it.s("id")==id}
}
fun rankKey(level:Int)=listOf("E","D","C","B","A","S")[((level-1)/5).coerceIn(0,5)]
@Composable fun Header(ui:NativeUi){ V40Header(ui) }
@Composable fun SceneColumn(ui:NativeUi,content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().verticalScroll(remember(ui.tab,ui.library){ScrollState(0)}).padding(horizontal=18.dp,vertical=20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){Header(ui);content();Spacer(Modifier.height(25.dp))}}
@Composable fun TopTabs(ui:NativeUi){ V40Tabs(ui) }
@Composable fun HomeScreen(ui:NativeUi){ V40Home(ui) }
@Composable fun Hero(ui:NativeUi,stats:JSONObject){ V40Hero(ui,stats) }
@Composable fun MissionCompanion(ui:NativeUi){ V40Companion(ui) }
@Composable fun MissionLaunch(ui:NativeUi){ V40Mission(ui) }
@Composable fun AcademyTile(ui:NativeUi){HPanel(onClick={ui.open("academy")}){Eyebrow("EL CONOCIMIENTO TAMBIÉN SUBE DE NIVEL");Heading("Explorar la academia ↗",size=28);HText("Técnica, descanso y preparación en casa",size=16,color=LocalHunter.current.muted)}}
@Composable fun GoalsPanel(ui:NativeUi){val s=ui.store;HPanel{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Heading("Tus objetivos",size=29);HText("Ver todos ↗",Modifier.clickable{ui.go("objectives")},size=16,color=LocalHunter.current.accent)};val goals=s.state.a("goals").objects();if(goals.isEmpty())HText("Crea una meta y márcala cuando la hayas terminado.",size=16,color=LocalHunter.current.muted);for(g in goals.take(5))HCheck(g.s("title",g.s("text")),today() in g.a("dates").strings()){ui.attempt{s.toggleGoal(g.s("id"))}};HButton("+ Añadir objetivo"){ui.open("goal-edit")}}}
@Composable fun ObjectivesScreen(ui:NativeUi){ V41ObjectivesScreen(ui) }
@Composable fun CyclePanel(ui:NativeUi){val s=ui.store;val monday=LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value-1).toLong()).toString();HPanel{Eyebrow("CICLO ${s.state.a("cycles").length().coerceAtLeast(1)}");Heading("Tu arco de 7 días",size=29);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){for(i in 0L..6L){val date=plus(monday,i);val d=s.day(date);val checked=s.logs.o(date).b("complete");val short=mapOf("strength" to "FUE","cardio" to "CAR","recovery" to "REC","rest" to "DES")[d.s("kind")]?:"MIX";Column(Modifier.weight(1f).clip(RoundedCornerShape(5.dp)).background(if(date==today())LocalHunter.current.selected else LocalHunter.current.input).clickable{ui.open("mission",obj("date" to date))}.padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally){HText(if(checked)"✓" else (i+1).toString(),size=18);HText(short,size=11)}}};HText("FUE Fuerza   CAR Cardio   REC Recuperación",size=13,color=LocalHunter.current.muted);HText("Generación dinámica de sesiones: calendario, objetivo, historial y restricciones. Se conservan sesiones registradas.",size=15,color=LocalHunter.current.muted);HButton("Ver todas las misiones →"){ui.go("plan")}}}
@Composable fun WeeklyPanel(ui:NativeUi){val s=ui.store;val monday=LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value-1).toLong()).toString();val training=(0L..6L).map{plus(monday,it)}.filter{it<=today()&&s.day(it).s("kind")!="rest"};val done=training.count{s.logs.o(it).b("complete")};val fraction=if(training.isEmpty())0f else done.toFloat()/training.size;val stats=s.stats();HPanel{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("PROGRESO DE LA SEMANA");HText("${(fraction*100).toInt()}%",bold=true)};HProgress(fraction);Heading("$done de ${training.size} días de ejercicio",size=26);HText("Completa tus misiones y registra el esfuerzo. La siguiente revisión será el lunes.",size=16,color=LocalHunter.current.muted);HText("${stats.i("xp")} XP acumulada · Nivel ${stats.i("level",1)}",size=16);HButton("Ver calendario mensual"){ui.go("plan")};HButton("Ver progreso →"){ui.go("stats")};HButton("Revisión semanal"){ui.open("review")}}}
@Composable fun TrainingMode(ui:NativeUi){val p=ui.store.profile?:return;Chips(listOf("challenges" to "Desafíos diarios","routines" to "Rutinas completas"),p.s("trainingMode","challenges")){mode->ui.attempt{ui.store.saveProfile(p.copyJson().put("trainingMode",mode))}}}
@Composable fun PlanScreen(ui:NativeUi){ V41MissionScreen(ui) }
@Composable fun MissionContent(ui:NativeUi,date:String){ V41MissionChecklist(ui,date) }
@Composable fun CalendarPanel(ui:NativeUi){var month by remember{mutableStateOf(YearMonth.now())};val locale=Locale("es");val start=(month.atDay(1).dayOfWeek.value-1);HPanel{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){HButton("‹"){month=month.minusMonths(1)};Heading(month.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy",locale)),size=25);HButton("›"){month=month.plusMonths(1)}};Row{for(t in listOf("L","M","X","J","V","S","D"))Box(Modifier.weight(1f),contentAlignment=Alignment.Center){HText(t,size=14,color=LocalHunter.current.muted)}};for(row in 0..5){if(row*7>=start+month.lengthOfMonth())break;Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){for(col in 0..6){val number=row*7+col-start+1;if(number !in 1..month.lengthOfMonth()){Spacer(Modifier.weight(1f));continue};val date=month.atDay(number).toString();val checked=ui.store.logs.o(date).b("complete");val train=ui.store.profile?.a("weekPlan")?.optString(weekday(date),"rest")!="rest";Column(Modifier.weight(1f).clip(RoundedCornerShape(5.dp)).background(if(date==today())LocalHunter.current.selected else LocalHunter.current.input).clickable{ui.open("mission",obj("date" to date))}.padding(vertical=6.dp),horizontalAlignment=Alignment.CenterHorizontally){HText(number.toString(),size=17);HText(if(checked)"✓" else if(train)"•" else "—",size=13,color=LocalHunter.current.accent)}}}};HText("• entrenamiento · — descanso · ✓ completado. Las semanas futuras son previsiones y se revisan con tus registros.",size=14,color=LocalHunter.current.muted)}}
@Composable fun RoutinesScreen(ui:NativeUi){ V41RoutinesScreen(ui) }
@Composable fun RoutineLibrary(ui:NativeUi,mode:String){val s=ui.store;var n by remember(mode){mutableIntStateOf(ui.routineLevel)};var category by remember(mode){mutableStateOf("")};var query by remember(mode){mutableStateOf("")};val generated=ui.generated[mode];val base=generated?:(s.catalog.routines+s.state.a("customRoutines").objects()).filter{it.b("gymOnly23")== (mode=="gym")};val list=base.filter{it.i("level")==n&&(category.isBlank()||it.s("category")==category)&&(query.isBlank()||normalize(it.s("name")).contains(normalize(query)))};LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(top=20.dp,bottom=30.dp)){item{Header(ui)};item{HButton("‹ Volver al centro"){ui.library=""}};item{RoutineGenerator(ui,mode)};item{Eyebrow(if(mode=="gym")"GIMNASIO" else "BIBLIOTECA SIN CONEXIÓN");Heading(if(mode=="gym")"Rutinas especializadas en gimnasio" else "Rutinas de Hunter System");HText(if(generated!=null)"Rutinas nuevas generadas localmente; las combinaciones no se guardan." else "Fuerza, cardio, movilidad y control. Selecciona tu nivel.",color=LocalHunter.current.muted)};item{Chips(listOf("0" to "Básico","1" to "Intermedio","2" to "Avanzado"),n.toString()){n=it.toInt();ui.routineLevel=n}};item{HInput(query,{query=it},"Buscar rutina");Spacer(Modifier.height(10.dp));Chips(listOf("" to "Todas","Fuerza" to "Fuerza","Cardio" to "Cardio","Movilidad" to "Movilidad","Mixta" to "Mixtas"),category){category=it}};item{HText("${list.size} resultados",size=16,color=LocalHunter.current.muted)};items(list,key={it.s("id")}){r->RoutineTile(ui,r)};if(list.isEmpty())item{HNotice("No hay ejercicios compatibles con estos filtros. Prueba otra categoría o duración, o revisa las restricciones y el equipo registrado en Perfil.")}}}
@Composable fun RoutineGenerator(ui:NativeUi,mode:String){val s=ui.store;val scope=rememberCoroutineScope();val chosen=remember(mode){mutableStateListOf<String>()};var duration by remember(mode){mutableStateOf("")};var busy by remember{mutableStateOf(false)};var info by remember(mode){mutableStateOf("")};val colors=listOf("#237CFF","#F44295","#8B51F5","#00B79A","#F1B837");val symbols=listOf("▲","▼","◆","♥","✧")
 HPanel(decor=false,padding=18,modifier=Modifier.border(1.dp,LocalHunter.current.secondary,RoundedCornerShape(16.dp))){Eyebrow("HUNTER SYSTEM · GENERADOR DINÁMICO");Heading("Diseña tu próxima rutina",size=31);HText("1200 demos 2D",size=14,color=LocalHunter.current.accent);HText("Elige los tipos de ejercicio y el tiempo. Los entrenamientos se generan por separado para cada categoría y para los tres niveles existentes.",size=16,color=LocalHunter.current.muted);Eyebrow("01 · GRUPOS DE ENTRENAMIENTO");HText("Selección múltiple",size=13,color=LocalHunter.current.muted)
 for(pair in groupLabels.entries.toList().chunked(2))Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){for(entry in pair){val i=groupLabels.keys.indexOf(entry.key);val color=parseColor(colors[i]);Row(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if(entry.key in chosen)color.copy(alpha=.20f) else LocalHunter.current.input).border(1.dp,if(entry.key in chosen)color else LocalHunter.current.line,RoundedCornerShape(10.dp)).clickable{if(!chosen.remove(entry.key))chosen+=entry.key}.padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){HText(symbols[i],color=color,size=23);HText(if(entry.key=="mobility")"Movilidad" else entry.value,size=16)}};if(pair.size==1)Spacer(Modifier.weight(1f))}
 Eyebrow("02 · DURACIÓN DE LA SESIÓN");Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){for((id,label)in listOf("short" to "5–10 min","medium" to "10–20 min","long" to "Hasta 30 min"))HChip(label,duration==id,Modifier.weight(1f)){duration=id}}
 HButton(if(busy)"Generando…" else "✧ Generar nuevas rutinas",Modifier.fillMaxWidth(),primary=true,enabled=!busy){if(chosen.isEmpty())info="Elige al menos un grupo de entrenamiento." else if(duration.isBlank())info="Selecciona uno de los tres intervalos de duración." else if(mode=="gym"&&s.profile?.s("equipment")!="gym")info="Para generar rutinas de gimnasio, configura primero el equipo de gimnasio en Perfil." else{busy=true;val p=s.adjusted();val logs=s.logs.copyJson();val types=chosen.toList();val range=duration;val epoch=++ui.serial;scope.launch{try{val rs=withContext(Dispatchers.Default){(0..2).flatMap{n->types.flatMap{type->(0..1).mapNotNull{v->s.planner.generateRoutine(mode,type,n,v,range,epoch,p,logs)}}}};if(rs.isEmpty())info="Sin rutinas compatibles con esta selección. Revisa el equipo, las restricciones o el tiempo." else{ui.generated[mode]=rs;info="${rs.size} rutinas listas. Selecciona el nivel debajo para verlas."}}catch(e:Exception){info=e.message?:"No se pudieron generar las rutinas"}finally{busy=false}}}}
 if(info.isNotBlank())HText(info,size=15,color=LocalHunter.current.accent);if(ui.generated[mode]!=null)HButton("Ver biblioteca original"){ui.generated.remove(mode);info=""}
}}
@Composable fun RoutineTile(ui:NativeUi,r:JSONObject){ V41RoutineCard(ui,r) }
@Composable fun ExercisesScreen(ui:NativeUi){ V41ExercisesScreen(ui) }
@Composable fun ProgressScreen(ui:NativeUi){
 val s=ui.store;val stats=s.stats();val records=s.logs.entries().filter{it.first<=today()&&it.second is JSONObject}.sortedByDescending{it.first}
 SceneColumn(ui){
  V41ProgressBanner(ui)
  Eyebrow("ESTADÍSTICAS REALES");Heading("Tu registro")
  val strength=records.count{(_,v)->(v as JSONObject).b("complete")&&v.s("kind")=="strength"}
  val values=listOf("Días completados" to stats.i("days"),"Ejercicios registrados" to stats.i("exerciseCount"),"Series realizadas" to stats.i("series"),"Repeticiones" to stats.i("reps"),"Minutos registrados" to stats.i("seconds")/60,"Días de fuerza" to strength)
  for(pair in values.chunked(2))Row(horizontalArrangement=Arrangement.spacedBy(11.dp)){for((label,v)in pair)HPanel(Modifier.weight(1f),padding=17){Heading(v.toString(),size=35);HText(label,size=16,color=LocalHunter.current.muted)}}
  HPanel{Heading("Últimos 7 días · series realizadas",size=27)
   val days=(6L downTo 0L).map{plus(today(),-it)};val counts=days.map{d->s.logs.o(d).a("items").objects().sumOf{x->x.a("actual").objects().count{it.b("done")}}};val maximum=counts.maxOrNull()?.coerceAtLeast(1)?:1
   Row(Modifier.fillMaxWidth().height(120.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(8.dp)){for((i,date)in days.withIndex())Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Bottom){HText(counts[i].toString(),size=13);Box(Modifier.fillMaxWidth().height(max(3f,counts[i].toFloat()/maximum*70).dp).background(LocalHunter.current.accent));HText(LocalDate.parse(date).dayOfMonth.toString(),size=13)}}
  }
  val quick=records.count{(_,v)->val l=v as JSONObject;l.b("complete")&&l.a("items").objects().none{x->x.a("actual").objects().any{it.b("done")}}}
  HNotice("$quick misiones finalizadas por confirmación rápida. Sus cantidades no registradas no se suman a las estadísticas.")
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Heading("Diario de misiones",size=31);HText("${stats.i("xp")} XP",color=LocalHunter.current.accent)}
  if(records.isEmpty())HPanel{Heading("Tu historia empieza con la primera serie.",size=29);HText("Abre la misión de hoy para comenzar.",size=17,color=LocalHunter.current.muted)}
  for((date,v)in records.take(60)){val l=v as JSONObject;HPanel(onClick={ui.open("history",obj("date" to date))},padding=17){Eyebrow(date);Heading(kindLabel(l.s("kind")),size=26);HText("${if(l.b("complete"))"Completado" else if(l.b("pain"))"Detenido por molestias" else "En curso"} · RPE ${l.i("rpe")} · ${(s.fraction(l)*100).toInt()}%",size=16);l.s("notes").takeIf{it.isNotBlank()}?.let{HText(it,size=15,color=LocalHunter.current.muted)}}}
  HText("Los minutos suman únicamente los ejercicios medidos en segundos; no representan la duración total de la sesión. Un ejercicio registrado tiene al menos una serie marcada.",size=14,color=LocalHunter.current.muted)
 }
}
