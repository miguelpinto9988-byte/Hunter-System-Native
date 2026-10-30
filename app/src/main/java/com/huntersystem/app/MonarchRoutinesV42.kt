package com.huntersystem.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Fase 06 V42.5. Nueva interfaz, motor Planner/Rules original y catálogo local. */
private val routineGroupColors = mapOf(
    "upper" to Color(0xFF448CFF),
    "lower" to Color(0xFFEE82B1),
    "core" to Color(0xFFA784FE),
    "mobility" to Color(0xFF47D3AD),
    "cardio" to Color(0xFFFFBD61)
)
private val routineLevels = listOf("0" to "Básico","1" to "Intermedio","2" to "Avanzado")
private val routineDurations = listOf("" to "Todas","short" to "5–10 min","medium" to "10–20 min","long" to "21–30 min", "extra" to "Más de 30")
private val routineCategories = listOf("" to "Todas","Fuerza" to "Fuerza","Cardio" to "Cardio","Movilidad" to "Movilidad","Mixta" to "Mixta")
private val routineImpact = listOf("all" to "Todos","low" to "Sin saltos","jumps" to "Con saltos")
private val routineEquipment = listOf("all" to "Cualquiera","body" to "Sin equipo","db" to "Mancuernas","gym" to "Gimnasio")
private fun durationOk(mins:Int, selected:String):Boolean = when(selected) {
    "short" -> mins in 5..10
    "medium" -> mins in 11..20
    "long" -> mins in 21..30
    "extra" -> mins>30
    else -> true
}

/** No cambia las restricciones de seguridad; solo reduce la selección visible. */
private fun equipmentOk(r:JSONObject,s:NativeStore,filter:String):Boolean {
    val ex=r.a("items").objects().mapNotNull{s.catalog.byId[it.s("id")]}
    if(ex.isEmpty())return false
    return when(filter){
        "body" -> ex.all{it.equipment=="body"&&!it.gym}
        "db" -> ex.all{it.equipment=="body"||it.equipment=="db"}&&ex.none{it.gym}
        "gym" -> ex.any{it.gym||it.equipment=="gym"}
        else -> true
    }
}
private fun impactOk(r:JSONObject,s:NativeStore,filter:String):Boolean{
    val xs=r.a("items").objects().mapNotNull{s.catalog.byId[it.s("id")]}
    val impact=xs.any{it.impact||it.power}
    return when(filter){"low"->!impact;"jumps"->impact;else->true}
}
private fun routineReasons(s:NativeStore,r:JSONObject,p:JSONObject):List<String>{
    val gate=s.rules.gate(p)
    if(!gate.ok)return listOf(gate.reason)
    val rs=mutableListOf<String>()
    for(row in r.a("items").objects()){
        val e=s.catalog.byId[row.s("id")]
        if(e==null)rs+="Ejercicio no encontrado en catálogo"
        else rs+=s.rules.reasons(e,p).take(2)
    }
    return rs.distinct()
}
private fun routineMinutes(s:NativeStore,r:JSONObject,p:JSONObject)=
    r.i("minutes",s.planner.minutes(r,p)).coerceAtLeast(0)

/** Panel principal; incluye los canales originales y el editor existente. */
@Composable fun MonarchRoutinesScreen(ui:NativeUi){
    val s=ui.store
    if(ui.library.isNotBlank()){
        MonarchRoutinesLibrary(ui,ui.library)
        return
    }
    val normal=s.catalog.routines.count{!it.b("gymOnly23")}
    val gym=s.catalog.routines.count{it.b("gymOnly23")}
    SceneColumn(ui){
        TopTabs(ui)
        Eyebrow("SISTEMA MONARCA  /  ENTRENAMIENTO")
        Heading("Centro de rutinas",size=36)
        HunterPngIcon("routines",Modifier.size(76.dp),"Biblioteca de rutinas")
        HText("Elige una secuencia de la biblioteca o genera una sesión adaptada a tu perfil, totalmente sin conexión.",
            size=16,color=LocalHunter.current.muted)
        TrainingMode(ui)
        MonarchRoutineModeCard(
            title="Biblioteca Hunter",subtitle="$normal rutinas originales · sin conexión",
            tag="01  /  ENTRENAR SIN EQUIPO DE GIMNASIO",
            count="$normal",color=routineGroupColors.getValue("upper")
        ){ui.library="offline"}
        MonarchRoutineModeCard(
            title="Modo gimnasio",subtitle="$gym rutinas especializadas · equipamiento configurable",
            tag="02  /  ENTRENAMIENTO CON MÁQUINAS Y PESAS",
            count="$gym",color=routineGroupColors.getValue("core")
        ){ui.library="gym"}
        HPanel(padding=18){
            Eyebrow("03  /  TU RUTINA")
            Heading("Crea tu secuencia",size=25)
            HText("Configura ejercicios, duración y descansos desde el editor de rutinas original.",
                size=15,color=LocalHunter.current.muted)
            HButton("ABRIR EDITOR DE RUTINAS  ↗",Modifier.fillMaxWidth()){
                ui.open("routine-editor")
            }
        }
        Heading("Entrenadores de referencia",size=27)
        for((idx,ch)in s.catalog.editorial.a("channels").objects().withIndex()){
            HPanel(onClick={ui.open("creator",obj("key" to ch.s("key")))},padding=16) {
                Row(verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    HText((idx+1).toString().padStart(2,'0'),size=23,
                        color=LocalHunter.current.accent,bold=true)
                    Column(Modifier.weight(1f)){
                        HText(ch.s("name"),size=19,bold=true)
                        HText("Material de referencia y enlaces originales  ↗",
                            size=14,color=LocalHunter.current.muted)
                    }
                }
            }
        }
        HNotice("Las secuencias generadas aquí son diseños originales de Hunter System. No se reproducen automáticamente rutinas completas de terceros.")
        HButton("FUENTES Y PUBLICACIONES  ↗"){ui.open("sources")}
    }
}
@Composable
private fun MonarchRoutineModeCard(title:String,subtitle:String,tag:String,count:String,
    color:Color,onClick:()->Unit){
    val c=LocalHunter.current
    val shape=RoundedCornerShape(23.dp)
    Box(Modifier.fillMaxWidth().clip(shape)
        .background(Brush.linearGradient(listOf(c.surface,c.panel,c.input)))
        .monarchOutline(shape)
        .clickable(role=Role.Button,onClick=onClick)
        .semantics { contentDescription="$title, $subtitle. Abrir biblioteca." }
        .padding(21.dp)){
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            HunterPngIcon(hunterRoutinePngKey(title),Modifier.size(74.dp),title)
            HText(tag,size=12,color=color,bold=true)
            Heading(title,size=29)
            HText(subtitle,size=15,color=c.muted)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically){
                HText("EXPLORAR SESIONES",size=14,color=c.accent,bold=true)
                HText("→",size=28,color=color,bold=true)
            }
        }
    }
}
@Composable fun MonarchRoutinesLibrary(ui:NativeUi,mode:String){
    val s=ui.store
    val c=LocalHunter.current
    val revision=s.revision.intValue
    var n by rememberSaveable(mode){mutableIntStateOf(level(s.profile?:JSONObject()))}
    var category by rememberSaveable(mode){mutableStateOf("")}
    var duration by rememberSaveable(mode){mutableStateOf("")}
    var equipment by rememberSaveable(mode){mutableStateOf("all")}
    var impact by rememberSaveable(mode){mutableStateOf("all")}
    var query by rememberSaveable(mode){mutableStateOf("")}
    var safeOnly by rememberSaveable(mode){mutableStateOf(false)}
    val p=remember(revision){s.adjusted()}
    val generated=ui.generated[mode]
    val base=remember(generated,revision,mode){
        generated ?: (s.catalog.routines+s.state.a("customRoutines").objects())
            .filter{it.b("gymOnly23")== (mode=="gym")}
    }
    val list=remember(base,revision,n,category,duration,equipment,impact,query,safeOnly){
        base.filter{r->
            r.i("level")==n &&
            (category.isBlank()||r.s("category")==category) &&
            (query.isBlank()||normalize(r.s("name")).contains(normalize(query))) &&
            durationOk(routineMinutes(s,r,p),duration) &&
            equipmentOk(r,s,equipment) && impactOk(r,s,impact) &&
            (!safeOnly||routineReasons(s,r,p).isEmpty())
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),
        verticalArrangement=Arrangement.spacedBy(15.dp),
        contentPadding=PaddingValues(top=18.dp,bottom=35.dp)){
        item{Header(ui)}
        item{
            HButton("‹ VOLVER AL CENTRO",Modifier.fillMaxWidth()){ui.library=""}
            Spacer(Modifier.height(14.dp))
            Eyebrow(if(mode=="gym")"MONARCA / GIMNASIO" else "MONARCA / BIBLIOTECA")
            Heading(if(mode=="gym")"Centro de gimnasio" else "Biblioteca de rutinas",size=34)
            HText("Rutinas consultables sin internet. El motor comprueba tu perfil antes de asignarlas.",
                size=15,color=c.muted)
        }
        item{MonarchRoutineGenerator(ui,mode){selected->n=selected}}
        item{
            HPanel(padding=16){
                Eyebrow("FILTROS DE RUTINAS")
                Heading("Encuentra tu sesión",size=26)
                HInput(query,{query=it},"Buscar por nombre")
                Eyebrow("NIVEL DE ENTRENAMIENTO")
                Chips(routineLevels,n.toString()){n=it.toInt()}
                Eyebrow("CATEGORÍA")
                Chips(routineCategories,category){category=it}
                Eyebrow("DURACIÓN ESTIMADA")
                Chips(routineDurations,duration){duration=it}
                Eyebrow("EQUIPO DE LA SECUENCIA")
                Chips(routineEquipment,equipment){equipment=it}
                Eyebrow("IMPACTO Y POTENCIA")
                Chips(routineImpact,impact){impact=it}
                HCheck("Mostrar solo rutinas compatibles con mi perfil",
                    safeOnly){safeOnly=!safeOnly}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    HText("${list.size} rutinas encontradas",size=16,
                        color=c.accent,bold=true)
                    HText(if(generated==null)"Biblioteca original" else "Generación nueva",
                        size=12,color=c.muted)
                }
                if(category.isNotBlank()||duration.isNotBlank()||equipment!="all"||
                    impact!="all"||query.isNotBlank()||safeOnly){
                    HButton("LIMPIAR FILTROS",Modifier.fillMaxWidth()){
                        category="";duration="";equipment="all";impact="all";query="";safeOnly=false
                    }
                }
            }
        }
        if(generated!=null)item {
            HButton("RESTAURAR BIBLIOTECA ORIGINAL",Modifier.fillMaxWidth()){
                ui.generated.remove(mode)
            }
        }
        if(list.isEmpty())item {
            HNotice("No hay resultados con los filtros seleccionados. Prueba otra duración, equipamiento o nivel. Si tienes lesiones o limitaciones, revisa las indicaciones de Perfil.")
        }
        items(list,key={it.s("id")}){r->
            MonarchRoutineCard(ui,r,p)
        }
    }
}
@Composable
fun MonarchRoutineGenerator(ui:NativeUi,mode:String,onGenerated:(Int)->Unit){
    val s=ui.store
    val scope=rememberCoroutineScope()
    val c=LocalHunter.current
    val selected=remember(mode){mutableStateListOf<String>()}
    var range by rememberSaveable(mode){mutableStateOf("medium")}
    var levels by rememberSaveable(mode){mutableStateOf("all")}
    var noImpact by rememberSaveable(mode){mutableStateOf(false)}
    var noGear by rememberSaveable(mode){mutableStateOf(false)}
    var busy by remember{mutableStateOf(false)}
    var info by remember(mode){mutableStateOf("")}
    HPanel(padding=17){
        Eyebrow("GENERADOR MONARCA  /  100 % LOCAL")
        Heading("Diseña tu desafío",size=29)
        HText("Selección múltiple de grupos, duración y niveles. Se generan secuencias sin guardar combinaciones precargadas.",
            size=15,color=c.muted)
        Eyebrow("01  ·  GRUPOS DE TRABAJO")
        groupLabels.entries.toList().chunked(2).forEach { pair->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                pair.forEach {entry->
                    val tint=routineGroupColors[entry.key]?:c.accent
                    val active=entry.key in selected
                    val shape=RoundedCornerShape(12.dp)
                    Box(Modifier.weight(1f).defaultMinSize(minHeight=58.dp)
                        .clip(shape)
                        .background(if(active)tint.copy(alpha=.19f) else c.input)
                        .monarchOutline(shape,active=active)
                        .clickable(role=Role.Checkbox){
                            if(!selected.remove(entry.key))selected.add(entry.key)
                        }
                        .semantics{contentDescription="${entry.value}, ${if(active)"seleccionado" else "sin seleccionar"}"}
                        .padding(horizontal=9.dp,vertical=9.dp),
                        contentAlignment=Alignment.CenterStart){
                        HText((if(active)"✓  " else "◇  ")+entry.value,
                            size=14,color=if(active)tint else c.text,bold=active)
                    }
                }
                if(pair.size==1)Spacer(Modifier.weight(1f))
            }
        }
        Eyebrow("02  ·  TIEMPO OBJETIVO")
        Chips(listOf("short" to "5–10 min","medium" to "10–20 min",
            "long" to "Hasta 30 min"),range){range=it}
        Eyebrow("03  ·  INTENSIDAD / NIVEL")
        Chips(listOf("all" to "Tres niveles")+routineLevels,levels){levels=it}
        Eyebrow("04  ·  CONTROL DEL EQUIPAMIENTO E IMPACTO")
        HCheck("Limitar a ejercicios sin saltos ni potencia",noImpact){noImpact=!noImpact}
        if(mode=="offline"){
            HCheck("Generar únicamente ejercicios sin equipamiento",noGear){noGear=!noGear}
        } else {
            HText("Modo gimnasio: solo usa el material declarado en Perfil.",
                size=14,color=c.muted)
        }
        HButton(if(busy)"GENERANDO…" else "✦  GENERAR NUEVAS RUTINAS",
            Modifier.fillMaxWidth(),primary=true,enabled=!busy) {
            val gate=s.rules.gate(s.adjusted())
            when {
                selected.isEmpty()->info="Selecciona al menos un grupo muscular."
                !gate.ok->info=gate.reason
                mode=="gym"&&s.profile?.s("equipment")!="gym" ->
                    info="Configura el equipamiento de gimnasio en Perfil para continuar."
                else -> {
                    val p=s.adjusted().copyJson()
                    // Nunca quitar restricciones existentes: solo añadir límites para esta generación.
                    if(noImpact){
                        val restrictions=p.o("restrictions").copyJson().put("noImpact",true)
                        p.put("restrictions",restrictions)
                    }
                    if(noGear && mode=="offline"){
                        p.put("equipment","body")
                        p.put("bench",false)
                        p.put("step",false)
                        p.put("gymEquipment23",arr(emptyList<String>()))
                        p.put("materials46",arr(emptyList<String>()))
                    }
                    val groups=selected.toList()
                    val chosenLevels=if(levels=="all")(0..2).toList()
                        else listOf(levels.toInt())
                    val requestedRevision=s.revision.intValue
                    val logs=s.logs.copyJson()
                    val avoidImpact=noImpact
                    val onlyBody=noGear
                    val epoch=++ui.serial
                    val selectedDuration=range
                    busy=true
                    info="Construyendo sesiones compatibles con tu perfil…"
                    scope.launch {
                        try {
                            val result=withContext(Dispatchers.Default){
                                chosenLevels.flatMap{n->
                                    groups.flatMap{type->
                                        (0..1).mapNotNull{variant->
                                            s.planner.generateRoutine(mode,type,n,variant,
                                                selectedDuration,epoch,p,logs)
                                        }
                                    }
                                }.filter{r->
                                    r.a("items").objects().all{x->
                                        s.catalog.byId[x.s("id")]?.let{e->
                                            s.rules.compatible(e,p)&&
                                            (!avoidImpact||!e.impact&&!e.power)&&
                                            (!onlyBody||mode!="offline"||e.equipment=="body"&&!e.gym)
                                        }==true
                                    }
                                }
                            }
                            if(s.revision.intValue!=requestedRevision){
                                info="El perfil cambió mientras se generaban las rutinas. Vuelve a generar para usar la configuración actual."
                            }else if(result.isEmpty()){
                                info="No hay combinaciones compatibles para el nivel, material y duración solicitados. Prueba otros filtros o revisa Perfil."
                            }else{
                                ui.generated[mode]=result
                                onGenerated(chosenLevels.first())
                                info="${result.size} rutinas generadas. Encontrarás las tarjetas debajo de los filtros."
                            }
                        }catch(e:Exception){
                            info=e.message?:"No fue posible generar la sesión."
                        }finally{busy=false}
                    }
                }
            }
        }
        if(info.isNotBlank())HNotice(info)
        if(ui.generated[mode]!=null)HButton("REINICIAR GENERACIÓN",Modifier.fillMaxWidth()){
            ui.generated.remove(mode)
            info="Biblioteca restaurada."
        }
        HText("La intensidad se adapta al nivel, historial y restricciones del perfil. Esta función no modifica las misiones diarias.",
            size=13,color=c.muted)
    }
}
@Composable
fun MonarchRoutineCard(ui:NativeUi,r:JSONObject,p:JSONObject){
    val s=ui.store
    val c=LocalHunter.current
    val reasons=remember(r,s.revision.intValue){routineReasons(s,r,p)}
    val valid=reasons.isEmpty()
    val rows=r.a("items").objects()
    val minutes=routineMinutes(s,r,p)
    val tag=r.s("focus3845")
    val tint=routineGroupColors[tag]?:when(r.s("category")){
        "Cardio"->routineGroupColors.getValue("cardio")
        "Movilidad"->routineGroupColors.getValue("mobility")
        else->c.accent
    }
    HPanel(padding=18){
        HunterPngIcon(hunterRoutinePngKey(r.s("name"),r.s("category")),
            Modifier.size(64.dp),"Tipo de rutina")
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically){
            HText(r.s("category","RUTINA").uppercase(),size=12,color=tint,bold=true)
            HText(if(valid)"✓ COMPATIBLE" else "REVISAR PERFIL",
                size=12,color=if(valid)routineGroupColors.getValue("mobility") else
                    routineGroupColors.getValue("lower"),bold=true)
        }
        Heading(r.s("name","Secuencia de entrenamiento"),size=25)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
            HText("◷ $minutes min",size=15,color=c.muted)
            HText("◈ ${rows.size} ejercicios",size=15,color=c.muted)
            HText("${rows.sumOf{it.i("sets")}} series",size=15,color=c.muted)
        }
        rows.take(4).forEachIndexed{index,row->
            val e=s.catalog.byId[row.s("id")]
            HText("${index+1}. ${e?.name?:row.s("id")}",size=15,maxLines=2)
        }
        if(rows.size>4)HText("+ ${rows.size-4} ejercicios adicionales",
            size=14,color=c.muted)
        if(!valid){
            HText("No asignable: "+reasons.take(2).joinToString("; "),
                size=14,color=c.muted)
        }
        HButton("VER SECUENCIA Y EJERCICIOS  ↗",Modifier.fillMaxWidth(),
            primary=valid){
            ui.open("routine",obj("id" to r.s("id")))
        }
    }
}
