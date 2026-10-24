package com.huntersystem.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
@Composable fun Header(ui:NativeUi){val s=ui.store;val rank=rankKey(s.stats().i("level",1));Row(Modifier.fillMaxWidth().padding(bottom=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){HText("✦",Modifier.width(30.dp),size=27,color=LocalHunter.current.accent,bold=true);Column(Modifier.weight(1f)){Heading("HUNTER SYSTEM",size=28);Eyebrow("SISTEMA DE SOMBRAS")};HText(rank,size=22,bold=true,color=LocalHunter.current.accent,modifier=Modifier.clickable{ui.open("evolution")});Avatar(s.state.s("avatar"),s.profile?.s("name")?:"C",Modifier.size(36.dp).clickable{ui.go("profile")})}}
@Composable fun SceneColumn(ui:NativeUi,content:@Composable ColumnScope.()->Unit){
 val scroll=rememberSaveable(ui.tab,ui.library,saver=ScrollState.Saver){ScrollState(0)}
 Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter){
  Column(Modifier.widthIn(max=960.dp).fillMaxWidth().fillMaxHeight()
    .verticalScroll(scroll).padding(horizontal=18.dp,vertical=20.dp),
    verticalArrangement=Arrangement.spacedBy(20.dp)){
    Header(ui);content();Spacer(Modifier.height(25.dp))
  }
 }
}
@Composable fun TopTabs(ui:NativeUi){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){for((id,title)in listOf("home" to "Panel","plan" to "Misiones","objectives" to "Objetivos"))HChip(title,ui.tab==id,Modifier.weight(1f)){ui.go(id)}}}
@Composable
fun HomeScreen(ui:NativeUi) {
 val stats=ui.store.stats()
 SceneColumn(ui) {
  MonarchDashboardWelcome(ui)
  MonarchHero(ui,stats)
  MonarchDashboardMission(ui)
  MonarchDashboardMetrics(ui,stats)
  MonarchHomeProgressStrip(ui)
  MonarchDashboardWeek(ui)
  MonarchDashboardShortcuts(ui)
  MonarchDashboardCompanion(ui)
  MonarchDashboardGoals(ui)
  MonarchDashboardGoalPlan(ui)
 }
}
@Composable fun Hero(ui:NativeUi,stats:JSONObject){val s=ui.store;val p=s.profile?:JSONObject();val c=LocalHunter.current;val rank=rankKey(stats.i("level",1)).lowercase();val appearance=s.state.o("appearance33");var collection=appearance.s("collection","hunter");if(collection=="alternate")collection=listOf("hunter","reader","regressor")[(LocalDate.now().toEpochDay()%3).toInt()];val char=if(collection=="hunter")"hunter-"+if(p.s("gender")=="female")"female" else "male" else collection;val path="${char}-$rank.webp";val art=remember(path){if(runCatching{s.context.assets.open("ui34/$path").close()}.isSuccess)"ui34/$path" else "ui33/$path"};val scene=when(appearance.s("scene","auto")){"sanctuary"->0;"journey"->1;else->((LocalDate.now().toEpochDay()-LocalDate.of(2026,1,1).toEpochDay())%2).toInt()}
 Box(Modifier.fillMaxWidth().height(420.dp)){Box(Modifier.fillMaxWidth().height(350.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(9.dp)).border(1.dp,c.line,RoundedCornerShape(9.dp))){AssetImage("ui33/scene-$rank-$scene.webp",Modifier.fillMaxSize(),ContentScale.Crop);Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(c.input,c.input.copy(alpha=.85f),c.input.copy(alpha=.25f)))))};AssetImage(art,Modifier.align(Alignment.TopEnd).offset(x=3.dp).fillMaxWidth(.58f).fillMaxHeight(),ContentScale.Crop,alignment=Alignment.TopCenter)
 Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(350.dp).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("SISTEMA EN LÍNEA");Eyebrow("RANGO ${rank.uppercase()}")};HText("${p.s("name","Cazador")} · ${s.catalog.editorial.o("goalPlans").o(p.s("finalTarget","general")).s("name","Tu siguiente nivel")}",Modifier.fillMaxWidth(.64f),size=14);Heading("SUPERA\nTUS LÍMITES.",size=46);HText("Una misión a la vez. Tu próximo nivel empieza hoy.",Modifier.fillMaxWidth(.60f),size=16,color=c.muted);Spacer(Modifier.weight(1f));Column(Modifier.fillMaxWidth(.65f),verticalArrangement=Arrangement.spacedBy(5.dp)){HText("NIVEL ${stats.i("level",1).toString().padStart(2,'0')}",bold=true,size=16);HText("RANGO ${rank.uppercase()} · ${s.catalog.design.a("ranks").objects().firstOrNull{it.s("key")==rank.uppercase()}?.s("name")?.uppercase()}",size=12);HProgress((stats.i("xp")%300)/300f);HText("${stats.i("xp")%300} / 300 XP",size=13,color=c.muted)}};HText("${rank.uppercase()}  EVOLUCIÓN ⟶",Modifier.align(Alignment.BottomEnd).padding(15.dp).clickable{ui.open("evolution")},size=12,color=c.accent)
}}
@Composable fun MissionCompanion(ui:NativeUi){HPanel(padding=12,onClick={ui.open("coach")}){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){AssetImage("ui33/companion-${companionKey(ui.store.profile)}.webp",Modifier.width(67.dp).height(88.dp));Column{Eyebrow("IA DE MISIÓN");Heading(companionName(ui.store.profile),size=26);HText("La misma IA que elegiste en Perfil",size=14,color=LocalHunter.current.muted)}}}}
@Composable fun MissionLaunch(ui:NativeUi){val s=ui.store;val d=s.day(today());val l=s.logs.optJSONObject(today());val total=d.a("items").length()+d.a("tasks").length();HPanel{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("MISIÓN DIARIA DISPONIBLE");HText("${(s.fraction(l)*total).toInt()}/$total",size=16)};Heading(d.s("title"),size=33);HText("Una ventana. Tus objetivos. Cada casilla cuenta.",size=16,color=LocalHunter.current.muted);HButton("ABRIR VENTANA DE MISIONES →",Modifier.fillMaxWidth(),primary=true){ui.open("mission",obj("date" to today()))};HButton("✎ Personalizar la misión"){ui.open("custom",obj("date" to today()))}}}
@Composable fun AcademyTile(ui:NativeUi){HPanel(onClick={ui.open("academy")}){Eyebrow("EL CONOCIMIENTO TAMBIÉN SUBE DE NIVEL");Heading("Explorar la academia ↗",size=28);HText("Técnica, descanso y preparación en casa",size=16,color=LocalHunter.current.muted)}}
@Composable fun GoalsPanel(ui:NativeUi){val s=ui.store;HPanel{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Heading("Tus objetivos",size=29);HText("Ver todos ↗",Modifier.clickable{ui.go("objectives")},size=16,color=LocalHunter.current.accent)};val goals=s.state.a("goals").objects();if(goals.isEmpty())HText("Crea una meta y márcala cuando la hayas terminado.",size=16,color=LocalHunter.current.muted);for(g in goals.take(5))HCheck(g.s("title",g.s("text")),today() in g.a("dates").strings()){ui.attempt{s.toggleGoal(g.s("id"))}};HButton("+ Añadir objetivo"){ui.open("goal-edit")}}}
@Composable fun ObjectivesScreen(ui:NativeUi){val s=ui.store;SceneColumn(ui){TopTabs(ui);MonarchSectionHeading("OBJETIVOS PERSONALES","Cada meta cuenta.");GoalsPanel(ui);for(g in s.state.a("goals").objects())HPanel{Heading(g.s("title",g.s("text")),size=28);HText(if(g.s("repeat")=="daily")"Repetir cada día" else "Objetivo personal",size=15);HCheck("Completado hoy",today() in g.a("dates").strings()){s.toggleGoal(g.s("id"))};HButton("Editar objetivo"){ui.open("goal-edit",g)};HButton("Eliminar"){ui.open("delete-goal",g)}}}}
@Composable fun CyclePanel(ui:NativeUi){val s=ui.store;val monday=LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value-1).toLong()).toString();HPanel{Eyebrow("CICLO ${s.state.a("cycles").length().coerceAtLeast(1)}");Heading("Tu arco de 7 días",size=29);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){for(i in 0L..6L){val date=plus(monday,i);val d=s.day(date);val checked=s.logs.o(date).b("complete");val short=mapOf("strength" to "FUE","cardio" to "CAR","recovery" to "REC","rest" to "DES")[d.s("kind")]?:"MIX";Column(Modifier.weight(1f).clip(RoundedCornerShape(5.dp)).background(if(date==today())LocalHunter.current.selected else LocalHunter.current.input).clickable{ui.open("mission",obj("date" to date))}.padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally){HText(if(checked)"✓" else (i+1).toString(),size=18);HText(short,size=11)}}};HText("FUE Fuerza   CAR Cardio   REC Recuperación",size=13,color=LocalHunter.current.muted);HText("Generación dinámica de sesiones: calendario, objetivo, historial y restricciones. Se conservan sesiones registradas.",size=15,color=LocalHunter.current.muted);HButton("Ver todas las misiones →"){ui.go("plan")}}}
@Composable fun WeeklyPanel(ui:NativeUi){val s=ui.store;val monday=LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value-1).toLong()).toString();val training=(0L..6L).map{plus(monday,it)}.filter{it<=today()&&s.day(it).s("kind")!="rest"};val done=training.count{s.logs.o(it).b("complete")};val fraction=if(training.isEmpty())0f else done.toFloat()/training.size;val stats=s.stats();HPanel{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("PROGRESO DE LA SEMANA");HText("${(fraction*100).toInt()}%",bold=true)};HProgress(fraction);Heading("$done de ${training.size} días de ejercicio",size=26);HText("Completa tus misiones y registra el esfuerzo. La siguiente revisión será el lunes.",size=16,color=LocalHunter.current.muted);HText("${stats.i("xp")} XP acumulada · Nivel ${stats.i("level",1)}",size=16);HButton("Ver calendario mensual"){ui.go("plan")};HButton("Ver progreso →"){ui.go("stats")};HButton("Revisión semanal"){ui.open("review")}}}
@Composable fun TrainingMode(ui:NativeUi){val p=ui.store.profile?:return;Chips(listOf("challenges" to "Desafíos diarios","routines" to "Rutinas completas"),p.s("trainingMode","challenges")){mode->ui.attempt{ui.store.saveProfile(p.copyJson().put("trainingMode",mode))}}}
/** Fachada histórica para navegación; implementación Fase 05 en MonarchMissionsV42.kt. */
@Composable fun PlanScreen(ui:NativeUi) { MonarchMissionsScreen(ui) }
@Composable fun MissionContent(ui:NativeUi,date:String) {
    MonarchMissionContent(ui,date)
}
@Composable fun CalendarPanel(ui:NativeUi) { MonarchMissionsCalendar(ui) }
/** Fachadas de compatibilidad: navegación existente apuntando a rutinas Monarca V42.5. */
@Composable fun RoutinesScreen(ui:NativeUi){MonarchRoutinesScreen(ui)}
@Composable fun RoutineLibrary(ui:NativeUi,mode:String){MonarchRoutinesLibrary(ui,mode)}
@Composable fun RoutineGenerator(ui:NativeUi,mode:String){MonarchRoutineGenerator(ui,mode){}}
@Composable fun RoutineTile(ui:NativeUi,r:JSONObject){MonarchRoutineCard(ui,r,ui.store.adjusted())}
@Composable fun ExercisesScreen(ui:NativeUi){val s=ui.store;var query by remember{mutableStateOf("")};var category by remember{mutableStateOf("")};var group by remember{mutableStateOf("")};var n by remember{mutableStateOf("")};var gear by remember{mutableStateOf("")};var variants by remember{mutableStateOf(false)};var restricted by remember{mutableStateOf(false)};val list=remember(query,category,group,n,gear,variants,restricted,s.revision.intValue){s.catalog.exercises.filter{e->(query.isBlank()||normalize(e.name).contains(normalize(query)))&&(category.isBlank()||e.category==category)&&(group.isBlank()||e.group==group)&&(n.isBlank()||e.raw.i("level").toString()==n)&&(variants||e.raw.s("variant")=="base")&&(!restricted||s.profile!=null&&s.rules.compatible(e,s.adjusted()))&&(gear.isBlank()||when(gear){"body"->e.equipment=="body"&&!e.gym;"db"->e.equipment=="db";"gym"->e.gym;else->gear in e.raw.a("materialRequirements").strings()})}}
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(top=20.dp,bottom=30.dp)){item{Header(ui)};item{AcademyTile(ui)};item{Eyebrow("ARCHIVO DE ENTRENAMIENTO");Heading("Todos los ejercicios");HText("1200 fichas: 412 movimientos base + variantes explicadas",size=16,color=LocalHunter.current.muted)};item{HInput(query,{query=it},"Buscar ejercicio");Spacer(Modifier.height(12.dp));Chips(listOf("" to "Todas","Fuerza" to "Fuerza","Cardio" to "Cardio","Movilidad" to "Movilidad"),category){category=it}};item{Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){HSelect("Grupo muscular",group,listOf("" to "Todos")+s.catalog.exercises.map{it.group}.distinct().map{it to it},Modifier.weight(1f)){group=it};HSelect("Nivel",n,listOf("" to "Todos","0" to "Inicial","1" to "Intermedio","2" to "Avanzado"),Modifier.weight(1f)){n=it};HSelect("Equipo",gear,listOf("" to "Todos","body" to "Sin equipo","db" to "Mancuernas","bench" to "Banco","step" to "Plataforma","gym" to "Gimnasio","barbell" to "Barras","cable" to "Poleas","machine" to "Máquinas","smith" to "Smith","cardio" to "Cinta o ergómetro"),Modifier.weight(1f)){gear=it}};HCheck("Incluir variantes de técnica y ritmo",variants){variants=!variants};HCheck("Aplicar mis restricciones",restricted){restricted=!restricted};HText("${list.size} resultados",size=15,color=LocalHunter.current.muted)};items(list,key={it.id}){e->HPanel(onClick={ui.open("exercise",obj("id" to e.id))},padding=18){Eyebrow("${e.group} · ${listOf("Inicial","Intermedio","Avanzado")[e.raw.i("level").coerceIn(0,2)]}");Heading(e.name,size=27);HText(if(e.equipment=="body")"Sin equipo especial" else if(e.gym)"Equipo de gimnasio" else "Mancuernas",size=15,color=LocalHunter.current.muted);HText("${if(e.impact)"Con saltos" else "Sin saltos"} · ${if(e.unit=="sec")"Por tiempo" else "Por repeticiones"}  ↗",size=15,color=LocalHunter.current.muted)}}}
}
@Composable fun ProgressScreen(ui:NativeUi){ MonarchProgressScreen(ui) }
