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
import androidx.compose.ui.semantics.*
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
 val scrolls=mutableMapOf<String,ScrollState>()
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
@Composable fun Header(ui:NativeUi){
 val c=LocalHunter.current
 Row(Modifier.fillMaxWidth().padding(bottom=2.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
  Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(c.selected),contentAlignment=Alignment.Center){HunterIcon(Sigil.Crown,Modifier.size(26.dp))}
  Column(Modifier.weight(1f)){Heading("HUNTER SYSTEM",size=20);Eyebrow("ECLIPSE · ASCENSIÓN")}
  if(ui.store.profile!=null)HIconButton(Sigil.Bell,"Recordatorios"){ui.open("reminders")}
  Avatar(ui.store.state.s("avatar"),ui.store.profile?.s("name")?:"C",Modifier.size(44.dp).clickable(role=Role.Button){ui.go("profile")}.semantics{contentDescription="Abrir perfil"})
 }
}
@Composable fun SceneColumn(ui:NativeUi,content:@Composable ColumnScope.()->Unit){
 Column(Modifier.fillMaxSize().verticalScroll(ui.scrolls.getOrPut(ui.tab+"/"+ui.library){ScrollState(0)}).padding(horizontal=20.dp,vertical=22.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
  Header(ui);content();Spacer(Modifier.height(24.dp))
 }
}
@Composable fun TopTabs(ui:NativeUi){
 Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
  for((id,title)in listOf("home" to "Resumen","plan" to "Misiones","objectives" to "Objetivos","stats" to "Progreso"))HChip(title,ui.tab==id){ui.go(id)}
 }
}
@Composable fun HomeScreen(ui:NativeUi){
 val s=ui.store;val stats=s.stats();val c=LocalHunter.current
 SceneColumn(ui){
  Hero(ui,stats)
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
   StatCard(stats.i("streak").toString(),"Días de racha",Sigil.Flame,Modifier.weight(1f))
   StatCard(stats.i("days").toString(),"Días completados",Sigil.Check,Modifier.weight(1f))
   StatCard(stats.i("xp").toString(),"XP acumulada",Sigil.Star,Modifier.weight(1f))
  }
  MissionLaunch(ui)
  Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
   HPanel(Modifier.weight(1f),onClick={ui.go("routines")},padding=17){HunterIcon(Sigil.Exercise);Heading("Entrenar",size=23);HText("Explorar rutinas",size=14,color=c.muted)}
   HPanel(Modifier.weight(1f),onClick={ui.go("stats")},padding=17){HunterIcon(Sigil.Chart);Heading("Mi progreso",size=23);HText("Ver mis registros",size=14,color=c.muted)}
  }
  MissionCompanion(ui);CyclePanel(ui);GoalsPanel(ui);WeeklyPanel(ui);AcademyTile(ui)
 }
}
@Composable fun Hero(ui:NativeUi,stats:JSONObject){
 val s=ui.store;val p=s.profile?:JSONObject();val c=LocalHunter.current;val tier=LocalVisualTier.current
 val rank=rankKey(stats.i("level",1));val appearance=s.state.o("appearance33")
 var collection=appearance.s("collection","hunter")
 if(collection=="alternate")collection=listOf("hunter","reader","regressor")[(LocalDate.now().toEpochDay()%3).toInt()]
 val character=if(collection=="hunter")"hunter-"+if(p.s("gender")=="female")"female" else "male" else collection
 val filename="$character-${rank.lowercase()}.webp"
 val art=remember(filename){if(runCatching{s.context.assets.open("ui34/$filename").close()}.isSuccess)"ui34/$filename" else "ui33/$filename"}
 val phase=ambientPhase(if(tier==VisualTier.MAX)1f else .65f)
 val mode=when(tier){VisualTier.MAX->"relic";VisualTier.BALANCED->"depth";VisualTier.SAVER->"flat"}
 HPanel(padding=0){
  Box(Modifier.fillMaxWidth().height((330 * androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)).dp)){
   AssetImage(chosenSceneArtwork(appearance.s("scene","auto")),Modifier.fillMaxSize(),ContentScale.Crop)
   Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(c.bg.copy(alpha=.92f),c.bg.copy(alpha=.35f),Color.Transparent))))
   Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,c.panel))))
   if(mode=="relic")RelicScene(Modifier.align(Alignment.TopEnd).size(140.dp))
   AssetImage(art,Modifier.align(Alignment.BottomEnd).fillMaxWidth(.56f).fillMaxHeight(.95f).graphicsLayer{
    if(tier!=VisualTier.SAVER){translationY=sin(phase.value*PI.toFloat()*2)*(if(tier==VisualTier.MAX)6 else 3).dp.toPx();rotationY=sin(phase.value*PI.toFloat()*2)*3f;cameraDistance=16*density}
   },ContentScale.Fit,alignment=Alignment.BottomCenter)
   Column(Modifier.padding(22.dp).fillMaxWidth(.70f),verticalArrangement=Arrangement.spacedBy(13.dp)){
    Box(Modifier.clickable{ui.open("evolution")}){StatusPill("RANGO $rank",Sigil.Crown)}
    HText("${p.s("name","Cazador")},",size=17,color=c.muted,maxLines=1)
    Heading("Tu siguiente\nnivel empieza\nhoy.",size=32)
    HText("Un paso más allá.",size=16,color=c.muted)
   }
  }
  Column(Modifier.fillMaxWidth().padding(start=22.dp,end=22.dp,bottom=22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){HText("NIVEL ${stats.i("level",1).toString().padStart(2,'0')}",size=14,bold=true);HText("${stats.i("xp")%300} / 300 XP",size=14,color=c.muted)}
   HProgress((stats.i("xp")%300)/300f)
   HButton("Abrir misión de hoy",Modifier.fillMaxWidth(),primary=true,icon=Sigil.Play){ui.open("mission",obj("date" to today()))}
  }
 }
}
@Composable fun MissionCompanion(ui:NativeUi){
 HPanel(padding=16,onClick={ui.open("coach")}){
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
   CompanionStage(companionKey(ui.store.profile),Modifier.size(90.dp,110.dp))
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){
    Eyebrow("A TU LADO");Heading(companionName(ui.store.profile),size=25)
    val day=ui.store.day(today())
    val group=GroupEvolution.sessionGroup(day,ui.store.profile,ui.store.catalog)?:""
    HText(CompanionDialogue.message(ui.store.profile,LocalVisualTier.current,
     DialogueContext(DialogueMoment.WELCOME,group=group)),size=15,color=LocalHunter.current.muted)
    HText("Abrir guía personal",size=14,color=LocalHunter.current.accent,bold=true)
   }
   HunterIcon(Sigil.Arrow,Modifier.size(18.dp))
  }
 }
}
@Composable fun MissionLaunch(ui:NativeUi){
 val s=ui.store;val day=s.day(today());val log=s.logs.optJSONObject(today());val c=LocalHunter.current
 HPanel(padding=20){
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Eyebrow("TU MISIÓN DE HOY");Heading(day.s("title").substringBefore(" · plan dinámico"),size=28)
    HText(if(log?.b("complete")==true)"Misión completada" else "${day.a("items").length()} ejercicios preparados",size=15,color=c.muted)
   }
   ProgressRing(s.fraction(log).toFloat(),"COMPLETADO")
  }
  HButton(if(day.s("kind")=="rest")"Ver día de descanso" else if(log?.b("complete")==true)"Revisar misión" else "Comenzar misión",Modifier.fillMaxWidth(),primary=true,icon=Sigil.Play){ui.open("mission",obj("date" to today()))}
  HButton("Personalizar misión",Modifier.fillMaxWidth(),icon=Sigil.Settings){ui.open("custom",obj("date" to today()))}
 }
}
@Composable fun AcademyTile(ui:NativeUi){HPanel(onClick={ui.open("academy")}){Eyebrow("EL CONOCIMIENTO TAMBIÉN SUBE DE NIVEL");Heading("Explorar la academia ↗",size=28);HText("Técnica, descanso y preparación en casa",size=16,color=LocalHunter.current.muted)}}
@Composable fun GoalsPanel(ui:NativeUi){val s=ui.store;HPanel{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Heading("Tus objetivos",Modifier.weight(1f),size=29);HText("Ver todos ↗",Modifier.clickable{ui.go("objectives")},size=16,color=LocalHunter.current.accent)};val goals=s.state.a("goals").objects();if(goals.isEmpty())HText("Crea una meta y márcala cuando la hayas terminado.",size=16,color=LocalHunter.current.muted);for(g in goals.take(5))HCheck(g.s("title",g.s("text")),today() in g.a("dates").strings()){ui.attempt{s.toggleGoal(g.s("id"))}};HButton("+ Añadir objetivo"){ui.open("goal-edit")}}}
@Composable fun ObjectivesScreen(ui:NativeUi){val s=ui.store;SceneColumn(ui){TopTabs(ui);Eyebrow("OBJETIVOS PERSONALES");Heading("Cada meta cuenta.");GoalsPanel(ui);for(g in s.state.a("goals").objects())HPanel{Heading(g.s("title",g.s("text")),size=28);HText(if(g.s("repeat")=="daily")"Repetir cada día" else "Objetivo personal",size=15);HCheck("Completado hoy",today() in g.a("dates").strings()){s.toggleGoal(g.s("id"))};HButton("Editar objetivo"){ui.open("goal-edit",g)};HButton("Eliminar"){ui.open("delete-goal",g)}}}}
@Composable fun CyclePanel(ui:NativeUi){val s=ui.store;val monday=LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value-1).toLong()).toString();HPanel{Eyebrow("CICLO ${s.state.a("cycles").length().coerceAtLeast(1)}");Heading("Tu arco de 7 días",size=29);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){for(i in 0L..6L){val date=plus(monday,i);val d=s.day(date);val checked=s.logs.o(date).b("complete");val short=mapOf("strength" to "FUE","cardio" to "CAR","recovery" to "REC","rest" to "DES")[d.s("kind")]?:"MIX";Column(Modifier.weight(1f).clip(RoundedCornerShape(5.dp)).background(if(date==today())LocalHunter.current.selected else LocalHunter.current.input).clickable{ui.open("mission",obj("date" to date))}.padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally){HText(if(checked)"✓" else (i+1).toString(),size=18);HText(short,size=11)}}};HText("FUE Fuerza   CAR Cardio   REC Recuperación",size=13,color=LocalHunter.current.muted);HText("Generación dinámica de sesiones: calendario, objetivo, historial y restricciones. Se conservan sesiones registradas.",size=15,color=LocalHunter.current.muted);HButton("Ver todas las misiones →"){ui.go("plan")}}}
@Composable fun WeeklyPanel(ui:NativeUi){val s=ui.store;val monday=LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value-1).toLong()).toString();val training=(0L..6L).map{plus(monday,it)}.filter{it<=today()&&s.day(it).s("kind")!="rest"};val done=training.count{s.logs.o(it).b("complete")};val fraction=if(training.isEmpty())0f else done.toFloat()/training.size;val stats=s.stats();HPanel{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("PROGRESO DE LA SEMANA");HText("${(fraction*100).toInt()}%",bold=true)};HProgress(fraction);Heading("$done de ${training.size} días de ejercicio",size=26);HText("Completa tus misiones y registra el esfuerzo. La siguiente revisión será el lunes.",size=16,color=LocalHunter.current.muted);HText("${stats.i("xp")} XP acumulada · Nivel ${stats.i("level",1)}",size=16);HButton("Ver calendario mensual"){ui.go("plan")};HButton("Ver progreso →"){ui.go("stats")};HButton("Revisión semanal"){ui.open("review")}}}
@Composable fun TrainingMode(ui:NativeUi){val p=ui.store.profile?:return;Chips(listOf("challenges" to "Desafíos diarios","routines" to "Rutinas completas"),p.s("trainingMode","challenges")){mode->ui.attempt{ui.store.saveProfile(p.copyJson().put("trainingMode",mode))}}}
@Composable fun PlanScreen(ui:NativeUi){SceneColumn(ui){TopTabs(ui);Eyebrow("TU SISTEMA · SEMANA ${ui.store.state.a("cycles").length().coerceAtLeast(1)}");Heading("Una misión a la vez.");TrainingMode(ui);MissionCompanion(ui);MissionContent(ui,today());if(ui.store.day(today()).a("items").length()>0)HPanel{Eyebrow("SESIÓN GUIADA · ANIMACIONES 2D");Heading("Un paso a la vez.",size=30);HText(ui.store.day(today()).s("planningNote"),size=16,color=LocalHunter.current.muted);HButton("Abrir sesión paso a paso",Modifier.fillMaxWidth(),primary=true){ui.open("session",obj("date" to today()))}};WeeklyPanel(ui);CalendarPanel(ui)}}
@Composable fun MissionContent(ui:NativeUi,date:String){val s=ui.store;val day=s.day(date);val l=s.logs.optJSONObject(date);val items=l?.a("items")?.objects()?:day.a("items").objects();val tasks=l?.a("missionTasks")?.objects()?:day.a("tasks").objects();val closed=l?.b("complete")==true||l?.b("pain")==true||date!=today();HPanel{Eyebrow("NOTIFICACIÓN DEL SISTEMA");Heading("MISIÓN DIARIA",size=32);Heading(day.s("title"),size=25);HText("${(s.fraction(l)*(items.size+tasks.size)).roundToInt()} / ${items.size+tasks.size} objetivos completados",size=16,color=LocalHunter.current.muted);day.s("blocked381").takeIf{it.isNotBlank()}?.let{HNotice(it)}
 val adjustment=items.mapNotNull{it.optJSONObject("groupAdaptation")}.firstOrNull()
 if(adjustment!=null){
  val delta=adjustment.i("difference")
  val adaptedExercise=items.firstOrNull{it.optJSONObject("groupAdaptation")!=null}?.let{s.catalog.byId[it.s("id")]}
  val unit=if(adaptedExercise?.unit=="sec")"segundos" else "repeticiones"
  HNotice("Adaptación de "+GroupEvolution.label(adjustment.s("group"))+" · "+
   (if(delta>0)"+" else "")+delta+" "+
   unit+
   ". Basada en la evaluación del "+adjustment.s("previousDate")+
   " (esfuerzo "+adjustment.i("rpe")+"/10), revisada en períodos de 7, 14 y 21 días.")
 }
 for((i,x)in items.withIndex()){val e=s.catalog.byId[x.s("id")]?:continue;Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){HCheck(e.name,x.b("missionState")||x.b("selfReported")||x.a("actual").objects().count{it.b("done")}>=x.i("sets"),enabled=!closed&&!x.b("archived")){ui.attempt{s.toggleMission(date,i)}};HText("${x.i("sets")} × ${x.i("target")} ${if(e.unit=="sec")"segundos" else "repeticiones"}${if(e.sides)" por lado" else ""}",Modifier.padding(start=33.dp),size=15,color=LocalHunter.current.muted)};HText("↗",Modifier.clickable{ui.open("exercise",obj("id" to e.id))}.padding(10.dp),color=LocalHunter.current.accent)}}
 for((i,x)in tasks.withIndex())HCheck(x.s("text"),x.b("done"),enabled=!closed){ui.attempt{s.toggleTask(date,i)}}
 HText(if(l?.b("complete")==true)"✓ MISIÓN COMPLETADA" else if(l?.b("pain")==true)"SESIÓN DETENIDA" else "◇ MISIÓN EN CURSO",color=LocalHunter.current.accent,size=16,bold=true)
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){HButton("✎ Modificar misión",Modifier.weight(1f)){ui.open("custom",obj("date" to date))};HButton("Registro detallado",Modifier.weight(1f),enabled=date==today()){ui.open("record",obj("date" to date))}}
 HText("Marcar una casilla confirma que terminaste. No añade series, repeticiones ni kg que no hayas registrado. Puedes desmarcar para corregir.",size=14,color=LocalHunter.current.muted)
}}
@Composable fun CalendarPanel(ui:NativeUi){var month by remember{mutableStateOf(YearMonth.now())};val locale=Locale("es");val start=(month.atDay(1).dayOfWeek.value-1);HPanel{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){HButton("‹"){month=month.minusMonths(1)};Heading(month.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy",locale)),size=25);HButton("›"){month=month.plusMonths(1)}};Row{for(t in listOf("L","M","X","J","V","S","D"))Box(Modifier.weight(1f),contentAlignment=Alignment.Center){HText(t,size=14,color=LocalHunter.current.muted)}};for(row in 0..5){if(row*7>=start+month.lengthOfMonth())break;Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){for(col in 0..6){val number=row*7+col-start+1;if(number !in 1..month.lengthOfMonth()){Spacer(Modifier.weight(1f));continue};val date=month.atDay(number).toString();val checked=ui.store.logs.o(date).b("complete");val train=ui.store.profile?.a("weekPlan")?.optString(weekday(date),"rest")!="rest";Column(Modifier.weight(1f).clip(RoundedCornerShape(5.dp)).background(if(date==today())LocalHunter.current.selected else LocalHunter.current.input).clickable{ui.open("mission",obj("date" to date))}.padding(vertical=6.dp),horizontalAlignment=Alignment.CenterHorizontally){HText(number.toString(),size=17);HText(if(checked)"✓" else if(train)"•" else "—",size=13,color=LocalHunter.current.accent)}}}};HText("• entrenamiento · — descanso · ✓ completado. Las semanas futuras son previsiones y se revisan con tus registros.",size=14,color=LocalHunter.current.muted)}}
@Composable fun RoutinesScreen(ui:NativeUi){
 val s=ui.store;if(ui.library.isNotBlank()){RoutineLibrary(ui,ui.library);return}
 SceneColumn(ui){
  Eyebrow("CENTRO DE ENTRENAMIENTO");Heading("Elige tu\nsiguiente desafío.")
  HText("Tu tiempo, tu nivel, tu siguiente paso.",color=LocalHunter.current.muted)
  TrainingMode(ui)
  HPanel(onClick={ui.library="offline"},padding=22){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){HunterIcon(Sigil.Exercise,Modifier.size(36.dp));StatusPill("SIN CONEXIÓN",Sigil.Shield)}
   Heading("Tu campo de entrenamiento",size=30)
   HText("${s.catalog.routines.count{!it.b("gymOnly23")}} rutinas · fuerza, cardio y movilidad",size=16,color=LocalHunter.current.muted)
   HButton("Explorar y generar",Modifier.fillMaxWidth(),primary=true,icon=Sigil.Arrow){ui.library="offline"}
  }
  HPanel(onClick={ui.library="gym"},padding=22){
   HunterIcon(Sigil.Routine,Modifier.size(34.dp));Heading("El siguiente nivel.\nEn el gimnasio.",size=30)
   HText("${s.catalog.routines.count{it.b("gymOnly23")}} rutinas con equipamiento",size=16,color=LocalHunter.current.muted)
   HButton("Explorar gimnasio",Modifier.fillMaxWidth(),icon=Sigil.Arrow){ui.library="gym"}
  }
  HButton("Crear mi rutina",Modifier.fillMaxWidth(),icon=Sigil.Plus){ui.open("routine-editor")}
  SectionTitle("Entrenadores de referencia","Consulta sus publicaciones originales",Sigil.Book)
  for(ch in s.catalog.editorial.a("channels").objects())HPanel(onClick={ui.open("creator",obj("key" to ch.s("key")))},padding=18){
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){HunterIcon(Sigil.Profile);Column(Modifier.weight(1f)){Heading(ch.s("name"),size=23);HText("Canal y enlaces originales",size=14,color=LocalHunter.current.muted)};HunterIcon(Sigil.Arrow)}
  }
  HNotice("Las secuencias de entrenamiento incluidas son diseños originales de Hunter System. Los enlaces de los creadores necesitan conexión.")
  HButton("Fuentes y publicaciones",icon=Sigil.Book){ui.open("sources")}
 }
}
@Composable fun RoutineLibrary(ui:NativeUi,mode:String){val s=ui.store;var n by remember(mode){mutableIntStateOf(level(s.profile?:JSONObject()))};var category by remember(mode){mutableStateOf("")};var query by remember(mode){mutableStateOf("")};val generated=ui.generated[mode];val base=generated?:(s.catalog.routines+s.state.a("customRoutines").objects()).filter{it.b("gymOnly23")== (mode=="gym")};val list=base.filter{it.i("level")==n&&(category.isBlank()||it.s("category")==category)&&(query.isBlank()||normalize(it.s("name")).contains(normalize(query)))};LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(top=20.dp,bottom=36.dp)){item{Header(ui)};item{HButton("‹ Volver al centro"){ui.library=""}};item{RoutineGenerator(ui,mode)};item{Eyebrow(if(mode=="gym")"GIMNASIO" else "BIBLIOTECA SIN CONEXIÓN");Heading(if(mode=="gym")"Rutinas especializadas en gimnasio" else "Rutinas de Hunter System");HText(if(generated!=null)"Rutinas nuevas generadas localmente; las combinaciones no se guardan." else "Fuerza, cardio, movilidad y control. Selecciona tu nivel.",color=LocalHunter.current.muted)};item{Chips(listOf("0" to "Básico","1" to "Intermedio","2" to "Avanzado"),n.toString()){n=it.toInt()}};item{HInput(query,{query=it},"Buscar rutina");Spacer(Modifier.height(10.dp));Chips(listOf("" to "Todas","Fuerza" to "Fuerza","Cardio" to "Cardio","Movilidad" to "Movilidad","Mixta" to "Mixtas"),category){category=it}};item{HText("${list.size} resultados",size=16,color=LocalHunter.current.muted)};items(list,key={it.s("id")}){r->RoutineTile(ui,r)};if(list.isEmpty())item{HNotice("No hay ejercicios compatibles con estos filtros. Prueba otra categoría o duración, o revisa las restricciones y el equipo registrado en Perfil.")}}}
@Composable fun RoutineGenerator(ui:NativeUi,mode:String){val s=ui.store;val scope=rememberCoroutineScope();val chosen=remember(mode){mutableStateListOf<String>()};var duration by remember(mode){mutableStateOf("")};var busy by remember{mutableStateOf(false)};var info by remember(mode){mutableStateOf("")};val colors=listOf("#237CFF","#F44295","#8B51F5","#00B79A","#F1B837")
 HPanel(decor=false,padding=18,modifier=Modifier.border(1.dp,LocalHunter.current.secondary,RoundedCornerShape(16.dp))){Eyebrow("HUNTER SYSTEM · GENERADOR DINÁMICO");Heading("Diseña tu próxima rutina",size=31);HText("1200 demos 2D",size=14,color=LocalHunter.current.accent);HText("Elige los tipos de ejercicio y el tiempo. Los entrenamientos se generan por separado para cada categoría y para los tres niveles existentes.",size=16,color=LocalHunter.current.muted);Eyebrow("01 · GRUPOS DE ENTRENAMIENTO");HText("Selección múltiple",size=13,color=LocalHunter.current.muted)
 for(pair in groupLabels.entries.toList().chunked(2))Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){for(entry in pair){val i=groupLabels.keys.indexOf(entry.key);val color=parseColor(colors[i]);Row(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if(entry.key in chosen)color.copy(alpha=.20f) else LocalHunter.current.input).border(1.dp,if(entry.key in chosen)color else LocalHunter.current.line,RoundedCornerShape(10.dp)).clickable{if(!chosen.remove(entry.key))chosen+=entry.key}.padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){HunterIcon(when(entry.key){"cardio"->Sigil.Flame;"mobility"->Sigil.Layers;"core"->Sigil.Target;else->Sigil.Exercise},Modifier.size(24.dp),color);HText(if(entry.key=="mobility")"Movilidad" else entry.value,size=16)}};if(pair.size==1)Spacer(Modifier.weight(1f))}
 Eyebrow("02 · DURACIÓN DE LA SESIÓN");Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){for((id,label)in listOf("short" to "5–10 min","medium" to "10–20 min","long" to "Hasta 30 min"))HChip(label,duration==id,Modifier.weight(1f)){duration=id}}
 HButton(if(busy)"Generando…" else "✧ Generar nuevas rutinas",Modifier.fillMaxWidth(),primary=true,enabled=!busy){if(chosen.isEmpty())info="Elige al menos un grupo de entrenamiento." else if(duration.isBlank())info="Selecciona uno de los tres intervalos de duración." else if(mode=="gym"&&s.profile?.s("equipment")!="gym")info="Para generar rutinas de gimnasio, configura primero el equipo de gimnasio en Perfil." else{busy=true;val p=s.adjusted();val logs=s.logs.copyJson();val types=chosen.toList();val range=duration;val epoch=++ui.serial;scope.launch{try{val rs=withContext(Dispatchers.Default){(0..2).flatMap{n->types.flatMap{type->(0..1).mapNotNull{v->s.planner.generateRoutine(mode,type,n,v,range,epoch,p,logs)}}}};if(rs.isEmpty())info="Sin rutinas compatibles con esta selección. Revisa el equipo, las restricciones o el tiempo." else{ui.generated[mode]=rs;info="${rs.size} rutinas listas. Selecciona el nivel debajo para verlas."}}catch(e:Exception){info=e.message?:"No se pudieron generar las rutinas"}finally{busy=false}}}}
 if(info.isNotBlank())HText(info,size=15,color=LocalHunter.current.accent);if(ui.generated[mode]!=null)HButton("Ver biblioteca original"){ui.generated.remove(mode);info=""}
}}
@Composable fun RoutineTile(ui:NativeUi,r:JSONObject){
 val s=ui.store
 HPanel(padding=20){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){StatusPill(r.s("category"),Sigil.Exercise);HText("≈ ${r.i("minutes",s.planner.minutes(r,s.adjusted()))} min",size=14,color=LocalHunter.current.muted)}
  Heading(r.s("name"),size=28)
  HText(r.a("items").objects().take(3).joinToString(" · "){s.catalog.byId[it.s("id")]?.name?:it.s("id")},size=16,color=LocalHunter.current.muted,maxLines=3)
  HText("${r.a("items").length()} ejercicios · ${r.a("items").objects().sumOf{it.i("sets")}} series",size=14,color=LocalHunter.current.secondary)
  HButton("Abrir secuencia 2D",Modifier.fillMaxWidth(),primary=true,icon=Sigil.Play){ui.open("routine",obj("id" to r.s("id")))}
 }
}
@Composable fun ExercisesScreen(ui:NativeUi){val s=ui.store;var query by remember{mutableStateOf("")};var category by remember{mutableStateOf("")};var group by remember{mutableStateOf("")};var n by remember{mutableStateOf("")};var gear by remember{mutableStateOf("")};var variants by remember{mutableStateOf(false)};var restricted by remember{mutableStateOf(false)};val list=remember(query,category,group,n,gear,variants,restricted,s.revision.intValue){s.catalog.exercises.filter{e->(query.isBlank()||normalize(e.name).contains(normalize(query)))&&(category.isBlank()||e.category==category)&&(group.isBlank()||e.group==group)&&(n.isBlank()||e.raw.i("level").toString()==n)&&(variants||e.raw.s("variant")=="base")&&(!restricted||s.profile!=null&&s.rules.compatible(e,s.adjusted()))&&(gear.isBlank()||when(gear){"body"->e.equipment=="body"&&!e.gym;"db"->e.equipment=="db";"gym"->e.gym;else->gear in e.raw.a("materialRequirements").strings()})}}
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(top=20.dp,bottom=36.dp)){item{Header(ui)};item{AcademyTile(ui)};item{Eyebrow("ARCHIVO DE ENTRENAMIENTO");Heading("Todos los ejercicios");HText("${s.catalog.exercises.size} fichas: ${s.catalog.exercises.count{it.raw.s("variant")=="base"}} movimientos base y variantes",size=16,color=LocalHunter.current.muted)};item{HInput(query,{query=it},"Buscar ejercicio");Spacer(Modifier.height(12.dp));Chips(listOf("" to "Todas","Fuerza" to "Fuerza","Cardio" to "Cardio","Movilidad" to "Movilidad"),category){category=it}};item{Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){HSelect("Grupo muscular",group,listOf("" to "Todos")+s.catalog.exercises.map{it.group}.distinct().map{it to it},Modifier.weight(1f)){group=it};HSelect("Nivel",n,listOf("" to "Todos","0" to "Inicial","1" to "Intermedio","2" to "Avanzado"),Modifier.weight(1f)){n=it};HSelect("Equipo",gear,listOf("" to "Todos","body" to "Sin equipo","db" to "Mancuernas","bench" to "Banco","step" to "Plataforma","gym" to "Gimnasio","barbell" to "Barras","cable" to "Poleas","machine" to "Máquinas","smith" to "Smith","cardio" to "Cinta o ergómetro"),Modifier.weight(1f)){gear=it}};HCheck("Incluir variantes de técnica y ritmo",variants){variants=!variants};HCheck("Aplicar mis restricciones",restricted){restricted=!restricted};HText("${list.size} resultados",size=15,color=LocalHunter.current.muted)};items(list,key={it.id}){e->HPanel(onClick={ui.open("exercise",obj("id" to e.id))},padding=18){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){HunterIcon(Sigil.Exercise,Modifier.size(30.dp));StatusPill(e.category,Sigil.Layers)};Eyebrow("${e.group} · ${listOf("Inicial","Intermedio","Avanzado")[e.raw.i("level").coerceIn(0,2)]}");Heading(e.name,size=27);HText(if(e.equipment=="body")"Sin equipo especial" else if(e.gym)"Equipo de gimnasio" else "Mancuernas",size=15,color=LocalHunter.current.muted);HText("${if(e.impact)"Con saltos" else "Sin saltos"} · ${if(e.unit=="sec")"Por tiempo" else "Por repeticiones"}  ↗",size=15,color=LocalHunter.current.muted)}}}
}
@Composable fun ProgressScreen(ui:NativeUi){
 val s=ui.store;val stats=s.stats();val records=s.logs.entries().filter{it.first<=today()&&it.second is JSONObject}.sortedByDescending{it.first}
 SceneColumn(ui){
  Eyebrow("CADA AVANCE CUENTA");TopTabs(ui);Heading("Tu historia, en movimiento.");HPanel{Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){ProgressRing((stats.i("xp")%300)/300f,"NIVEL");Column{Eyebrow("RANGO ${rankKey(stats.i("level",1))}");Heading("Nivel ${stats.i("level",1)}",size=32);HText("${stats.i("xp")} XP registrados",size=16,color=LocalHunter.current.muted)}}}
  val strength=records.count{(_,v)->(v as JSONObject).b("complete")&&v.s("kind")=="strength"}
  val values=listOf("Días completados" to stats.i("days"),"Ejercicios registrados" to stats.i("exerciseCount"),"Series realizadas" to stats.i("series"),"Repeticiones" to stats.i("reps"),"Minutos registrados" to stats.i("seconds")/60,"Días de fuerza" to strength)
  for(pair in values.chunked(2))Row(horizontalArrangement=Arrangement.spacedBy(11.dp)){for((label,v)in pair)HPanel(Modifier.weight(1f),padding=17){Heading(v.toString(),size=35);HText(label,size=16,color=LocalHunter.current.muted)}}
  HPanel{Heading("Últimos 7 días · series realizadas",size=27)
   val days=(6L downTo 0L).map{plus(today(),-it)};val counts=days.map{d->s.logs.o(d).a("items").objects().sumOf{x->x.a("actual").objects().count{it.b("done")}}};val maximum=counts.maxOrNull()?.coerceAtLeast(1)?:1
   Row(Modifier.fillMaxWidth().height(120.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(8.dp)){for((i,date)in days.withIndex())Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Bottom){HText(counts[i].toString(),size=13);Box(Modifier.fillMaxWidth().height(max(3f,counts[i].toFloat()/maximum*70).dp).clip(RoundedCornerShape(topStart=6.dp,topEnd=6.dp)).background(Brush.verticalGradient(listOf(LocalHunter.current.secondary,LocalHunter.current.accent))));HText(LocalDate.parse(date).dayOfMonth.toString(),size=13)}}
  }
  val quick=records.count{(_,v)->val l=v as JSONObject;l.b("complete")&&l.a("items").objects().none{x->x.a("actual").objects().any{it.b("done")}}}
  HNotice("$quick misiones finalizadas por confirmación rápida. Sus cantidades no registradas no se suman a las estadísticas.")
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Heading("Diario de misiones",size=31);HText("${stats.i("xp")} XP",color=LocalHunter.current.accent)}
  if(records.isEmpty())HPanel{Heading("Tu historia empieza con la primera serie.",size=29);HText("Abre la misión de hoy para comenzar.",size=17,color=LocalHunter.current.muted)}
  for((date,v)in records.take(60)){val l=v as JSONObject;HPanel(onClick={ui.open("history",obj("date" to date))},padding=17){Eyebrow(date);Heading(kindLabel(l.s("kind")),size=26);HText("${if(l.b("complete"))"Completado" else if(l.b("pain"))"Detenido por molestias" else "En curso"} · RPE ${l.i("rpe")} · ${(s.fraction(l)*100).toInt()}%",size=16);l.s("notes").takeIf{it.isNotBlank()}?.let{HText(it,size=15,color=LocalHunter.current.muted)}}}
  HText("Los minutos suman únicamente los ejercicios medidos en segundos; no representan la duración total de la sesión. Un ejercicio registrado tiene al menos una serie marcada.",size=14,color=LocalHunter.current.muted)
 }
}
