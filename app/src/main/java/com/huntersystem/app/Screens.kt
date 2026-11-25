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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
 var tab by mutableStateOf("home")
 var profileEditorOpen by mutableStateOf(store.profile==null)
 var top by mutableStateOf("home")
 var library by mutableStateOf("")
 val stack=mutableStateListOf<Overlay>()
 val generated=mutableStateMapOf<String,List<JSONObject>>()
 var serial=0
 var pendingAvatar by mutableStateOf<String?>(null)
 var message:(String)->Unit={}
 var export:()->Unit={}
 var import:()->Unit={}
 var chooseAvatar:()->Unit={}
 var openUrl:(String)->Unit={}
 var requestNotifications:()->Unit={}
 fun openProfile(){
  profileEditorOpen=true
  library=""
  stack.clear()
 }
 fun closeProfile(){
  if(store.profile==null)return // El usuario debe completar la creación inicial.
  profileEditorOpen=false
  tab="home"
  top="home"
  library=""
  pendingAvatar=null
  stack.clear()
 }
 fun profileSaved(){
  check(store.profile!=null){"Guarda tu perfil antes de entrar al Panel"}
  closeProfile()
 }
 fun go(t:String){
  if(t=="profile" || store.profile==null){
   openProfile()
   return
  }
  profileEditorOpen=false
  tab=t
  library=""
  stack.clear()
 }
 // La antigua ventana de rangos ya no está disponible.
 fun open(type:String,data:JSONObject=JSONObject()){
  if(type=="evolution")return
  stack+=Overlay(type,data)
 }
 fun back(){
  when{
   stack.isNotEmpty()->stack.removeAt(stack.lastIndex)
   profileEditorOpen->closeProfile()
   library.isNotBlank()->library=""
   tab!="home"->tab="home"
  }
 }
 fun attempt(block:()->Unit){
  try{block()}catch(e:Exception){message(e.message?:"No se pudo completar la acción")}
 }
 fun routine(id:String)=generated.values.flatten().firstOrNull{it.s("id")==id}
  ?:store.state.a("customRoutines").objects().firstOrNull{it.s("id")==id}
  ?:store.catalog.routines.firstOrNull{it.s("id")==id}
}

/** Perfil separado del host de navegación, visible tras el splash solo si falta. */
internal fun mustShowStandaloneProfile(profileExists:Boolean,editorRequested:Boolean,readError:Boolean):Boolean =
 !readError && (!profileExists || editorRequested)

fun rankKey(level:Int)=listOf("E","D","C","B","A","S")[((level-1)/5).coerceIn(0,5)]
@Composable fun Header(ui:NativeUi){
 val s=ui.store
 val rank=rankKey(s.stats().i("level",1))
 val refresh=LocalMonarchPanelRefresh.current
 Row(Modifier.fillMaxWidth().padding(bottom=if(refresh)4.dp else 12.dp),
     verticalAlignment=Alignment.CenterVertically,
     horizontalArrangement=Arrangement.spacedBy(8.dp)){
  Column(Modifier.weight(1f)){
   Heading("HUNTER SYSTEM",size=if(refresh)25 else 28)
   Eyebrow("SISTEMA DE SOMBRAS")
  }
  HText(rank,size=22,bold=true,color=LocalHunter.current.accent)
  Avatar(s.state.s("avatar"),s.profile?.s("name")?:"C",
    Modifier.size(if(refresh)48.dp else 36.dp).clickable{ui.go("profile")}.semantics{
      contentDescription="Abrir perfil"
    })
 }
}
@Composable fun SceneColumn(ui:NativeUi,showHeader:Boolean=true,content:@Composable ColumnScope.()->Unit){
 val scroll=rememberSaveable(ui.tab,ui.library,saver=ScrollState.Saver){ScrollState(0)}
 Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter){
  Column(Modifier.widthIn(max=960.dp).fillMaxWidth().fillMaxHeight()
    .verticalScroll(scroll).padding(horizontal=18.dp,vertical=20.dp),
    verticalArrangement=Arrangement.spacedBy(if(LocalMonarchPanelRefresh.current)16.dp else 20.dp)){
    if(showHeader)Header(ui)
    content()
    Spacer(Modifier.height(25.dp))
  }
 }
}
@Composable fun TopTabs(ui:NativeUi){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){for((id,title)in listOf("home" to "Panel","plan" to "Misiones","objectives" to "Objetivos"))HChip(title,ui.tab==id,Modifier.weight(1f)){ui.go(id)}}}
@Composable
fun HomeScreen(ui:NativeUi) {
 val stats=ui.store.stats()
 SceneColumn(ui) {
  MonarchDashboardWelcome(ui)
  // La misión especial ocupa el antiguo espacio del portal Eclipse.
  MonarchSpecialMissionsPanel(ui)
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
 Box(Modifier.fillMaxWidth().height(420.dp)){Box(Modifier.fillMaxWidth().height(350.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(9.dp)).monarchOutline(RoundedCornerShape(9.dp))){AssetImage("ui33/scene-$rank-$scene.webp",Modifier.fillMaxSize(),ContentScale.Crop);Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(c.input,c.input.copy(alpha=.85f),c.input.copy(alpha=.25f)))))};AssetImage(art,Modifier.align(Alignment.TopEnd).offset(x=3.dp).fillMaxWidth(.58f).fillMaxHeight(),ContentScale.Crop,alignment=Alignment.TopCenter)
 Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(350.dp).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("SISTEMA EN LÍNEA");Eyebrow("RANGO ${rank.uppercase()}")};HText("${p.s("name","Cazador")} · ${s.catalog.editorial.o("goalPlans").o(p.s("finalTarget","general")).s("name","Tu siguiente nivel")}",Modifier.fillMaxWidth(.64f),size=14);Heading("SUPERA\nTUS LÍMITES.",size=46);HText("Una misión a la vez. Tu próximo nivel empieza hoy.",Modifier.fillMaxWidth(.60f),size=16,color=c.muted);Spacer(Modifier.weight(1f));Column(Modifier.fillMaxWidth(.65f),verticalArrangement=Arrangement.spacedBy(5.dp)){HText("NIVEL ${stats.i("level",1).toString().padStart(2,'0')}",bold=true,size=16);HText("RANGO ${rank.uppercase()} · ${s.catalog.design.a("ranks").objects().firstOrNull{it.s("key")==rank.uppercase()}?.s("name")?.uppercase()}",size=12);HProgress((stats.i("xp")%300)/300f);HText("${stats.i("xp")%300} / 300 XP",size=13,color=c.muted)}}
}}
@Composable fun MissionCompanion(ui:NativeUi){HPanel(padding=12,onClick={ui.open("coach")}){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){AssetImage("ui33/companion-${companionKey(ui.store.profile)}.webp",Modifier.width(67.dp).height(88.dp));Column{Eyebrow("IA DE MISIÓN");Heading(companionName(ui.store.profile),size=26);HText("La misma IA que elegiste en Perfil",size=14,color=LocalHunter.current.muted)}}}}
@Composable fun MissionLaunch(ui:NativeUi){val s=ui.store;val d=s.day(today());val l=s.logs.optJSONObject(today());val total=d.a("items").length()+d.a("tasks").length();HPanel{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("MISIÓN DIARIA DISPONIBLE");HText("${(s.fraction(l)*total).toInt()}/$total",size=16)};Heading(d.s("title"),size=33);HText("Una ventana. Tus objetivos. Cada casilla cuenta.",size=16,color=LocalHunter.current.muted);HButton("ABRIR VENTANA DE MISIONES →",Modifier.fillMaxWidth(),primary=true){ui.open("mission",obj("date" to today()))};HButton("✎ Personalizar la misión"){ui.open("custom",obj("date" to today()))}}}
@Composable fun AcademyTile(ui:NativeUi){HPanel(onClick={ui.open("academy")}){Eyebrow("EL CONOCIMIENTO TAMBIÉN SUBE DE NIVEL");Heading("Explorar la academia ↗",size=28);HText("Técnica, descanso y preparación en casa",size=16,color=LocalHunter.current.muted)}}
@Composable fun GoalsPanel(ui:NativeUi){val s=ui.store;HPanel{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Heading("Tus objetivos",size=29);HText("Ver todos ↗",Modifier.clickable{ui.go("objectives")},size=16,color=LocalHunter.current.accent)};val goals=s.state.a("goals").objects();if(goals.isEmpty())HText("Crea una meta y márcala cuando la hayas terminado.",size=16,color=LocalHunter.current.muted);for(g in goals.take(5))Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){HunterPngIcon(hunterGoalPngKey(g.s("title",g.s("text"))),Modifier.size(50.dp));Box(Modifier.weight(1f)){HCheck(g.s("title",g.s("text")),today() in g.a("dates").strings()){ui.attempt{s.toggleGoal(g.s("id"))}}}};HButton("+ Añadir objetivo"){ui.open("goal-edit")}}}
@Composable fun ObjectivesScreen(ui:NativeUi){val s=ui.store;SceneColumn(ui){TopTabs(ui);MonarchSectionHeading("OBJETIVOS PERSONALES","Cada meta cuenta.");GoalsPanel(ui);for(g in s.state.a("goals").objects())HPanel{HunterPngIcon(hunterGoalPngKey(g.s("title",g.s("text"))),Modifier.size(62.dp));Heading(g.s("title",g.s("text")),size=28);HText(if(g.s("repeat")=="daily")"Repetir cada día" else "Objetivo personal",size=15);HCheck("Completado hoy",today() in g.a("dates").strings()){s.toggleGoal(g.s("id"))};HButton("Editar objetivo"){ui.open("goal-edit",g)};HButton("Eliminar"){ui.open("delete-goal",g)}}}}
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
@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun ExercisesScreen(ui: NativeUi) {
    val store = ui.store
    var query by remember { mutableStateOf("") }
    var archiveMode by rememberSaveable { mutableStateOf(false) }
    val archive = remember(store.catalog,query,archiveMode) {
        if(archiveMode) store.catalog.historicalInventory.filter { normalize(query) in normalize(it.name) } else emptyList()
    }
    val activeIds = remember(store.catalog) { store.catalog.exercises.mapTo(hashSetOf()) { it.id } }
    var focus by rememberSaveable { mutableStateOf(CatalogFocus.DYNAMIC) }
    var category by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("") }
    var gear by remember { mutableStateOf("") }
    var restricted by remember { mutableStateOf(false) }
    val levels = listOf("Inicial", "Intermedio", "Avanzado")
    val families = remember(focus, category, store.catalog) {
        store.catalog.libraryExercises.mapNotNull { exercise ->
            store.catalog.classification(exercise.id)?.takeIf { focus.includes(exercise, it.category) }
        }
            .filter { category.isBlank() || it.category.code == category }
            .map { it.family }.distinct().sorted()
    }
    val all = "" to "Todos"
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val groups = remember(query, focus, category, group, difficulty, gear,
        restricted, store.revision.intValue) {
        val term = normalize(query)
        val profile = if (restricted && store.profile != null) store.adjusted() else null
        store.catalog.libraryExercises.filter { exercise ->
            val classification = store.catalog.classification(exercise.id)
            focus.includes(exercise, classification?.category) &&
                store.catalog.matchesExercise(exercise,term) &&
                (category.isBlank() || classification?.category?.code == category) &&
                (group.isBlank() || classification?.family == group) &&
                (difficulty.isBlank() || exercise.raw.i("level").toString() == difficulty) &&
                (!restricted || profile != null && classification != null && store.rules.compatible(exercise, profile)) &&
                when (gear) {
                    "" -> true
                    "body" -> exercise.equipmentKeys.isEmpty()
                    else -> gear in exercise.equipmentKeys
                }
        }.groupBy { it.oldFamily }.values.toList()
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)
        .semantics { testTagsAsResourceId = true }.testTag("exerciseCatalog"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 30.dp)) {
        item { Header(ui) }
        item { AcademyTile(ui) }
        item {
            Eyebrow("ARCHIVO DE ENTRENAMIENTO")
            Heading("Catálogo de ejercicios")
            Chips(listOf("active" to "Biblioteca actual", "archive" to "Catálogo 2.000"), if(archiveMode) "archive" else "active") { selected ->
                archiveMode = selected == "archive"
                query = ""
            }
            if(archiveMode) HText("Inventario histórico de consulta. Las fichas retiradas o pendientes no se incorporan al entrenamiento automático.", size = 14, color = LocalHunter.current.muted)
        }
        item {
            HInput(query, { query = it }, "Buscar ejercicio")
            Spacer(Modifier.height(12.dp))
            if(!archiveMode) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CatalogFocus.entries.toList().let { focuses ->
                    for (row in listOf(focuses.take(2)) + focuses.drop(2).chunked(3)) {
                        Chips(row.map { it.name to it.label }, focus.name) { selected ->
                            focus = CatalogFocus.valueOf(selected)
                            category = ""
                            group = ""
                        }
                    }
                }
            }
        }
        if(archiveMode) item { HText("${archive.size} de ${store.catalog.historicalInventory.size} fichas históricas", size = 15, color = LocalHunter.current.muted) }
        if(archiveMode) items(archive, key = { "archive-"+it.id }) { exercise ->
            HPanel(padding = 18) {
                Heading(exercise.name, size = 27)
                if(exercise.id !in activeIds) HText("ARCHIVO · NO INCLUIDO EN LA BIBLIOTECA ACTIVA", size = 13, color = LocalHunter.current.muted)
                HButton("Ver demostración", Modifier.fillMaxWidth().semantics {
                    contentDescription = "Abrir ejercicio: ${exercise.name}"
                }) { ui.open("exercise", obj("id" to exercise.id, "historicalInventory" to true)) }
            }
        }
        if(!archiveMode) item {
            HButton(if (showFilters) "Ocultar filtros" else "Mostrar filtros", Modifier.fillMaxWidth()) {
                showFilters = !showFilters
            }
            HText("${groups.size} ejercicios", size = 15, color = LocalHunter.current.muted)
        }
        if (!archiveMode && showFilters) item {
            if (focus != CatalogFocus.MOBILITY && focus != CatalogFocus.PREPARATION) {
                val categories = trainingCategoryLabels.filterKeys { it.code != "mobility" }
                Chips(listOf("" to "Todas") + categories.map { (key, name) -> key.code to name },
                    category) { category = it; group = "" }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                HSelect("Familia", group, listOf(all) + families.map { it to it },
                    Modifier.weight(1f)) { group = it }
                HSelect("Nivel", difficulty, listOf(all) + levels.mapIndexed { index, label ->
                    index.toString() to label
                }, Modifier.weight(1f)) { difficulty = it }
            }
            HSelect("Equipo", gear, listOf(all, "body" to "Sin equipo declarado") +
                store.catalog.originalEquipmentOptions, Modifier.fillMaxWidth()) { gear = it }
            HCheck("Aplicar mis restricciones", restricted) { restricted = !restricted }
        }
        if(!archiveMode) items(groups, key = { it.first().oldFamily }) { groupExercises ->
            val family = groupExercises.first().oldFamily
            val exercise = groupExercises.firstOrNull { it.isBaseVariant }
                ?: store.catalog.originalExercise(family) ?: groupExercises.first()
            val variants = groupExercises.filter { it.id != exercise.id }
            val showBase = groupExercises.any { it.id == exercise.id }
            var showVariants by rememberSaveable(family) { mutableStateOf(false) }
            HPanel(padding = 18) {
                val levelLabel = levels[exercise.raw.i("level").coerceIn(0, 2)]
                val classification = store.catalog.classification(exercise.id)
                Eyebrow(listOf(exercise.sport, trainingCategoryLabels[classification?.category].orEmpty(),
                    classification?.family.orEmpty(), levelLabel)
                    .filter { it.isNotBlank() }.joinToString(" · "))
                Heading(exercise.name, size = 27)
                if (showBase) HButton("Ver demostración", Modifier.fillMaxWidth().semantics {
                    contentDescription = "Abrir ejercicio: ${exercise.name}"
                }) { ui.open("exercise", obj("id" to exercise.id, "originalCatalog" to true)) }
                if (exercise.validationBlocked)
                    HText("PENDIENTE DE VALIDACIÓN", size = 13, color = LocalHunter.current.accent, bold = true)
                val equipment = exercise.equipmentKeys.filter { it != "gym" || exercise.equipmentKeys.size == 1 }
                    .map(store.catalog::equipmentLabel).joinToString(" · ").ifBlank { "Sin equipo declarado" }
                HText(equipment, size = 15, color = LocalHunter.current.muted)
                val intensityTags = listOfNotNull("Potencia".takeIf { exercise.isExplosiveMovement },
                    "Impacto".takeIf { exercise.impact })
                val unit = if (exercise.unit == "sec") "Por tiempo" else "Por repeticiones"
                HText((intensityTags + unit).joinToString(" · "), size = 15, color = LocalHunter.current.muted)
                if (showBase && variants.isNotEmpty()) {
                    HButton(if (showVariants) "Ocultar variantes" else "Ver variantes (${variants.size})",
                        Modifier.fillMaxWidth()) { showVariants = !showVariants }
                }
                if (showVariants || !showBase) for (variant in variants) {
                    HButton(variant.name, Modifier.fillMaxWidth()) {
                        ui.open("exercise", obj("id" to variant.id, "originalCatalog" to true))
                    }
                }
            }
        }
    }
}
@Composable fun ProgressScreen(ui:NativeUi){ MonarchProgressScreen(ui) }
