package com.huntersystem.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.time.LocalDate

private val stages = listOf("Identidad del cazador", "Entrenamiento", "Personalización", "Activación del sistema")
private val stageArt = listOf("identity", "training", "appearance", "activation")
private val genders = listOf("male" to "Masculino", "female" to "Femenino", "unspecified" to "No especificado")
private val levels = listOf("basic" to "Básico", "intermediate" to "Intermedio", "advanced" to "Avanzado")
private val objectives = listOf("general" to "Mejorar mi bienestar general", "strength" to "Desarrollar fuerza funcional",
 "endurance" to "Mejorar mi resistencia", "mobility" to "Mejorar mi movilidad", "habit" to "Crear un hábito de actividad física")
private val durations = listOf(10,15,20,30,45,60).map { it.toString() to "$it minutos" }
private val rests = listOf(30,60,90).map { it.toString() to "$it segundos" }
private val formats = listOf("rounds" to "Por rondas", "exercise" to "Por series")
private val themes = listOf("light" to "Blanco", "dark" to "Negro")
private val edges = listOf("ocean" to "Azul", "purple" to "Morado", "pink" to "Rosa",
 "green" to "Verde", "red" to "Rojo", "white" to "Blanco")
private val silhouettes = listOf("hunter-male", "hunter-female", "reader", "regressor", "reader-female")
private val scenes = listOf("templo-del-ocaso" to "Ruinas arcanas", "biblioteca-lunar" to "Niebla violeta",
 "trono-volcanico" to "Obsidiana", "reino-de-las-nubes" to "Aurora dorada", "jardin-sagrado" to "Bosque ancestral")
private val materials = listOf("mat" to "Colchoneta o superficie acolchada estable",
 "wall" to "Pared o apoyo vertical firme", "chair" to "Silla estable")
private val gymOptions = listOf("cable" to "Poleas", "machine" to "Máquinas guiadas", "bench" to "Banco",
 "barbell" to "Barras", "bike" to "Bicicleta estática", "rower" to "Máquina de remo", "elliptical" to "Elíptica")
private val dayNames = listOf("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo")
private val categoryLabels = listOf("Tren superior","Tren central","Tren inferior","Movilidad","Cardio","Descanso")
private val categoryIcons = listOf("↑","◆","↓","≈","♥","☾")
private val categoryColors = listOf(0xFF258FE6,0xFF9A68F2,0xFF34BC91,0xFF168887,0xFFD8732D,0xFF62738B)

@Composable
fun ProfileScreen(ui:NativeUi) {
 val store=ui.store
 val original=remember { store.profile?.copyJson() }
 val originalAppearance=remember { store.state.o("appearance33").copyJson() }
 val draft=remember {
  mutableStateMapOf(
   "name" to original?.s("name").orEmpty(),"age" to original?.i("age")?.toString().orEmpty(),
   "height" to original?.s("height").orEmpty(),"weight" to original?.s("weight").orEmpty(),
   "gender" to (original?.s("gender","unspecified")?:"unspecified"),
   "difficulty" to (original?.s("difficulty","basic")?:"basic"),
   "objective" to (original?.s("profileObjective",original.s("goal","general"))?:"general"),
   "finalGoal" to original?.s("custom_goal",original.s("finalGoal")).orEmpty(),
   "trainingMode" to (original?.s("trainingMode","challenges")?:"challenges"),
   "dailyMissions" to (original?.i("dailyMissions",3)?:3).toString(),
   "minutes" to (original?.i("minutes",30)?:30).toString(),
   "restSeconds" to (original?.i("restSeconds",60)?:60).toString(),
   "blockMode22" to (original?.s("blockMode22","rounds")?:"rounds"),
   "dumbbellWeights" to (original?.a("dumbbellWeightsKg")?.strings()?.joinToString(", ")
    ?.takeIf { it.isNotBlank() } ?: original?.n("load")?.takeIf { it>0 }?.toString().orEmpty()),
   "theme" to (original?.let { HunterAppearanceV43.mode(it.s("theme")) } ?: "light"),
   "outlineColor" to (original?.let { HunterAppearanceV43.outline(it.s("outlineColor",it.s("theme"))) } ?: "ocean"),
   "companionCharacter" to (original?.s("companionCharacter","knight")?:"knight"),
   "collection" to originalAppearance.s("collection","hunter-male"),
   "scene" to originalAppearance.s("scene","templo-del-ocaso")
  )
 }
 val initial=remember { draft.toMap() }
 val healthValues=remember {
  mutableStateMapOf<String,String>().apply {
   original?.o("health")?.entries()?.forEach { (key,value)->put(key,value.toString()) }
   original?.o("restrictions")?.entries()?.forEach { (key,value)->put("r_$key",value.toString()) }
  }
 }
 val initialHealth=remember { healthValues.toMap() }
 val healthExpanded=remember { mutableStateMapOf<String,Boolean>() }
 val injuries=remember { mutableStateListOf<JSONObject>().apply { addAll(original?.a("injuries")?.objects().orEmpty()) } }
 var injuryZone by remember { mutableStateOf("knee") }
 var injuryType by remember { mutableStateOf("joint") }
 var injuryStage by remember { mutableStateOf("current") }
 val week=remember { mutableStateListOf<Int>().apply {
  addAll(original?.let { MonarchProfileAdapter.from(it,LocalDate.now())?.weekly }?:List(7){5})
 } }
 val gear=remember { mutableStateMapOf<String,Boolean>().apply {
  put("body",original?.b("bodyAvailable",true)?:true)
  put("db",original?.b("dumbbellsAvailable",original.s("equipment") in listOf("db","gym"))?:false)
  put("gym",original?.s("equipment")=="gym")
  materials.forEach { (id,_) -> put(id,id in original?.a("materials46")?.strings().orEmpty()) }
  gymOptions.forEach { (id,_) -> put("gym_$id",id in original?.a("gymEquipment23")?.strings().orEmpty()) }
 } }
 val initialGear=remember { gear.toMap() }
 val initialWeek=remember { week.toList() }
 var stage by remember { mutableIntStateOf(0) }
 var confirmed by remember { mutableStateOf(false) }
 var summary by remember { mutableIntStateOf(-1) }
 var openSelector by remember { mutableStateOf("") }
 fun value(key:String)=draft.getValue(key)
 fun move(target:Int){stage=target;openSelector="";summary=-1;confirmed=false}
 fun validateIdentity(){
  require(value("name").isNotBlank()){"Escribe tu nombre de cazador"}
  require(value("age").toIntOrNull()?.let { it>=0 }==true){"Escribe una edad válida en años"}
  for (key in listOf("height","weight")) if(value(key).isNotBlank()){
   require(value(key).replace(',','.').toDoubleOrNull()?.let { it.isFinite()&&it>0 }==true){
    if(key=="height")"Revisa la estatura" else "Revisa el peso"
   }
  }
 }
 fun weights()=if(gear["db"]==true)parseDumbbellWeights(value("dumbbellWeights")) else emptyList()
 fun save(){
  validateIdentity()
  val appearance=originalAppearance.copyJson().put("collection",value("collection")).put("scene",value("scene"))
  val artworkKeys=setOf("theme","outlineColor","collection","scene","companionCharacter")
  val appearanceOnly=original!=null&&initialWeek==week&&initialGear==gear&&initialHealth==healthValues&&
   arr(injuries).toString()==original.a("injuries").toString()&&draft.all { (key,v)->key in artworkKeys||initial[key]==v }
  if(appearanceOnly){
   store.updateAppearanceOnly(value("theme"),appearance,ui.pendingAvatar,value("outlineColor"),value("companionCharacter"))
   ui.profileSaved()
   return
  }
  val p=original?.copyJson()?:JSONObject()
  val health=p.o("health").copyJson()
  val restrictions=p.o("restrictions").copyJson()
  for((key,v) in healthValues)if(key.startsWith("r_"))restrictions.put(key.removePrefix("r_"),v=="true")
   else if(key in setOf("pain","recommendations"))health.put(key,v) else health.put(key,v=="true")
  p.put("health",health).put("restrictions",restrictions).put("injuries",arr(injuries))
  if(arr(injuries).toString()!=original?.a("injuries")?.toString())p.put("limits",injuries.any {it.s("stage")!="past"})

  val equipment=if(gear["gym"]==true)"gym" else if(gear["db"]==true)"db" else "body"
  val selectedWeights=weights()
  for(key in listOf("name","gender","difficulty","finalGoal","trainingMode","blockMode22","theme","outlineColor","companionCharacter")){
   p.put(key,value(key).let { if(key=="name")it.trim() else it })
  }
  p.put("age",value("age").toInt())
  for(key in listOf("height","weight")){
   val n=value(key).replace(',','.').toDoubleOrNull()
   if(n==null)p.remove(key) else p.put(key,n)
  }
  val objective=value("objective")
  val priorObjective=original?.s("profileObjective",original.s("goal","general"))
  if(original==null||priorObjective!=objective){
   val target=if(objective=="habit")"general" else objective
   p.put("goal",target).put("finalTarget",target)
   p.put("goalGroup",when(objective){"strength"->"build";"endurance"->"fitness";"mobility"->"movement";else->"habit"})
  }
  p.put("profileObjective",objective).put("manualGoalGroupNative",true)
  for(key in listOf("minutes","restSeconds","dailyMissions"))p.put(key,value(key).toInt())
  p.put("intensityPreference",original?.s("intensityPreference","baja")?:"baja")
  p.put("frequency",week.count { it!=5 }).put("weeklyCategories",arr(week))
  val legacyWeek=(0..6).map { index -> week[(index+6)%7] }
  p.put("weekPlan",arr(legacyWeek.map { when(it){3->"mobility";4->"cardio";5->"rest";else->"train"} }))
  p.put("weekFocus",arr(legacyWeek.map { listOf("upper","core","lower","mobility","cardio","general")[it] }))
  val selectedMaterials=original?.a("materials46")?.strings().orEmpty().toMutableSet()
  for((key,_) in materials)if(gear[key]==true)selectedMaterials.add(key) else selectedMaterials.remove(key)
  val selectedGym=original?.a("gymEquipment23")?.strings().orEmpty().toMutableSet()
  for((key,_) in gymOptions)if(gear["gym_$key"]==true)selectedGym.add(key) else selectedGym.remove(key)
  if(equipment!="gym")selectedGym.clear()
  p.put("materials46",arr(selectedMaterials.filter { it in profileAllowedMaterials(equipment) }))
  p.put("gymEquipment23",arr(selectedGym))
  p.put("bench","bench" in selectedGym || equipment=="db"&&original?.b("bench")==true)
  p.put("equipment",equipment).put("bodyAvailable",gear["body"]==true).put("dumbbellsAvailable",gear["db"]==true)
  p.put("dumbbellWeightsKg",arr(selectedWeights)).put("load",selectedWeights.firstOrNull()?:0)
  store.saveProfile(p,appearance,ui.pendingAvatar)
  ui.pendingAvatar=null
  HunterAlarms.sync(store)
  if(original==null)move(4) else { ui.profileSaved();ui.message("Cambios guardados.") }
 }
 @Composable fun field(key:String,label:String,icon:String,tint:Color,number:Boolean=false){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){
   ProfileBadge(icon,tint)
   HInput(value(key),{ draft[key]=if(key=="name")it.take(40) else it },label,Modifier.weight(1f),number)
  }
 }
 @Composable fun select(key:String,label:String,options:List<Pair<String,String>>){
  val selected=options.firstOrNull { it.first==value(key) }?.second?:value(key)
  HButton("$label · $selected ▾",Modifier.fillMaxWidth(),primary=openSelector==key){openSelector=if(openSelector==key)"" else key}
  if(openSelector==key)for((id,text) in options){
   HButton(text,Modifier.fillMaxWidth(),primary=value(key)==id){draft[key]=id;openSelector=""}
  }
 }
 BackHandler(enabled=ui.stack.isEmpty()&&(stage>0||original!=null)){
  if(stage==4)ui.profileSaved() else if(stage>0)move(stage-1) else ui.closeProfile()
 }
 val palette=remember(value("theme"),value("outlineColor")) {
  HunterColors(HunterAppearanceV43.tokens(value("theme"),value("outlineColor")))
 }
 CompositionLocalProvider(LocalHunter provides palette){
  ProfileWindow(stage,stageArt[stage.coerceAtMost(3)],footer={
   if(stage==4){
    ProfileAction("ENTRAR AL SISTEMA",Modifier.fillMaxWidth(),primary=true){ui.profileSaved()}
   }else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
    if(stage>0)ProfileAction("‹ ATRÁS",Modifier.weight(.8f)){move(stage-1)}
    val label=if(stage<3)"SIGUIENTE ›" else if(original==null)"ACTIVAR PERFIL" else "GUARDAR CAMBIOS"
    ProfileAction(label,Modifier.weight(1.2f),primary=true,enabled=stage<3||confirmed){
     ui.attempt{when(stage){0->{validateIdentity();move(1)};1->{weights();move(2)};2->move(3);else->save()}}
    }
   }
  }){
   when(stage){
    0->{
     field("name","Nombre del cazador *","▤",Color(0xFF338CD1))
     field("age","Edad en años *","▦",Color(0xFF9869D3),true)
     field("height","Estatura (cm) · Opcional","↕",Color(0xFF329878),true)
     field("weight","Peso (kg) · Opcional","◇",Color(0xFFC36769),true)
     ProfileModule("⚥","Género",Color(0xFF9869D3)){
      ProfileChoices(genders,value("gender")){draft["gender"]=it}
     }
     ProfileModule("▣","Fotografía opcional",Color(0xFFC19958)){
      Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
       val avatar=ui.pendingAvatar?:store.state.s("avatar")
       if(avatar.isBlank())AssetImage("profile-monarca/avatar.webp",Modifier.size(52.dp).clip(CircleShape),ContentScale.Crop)
       else Avatar(avatar,value("name"),Modifier.size(52.dp))
       HButton("Elegir fotografía",Modifier.weight(1f)){ui.chooseAvatar()}
      }
     }
     HText("La edad no limita el acceso a los ejercicios.",size=12,color=palette.muted)
     if(original!=null)HButton("Volver al inicio"){ui.closeProfile()}
    }
    1->{
     ProfileModule("▥","Nivel",Color(0xFF338CD1)){ProfileChoices(levels,value("difficulty")){draft["difficulty"]=it}}
     ProfileModule("♥","Objetivo principal",Color(0xFF9869D3)){select("objective","Objetivo",objectives)}
     ProfileModule("✎","Objetivo personalizado",Color(0xFF329878)){
      HInput(value("finalGoal"),{draft["finalGoal"]=it},"Describe tu objetivo con tus propias palabras",multiline=true)
     }
     ProfileModule("⚑","Modalidad del sistema",Color(0xFFC19958)){
      ProfileChoices(listOf("challenges" to "Misiones diarias","routines" to "Rutinas"),value("trainingMode")){
       draft["trainingMode"]=it;openSelector=""
      }
      if(value("trainingMode")=="routines"){
       select("restSeconds","Descanso",rests);select("blockMode22","Organización",formats)
       select("minutes","Tiempo disponible",durations)
      }else{
       select("dailyMissions","Cantidad",(1..7).map { it.toString() to if(it==1)"1 misión" else "$it misiones" })
       HText("Las misiones pueden incluir actividades ligeras, aprendizaje y descansos.",size=12)
      }
     }
     ProfileModule("▦","Planificación semanal",Color(0xFF9869D3)){
      Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){
       for(i in week.indices){
        val cat=week[i];val color=Color(categoryColors[cat]);val shape=RoundedCornerShape(6.dp)
        Column(Modifier.width(42.dp).height(49.dp).clip(shape).background(color.copy(alpha=.22f))
         .border(1.dp,color,shape).semantics { contentDescription=dayNames[i]+": "+categoryLabels[cat] }
         .clickable(role=Role.Button){week[i]=(cat+1)%6},horizontalAlignment=Alignment.CenterHorizontally,
         verticalArrangement=Arrangement.Center){
         HText("LMXJVSD"[i].toString(),size=13,bold=true);HText(categoryIcons[cat],size=14)
        }
       }
      }
      for(i in categoryLabels.indices)HText(categoryIcons[i]+"  "+categoryLabels[i],size=12,bold=true,
       modifier=Modifier.border(1.dp,Color(categoryColors[i]),RoundedCornerShape(4.dp)).padding(horizontal=5.dp))
     }
     ProfileModule("◇","Equipamiento disponible",Color(0xFFC36769)){
      val choices=listOf("body" to "Peso corporal / sin equipo")+materials+listOf("db" to "Mancuernas","gym" to "Gimnasio")
      for((key,label) in choices)HCheck(label,gear[key]==true){gear[key]=gear[key]!=true}
      if(gear["db"]==true)HInput(value("dumbbellWeights"),{draft["dumbbellWeights"]=it},"Pesos disponibles (kg), separados por comas · 2, 4, 6")
      if(gear["gym"]==true)for((id,label) in gymOptions)HCheck(label,gear["gym_$id"]==true){gear["gym_$id"]=gear["gym_$id"]!=true}
      HButton("Indicaciones y restricciones ▾",Modifier.fillMaxWidth()){
       healthExpanded["health"]=healthExpanded["health"]!=true
      }
      if(healthExpanded["health"]==true){
       val section=store.catalog.profileForm.objects().last()
       HunterProfileFields(section,section.a("fields").objects().map { it.s("name") }.toSet(),
        healthValues,healthExpanded,store){}
       for((i,injury) in injuries.withIndex())HButton(
        (zoneLabels[injury.s("zone")]?:injury.s("zone"))+" · Quitar",
        Modifier.fillMaxWidth()){injuries.removeAt(i)}
       HSelect("Zona",injuryZone,zoneLabels.toList()){injuryZone=it}
       HSelect("Tipo",injuryType,typeLabels.toList()){injuryType=it}
       ProfileChoices(listOf("current" to "Actual","past" to "Anterior"),injuryStage){injuryStage=it}
       HButton("Añadir zona"){injuries+=obj("zone" to injuryZone,"type" to injuryType,"stage" to injuryStage)}
      }
     }
    }
    2->{
     ProfileModule("♟","Acompañante del sistema",Color(0xFF338CD1)){
      val allies=MonarchCompanions.profileChoices
      Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
       for(item in allies)ProfilePicture("ui33/companion-${item.asset}.webp",item.title,
        value("companionCharacter")==item.id,Modifier.size(75.dp)){draft["companionCharacter"]=item.id}
      }
     }
     ProfileModule("◇","Apariencia",Color(0xFF9869D3)){
      Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
       HButton("Tema · "+(themes.firstOrNull { it.first==value("theme") }?.second.orEmpty()),Modifier.weight(1f)){
        openSelector=if(openSelector=="theme")"" else "theme"
       }
       HButton("Borde",Modifier.weight(1f)){openSelector=if(openSelector=="outlineColor")"" else "outlineColor"}
      }
      Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
       HButton("Silueta",Modifier.weight(1f)){openSelector=if(openSelector=="collection")"" else "collection"}
       HButton("Fondo",Modifier.weight(1f)){openSelector=if(openSelector=="scene")"" else "scene"}
      }
      when(openSelector){
       "theme"->ProfileChoices(themes,value("theme")){draft["theme"]=it;openSelector=""}
       "outlineColor"->for(pair in edges.chunked(2)){
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
         for((id,label) in pair){
          val shade=parseColor(HunterAppearanceV43.borderHex(value("theme"),id))
          val shape=RoundedCornerShape(7.dp)
          Row(Modifier.weight(1f).clip(shape).background(palette.input)
           .border(if(value("outlineColor")==id)2.dp else 1.dp,shade,shape)
           .clickable(role=Role.RadioButton){draft["outlineColor"]=id;openSelector=""}
           .padding(7.dp),verticalAlignment=Alignment.CenterVertically,
           horizontalArrangement=Arrangement.spacedBy(5.dp)){
           Box(Modifier.size(15.dp).background(shade,CircleShape).border(1.dp,palette.line,CircleShape))
           HText(label,size=13)
          }
         }
        }
       }
       "collection"->for(pair in silhouettes.withIndex().chunked(2)){
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
         for((i,id) in pair)ProfilePicture(chosenHunterArtwork(id,value("gender")),"Silueta ${i+1}",
          value("collection")==id,Modifier.weight(1f).height(92.dp)){draft["collection"]=id;openSelector=""}
         if(pair.size==1)Spacer(Modifier.weight(1f))
        }
       }
       "scene"->for(pair in scenes.chunked(2)){
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
         for((id,label) in pair)Column(Modifier.weight(1f)){
          ProfilePicture(chosenSceneArtwork(id),label,value("scene")==id,Modifier.fillMaxWidth().height(73.dp),ContentScale.Crop){
           draft["scene"]=id;openSelector=""
          }
          HText(label,size=12)
         }
         if(pair.size==1)Spacer(Modifier.weight(1f))
        }
       }
      }
     }
     ProfileModule("▣","Vista previa del cazador",Color(0xFFC19958)){
      HunterProfileCardPreview(value("collection"),value("gender"),value("scene"),value("name"),
       store.stats().i("level",1),store.stats().i("xp"),value("companionCharacter"))
     }
     if(original!=null){
      HButton("Ajustes de la aplicación ▾",Modifier.fillMaxWidth()){
       openSelector=if(openSelector=="app")"" else "app"
      }
      if(openSelector=="app"){
       HButton("Alarmas del cazador",Modifier.fillMaxWidth()){ui.open("reminders")}
       HSelect("Rendimiento visual",visualTier(store).id,
        listOf("max" to "Máxima calidad","balanced" to "Equilibrado","saver" to "Ahorro de energía")){
        store.state.put("visualQuality",it);store.save()
       }
       HButton("Exportar copia de seguridad",Modifier.fillMaxWidth()){ui.export()}
       HButton("Restaurar copia",Modifier.fillMaxWidth()){ui.import()}
       HCheck("Actualizar entrenadores automáticamente",store.state.b("creatorAuto",true)){
        val enabled=!store.state.b("creatorAuto",true)
        store.state.put("creatorAuto",enabled);store.save()
        CreatorFeedNative.schedule(store.context,enabled)
       }
       HButton("Fuentes y publicaciones",Modifier.fillMaxWidth()){ui.open("sources")}
      }
     }
    }
    3->{
     val labels=listOf("Identidad del cazador","Entrenamiento","Personalización")
     for((i,title) in labels.withIndex()){
      HButton(title+(if(summary==i)"  ⌃" else "  ▾"),Modifier.fillMaxWidth()){
       summary=if(summary==i)-1 else i
      }
      if(summary==i)HText(when(i){
       0->listOf(value("name"),value("age")+" años","Estatura: "+value("height").ifBlank{"Sin indicar"},
        "Peso: "+value("weight").ifBlank{"Sin indicar"},genders.firstOrNull { it.first==value("gender") }?.second.orEmpty()).joinToString("\n")
       1->listOf(levels.firstOrNull { it.first==value("difficulty") }?.second.orEmpty(),
        objectives.firstOrNull { it.first==value("objective") }?.second?:value("objective"),
        value("finalGoal").ifBlank{"Objetivo personal sin indicar"},
        if(value("trainingMode")=="routines")"Rutinas · ${value("minutes")} min · ${value("restSeconds")} s de descanso · "+(formats.firstOrNull { it.first==value("blockMode22") }?.second?:"Por series") else "${value("dailyMissions")} misiones diarias",
        dayNames.indices.joinToString("\n"){dayNames[it]+": "+categoryLabels[week[it]]},
        (listOf("body" to "Peso corporal")+materials+listOf("db" to "Mancuernas","gym" to "Gimnasio")+
         gymOptions.map { "gym_${it.first}" to it.second }).filter { gear[it.first]==true }.joinToString { it.second }.ifBlank{"Equipo sin indicar"}
       ).joinToString("\n")
       else->listOf(themes.first { it.first==value("theme") }.second,
        edges.firstOrNull { it.first==value("outlineColor") }?.second?:value("outlineColor"),
        scenes.firstOrNull { it.first==value("scene") }?.second?:"Fondo guardado","Silueta y acompañante seleccionados").joinToString("\n")
      },size=15)
     }
     HCheck("He revisado mi información y deseo activar mi perfil de cazador.",confirmed){confirmed=!confirmed}
    }
    4->{Heading("Sistema activado",size=27);HText("Registro completado.")}
   }
  }
 }
}

internal fun parseDumbbellWeights(text:String):List<Double> =
 text.split(',').map(String::trim).filter(String::isNotBlank).map {
  val value=it.toDoubleOrNull()
  require(value!=null&&value.isFinite()&&value>0){"Indica pesos positivos separados por comas"}
  value
 }.distinct().sorted()

@Composable private fun ProfileWindow(stage:Int,art:String,footer:@Composable ()->Unit,content:@Composable ColumnScope.()->Unit){
 val c=LocalHunter.current
 val shape=CutCornerShape(16.dp)
 val gold=if(c.isLight)Color(0xFF9A7040) else Color(0xFFE9BE79)
 Box(Modifier.fillMaxSize()){
  AssetImage("profile-monarca/$art.webp",Modifier.matchParentSize(),ContentScale.Crop,alignment=Alignment.CenterStart)
  HText("✦ HUNTER SYSTEM · MONARCA ECLIPSE",Modifier.align(Alignment.TopEnd).fillMaxWidth(.76f)
   .padding(top=11.dp,end=6.dp),size=15,color=Color(0xFFFFE8BA),bold=true)
  Column(Modifier.align(Alignment.CenterEnd).fillMaxWidth(.73f).fillMaxHeight(.84f)
   .padding(end=7.dp).clip(shape).background(c.bg).border(2.dp,gold,shape)
   .padding(horizontal=7.dp,vertical=9.dp)){
   Box(Modifier.weight(1f)){
    key(stage){
     Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=3.dp,vertical=6.dp),
      verticalArrangement=Arrangement.spacedBy(9.dp)){
      if(stage<4){
       HText("◆  PASO ${stage+1} DE 4",size=13,bold=true,color=gold)
       Heading(stages[stage],size=25)
       Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
        for(i in stages.indices)Box(Modifier.weight(1f).height(32.dp).border(1.dp,
         if(i==stage)gold else c.line,CircleShape).semantics { selected=i==stage },contentAlignment=Alignment.Center){
         HText((i+1).toString(),size=14,bold=i==stage,color=if(i==stage)gold else c.muted)
        }
       }
      }
      content()
     }
    }
   }
   Spacer(Modifier.height(5.dp));Box(Modifier.fillMaxWidth().background(c.line).height(1.dp))
   Spacer(Modifier.height(5.dp));footer()
  }
 }
}

@Composable private fun ProfileAction(text:String,modifier:Modifier,primary:Boolean=false,enabled:Boolean=true,onClick:()->Unit){
 val c=LocalHunter.current
 val shape=CutCornerShape(10.dp)
 val edge=if(primary&&enabled)Color(0xFFE1AF63) else c.line
 val fill=if(primary&&enabled){
  if(c.isLight)listOf(Color(0xFFFFECC2),Color(0xFFDAA861)) else listOf(Color(0xFF805429),Color(0xFF4D301A))
 }else listOf(c.surface,c.input)
 Box(modifier.heightIn(min=47.dp).clip(shape).background(Brush.verticalGradient(fill)).border(1.dp,edge,shape)
  .clickable(enabled=enabled,role=Role.Button,onClick=onClick).padding(6.dp),contentAlignment=Alignment.Center){
  HText(text,size=13,color=when{!enabled->c.muted;primary&&c.isLight->Color(0xFF42290E);primary->Color(0xFFFFEAC7);else->c.text},bold=true)
 }
}

@Composable private fun ProfileBadge(icon:String,tint:Color){
 val shape=CutCornerShape(4.dp)
 Box(Modifier.size(32.dp).clip(shape).background(tint.copy(alpha=.24f)).border(1.dp,tint,shape),contentAlignment=Alignment.Center){
  HText(icon,size=17,bold=true)
 }
}
@Composable private fun ProfileModule(icon:String,title:String,tint:Color,content:@Composable ColumnScope.()->Unit){
 HPanel(decor=false,padding=8){
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){
   ProfileBadge(icon,tint);HText(title,size=16,bold=true)
  }
  content()
 }
}
@Composable private fun ProfileChoices(options:List<Pair<String,String>>,selected:String,onChoice:(String)->Unit){
 val c=LocalHunter.current
 val shape=RoundedCornerShape(7.dp)
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
  for((id,label) in options){
   val active=id==selected
   Box(Modifier.weight(1f).heightIn(min=45.dp).clip(shape)
    .background(if(active)c.selected else c.input)
    .border(if(active)2.dp else 1.dp,if(active)c.accent else c.line,shape)
    .semantics { this.selected=active }.clickable(role=Role.RadioButton){onChoice(id)}
    .padding(4.dp),contentAlignment=Alignment.Center){
    HText(label,size=13,bold=active)
   }
  }
 }
}
@Composable private fun ProfilePicture(asset:String,label:String,active:Boolean,modifier:Modifier,scale:ContentScale=ContentScale.Fit,onClick:()->Unit){
 val c=LocalHunter.current;val shape=CutCornerShape(7.dp)
 Box(modifier.clip(shape).background(c.input).border(if(active)2.dp else 1.dp,if(active)c.accent else c.line,shape)
  .semantics { contentDescription=label;selected=active }.clickable(role=Role.RadioButton,onClick=onClick)){
  AssetImage(asset,Modifier.fillMaxSize().padding(3.dp),scale)
  if(active)HText("✓",Modifier.align(Alignment.TopEnd).background(c.bg).padding(horizontal=4.dp),size=13,bold=true)
 }
}
val zoneLabels=linkedMapOf("neck" to "Cuello","shoulder" to "Hombro","elbow" to "Codo","wrist" to "Muñeca o mano","back" to "Espalda","hip" to "Cadera","knee" to "Rodilla","ankle" to "Tobillo o pie","other" to "Otra zona / no sé")
val typeLabels=linkedMapOf("muscle" to "Muscular","tendon" to "Tendón","joint" to "Articulación o ligamento","bone" to "Fractura / lesión ósea","surgery" to "Después de una cirugía","unknown" to "Sin diagnóstico / otra")
