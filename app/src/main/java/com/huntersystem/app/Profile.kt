package com.huntersystem.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.json.JSONObject
import java.time.LocalDate

internal val objectives = mapOf("general" to "Mejorar mi bienestar general", "strength" to "Desarrollar fuerza funcional",
 "endurance" to "Mejorar mi resistencia", "mobility" to "Mejorar mi movilidad", "habit" to "Crear un hábito de actividad física")
internal val materials = listOf("mat" to "Colchoneta o superficie acolchada estable",
 "wall" to "Pared o apoyo vertical firme", "chair" to "Silla estable")
internal val gymOptions = listOf("cable" to "Poleas", "machine" to "Máquinas guiadas", "bench" to "Banco",
 "barbell" to "Barras", "bike" to "Bicicleta estática", "rower" to "Máquina de remo", "elliptical" to "Elíptica",
 "smith" to "Máquina Smith", "airbike" to "Bicicleta de aire", "cardio" to "Todas las máquinas de cardio del catálogo")

private val profileChoiceIds=mapOf(
 "gender" to setOf("unspecified","male","female"),
 "difficulty" to setOf("basic","intermediate","advanced"),
 "objective" to objectives.keys,
 "trainingMode" to setOf("challenges","routines"),
 "dailyMissions" to (1..7).map(Int::toString).toSet(),
 "restSeconds" to setOf("30","60","90"),
 "blockMode22" to setOf("rounds","exercise"),
 "minutes" to setOf("10","15","20","30","45","60"),
 "theme" to setOf("light","dark"),
 "outlineColor" to HunterAppearanceV43.profileOutlines.map { it.id }.toSet(),
 "companionCharacter" to MonarchCompanions.profileChoices.map { it.id }.toSet(),
 "collection" to setOf("hunter-male","hunter-female","reader","regressor","reader-female"),
 "scene" to setOf("templo-del-ocaso","biblioteca-lunar","trono-volcanico","reino-de-las-nubes","jardin-sagrado")
)

internal fun profileIdentityIssue(fields: JSONObject): String? {
 val name=fields.s("name")
 if(name.isBlank() || name.length>40)return "Escribe tu nombre de cazador"
 if(fields.s("age").toIntOrNull()?.let { it>=0 }!=true)return "Escribe una edad válida en años"
 for((key,label) in listOf("height" to "estatura","weight" to "peso")){
  val raw=fields.s(key)
  if(raw.isNotBlank() && raw.replace(',','.').toDoubleOrNull()?.let { it.isFinite() && it>0 }!=true)
   return "Revisa ${if(key=="height") "la" else "el"} $label"
 }
 return null
}

internal class ProfileRegistration(private val ui:NativeUi) {
 val store=ui.store
 val original=store.profile?.copyJson()
 val originalAppearance=store.state.o("appearance33").copyJson()
 private val draft=mutableMapOf(
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
   "collection" to (original?.let { originalAppearance.s("collection","hunter-male") } ?: "hunter-male"),
   "scene" to (original?.let { originalAppearance.s("scene","templo-del-ocaso") } ?: "templo-del-ocaso")
  )
 private val initial=draft.toMap()
 val healthValues=mutableStateMapOf<String,String>().apply {
   original?.o("health")?.entries()?.forEach { (key,value)->put(key,value.toString()) }
   original?.o("restrictions")?.entries()?.forEach { (key,value)->put("r_$key",value.toString()) }
  }
 private val initialHealth=healthValues.toMap()
 val injuries=mutableStateListOf<JSONObject>().apply { addAll(original?.a("injuries")?.objects().orEmpty()) }
 private val initialWeek=original?.let { MonarchProfileAdapter.from(it,LocalDate.now())?.weekly }?:List(7){5}
 private val additionalMaterials = store.catalog.originalEquipmentOptions.filter { (id, _) ->
  id in profileAllowedMaterials("gym") && materials.none { it.first == id }
 }
 private val materialOptions = materials + additionalMaterials
 fun accessoryOptions(equipment:String) = buildList {
  if(equipment!="gym") add("bench" to "Banco estable")
  add("step" to "Escalón estable")
  addAll(additionalMaterials.filter { it.first in profileAllowedMaterials(equipment) })
 }

 val capabilityOptions = listOf("landing", "hanging", "strictpull", "supporthold", "overhead", "nordic")
  .map { id -> id to store.catalog.capabilities.s(id) }

 private val initialGear=buildMap {
  put("body",original?.b("bodyAvailable",true)?:true)
  put("db",original?.b("dumbbellsAvailable",original.s("equipment") in listOf("db","gym"))?:false)
  put("gym",original?.s("equipment")=="gym")
  put("bench",original?.b("bench")==true && "bench" !in original.a("gymEquipment23").strings())
  put("step",original?.b("step")==true)
  materialOptions.forEach { (id,_) -> put(id,id in original?.a("materials46")?.strings().orEmpty()) }
  capabilityOptions.forEach { (id,_) -> put("cap_$id",id in original?.a("capabilities46")?.strings().orEmpty()) }
  gymOptions.forEach { (id,_) -> put("gym_$id",id in original?.a("gymEquipment23")?.strings().orEmpty()) }
 }

 val editing get()=original!=null

 fun initialData()=obj(
  "editing" to editing,
  "fields" to JSONObject(initial),
  "week" to arr(initialWeek),
  "gear" to JSONObject(initialGear),
  "photo" to (ui.pendingAvatar?:store.state.s("avatar")),
  "legacyLabels" to obj(
   "goal" to "Objetivo guardado",
   "edge" to "Color guardado",
   "scene" to (hunterProfileSceneChoices.toMap()[value("scene")]?:"Fondo guardado"),
   "figure" to "Silueta guardada"
  ),
  "legacyArtwork" to obj(
   "edge" to HunterAppearanceV43.borderHex(value("theme"),value("outlineColor")),
   "scene" to "/assets/"+chosenSceneArtwork(value("scene")),
   "figure" to "/assets/"+chosenHunterArtwork(value("collection"),value("gender"))
  )
 )

 private fun value(key:String)=draft.getValue(key)
 fun submit(input:JSONObject){
  require(input.b("confirmed")){"Confirma que revisaste tu información."}
  val incoming=input.o("fields")
  val week=input.a("week").let { values ->
   require(values.length()==7){"Revisa la planificación semanal."}
   (0..6).map { index -> values.getInt(index).also { require(it in 0..5) } }
  }
  val declared=input.o("gear")
  val gear=initialGear.keys.associateWith { declared.b(it,initialGear.getValue(it)) }
  for(key in initial.keys){
   val value=incoming.optString(key,"")
   val allowed=profileChoiceIds[key]
   require(allowed==null||value in allowed||value==initial[key]){"Revisa las opciones de tu perfil."}
   draft[key]=value
  }
  profileIdentityIssue(JSONObject(draft))?.let { throw IllegalArgumentException(it) }
  val appearance=originalAppearance.copyJson().put("collection",value("collection")).put("scene",value("scene"))
  val artworkKeys=setOf("theme","outlineColor","collection","scene","companionCharacter")
  val appearanceOnly=original!=null&&initialWeek==week&&initialGear==gear&&initialHealth==healthValues&&
   arr(injuries).toString()==original.a("injuries").toString()&&draft.all { (key,v)->key in artworkKeys||initial[key]==v }
  if(appearanceOnly){
   store.updateAppearanceOnly(value("theme"),appearance,ui.pendingAvatar,value("outlineColor"),value("companionCharacter"))
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
  val selectedWeights=if(gear["db"]==true)parseDumbbellWeights(value("dumbbellWeights")) else emptyList()
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
  for((key,_) in materialOptions)if(gear[key]==true)selectedMaterials.add(key) else selectedMaterials.remove(key)
  val selectedGym=original?.a("gymEquipment23")?.strings().orEmpty().toMutableSet()
  for((key,_) in gymOptions)if(gear["gym_$key"]==true)selectedGym.add(key) else selectedGym.remove(key)
  if(equipment!="gym")selectedGym.clear()
  p.put("materials46",arr(selectedMaterials.filter { it in profileAllowedMaterials(equipment) }))
  p.put("gymEquipment23",arr(selectedGym))
  val capabilities=original?.a("capabilities46")?.strings().orEmpty().toMutableSet()
  for((key,_) in capabilityOptions)if(gear["cap_$key"]==true)capabilities.add(key) else capabilities.remove(key)
  p.put("capabilities46",arr(capabilities))
  p.put("bench","bench" in selectedGym || equipment!="gym"&&gear["bench"]==true)
  p.put("step",gear["step"]==true)
  p.put("equipment",equipment).put("bodyAvailable",gear["body"]==true).put("dumbbellsAvailable",gear["db"]==true)
  p.put("dumbbellWeightsKg",arr(selectedWeights)).put("load",selectedWeights.firstOrNull()?:0)
  store.saveProfile(p,appearance,ui.pendingAvatar)
  ui.pendingAvatar=null
  HunterAlarms.sync(store)
 }
}

@Composable
fun ProfileScreen(ui:NativeUi) {
 val session=remember(ui) { ProfileRegistration(ui) }
 var supplementary by remember { mutableStateOf<String?>(null) }
 ComposeHunterProfile(ui,session,supplementary!=null){ supplementary=it }
 supplementary?.let { mode -> ProfileSupplement(ui,session,mode){supplementary=null} }
}

@Composable
private fun ProfileSupplement(ui:NativeUi,session:ProfileRegistration,mode:String,close:()->Unit) {
 val store=ui.store
 Dialog(onDismissRequest=close) {
  HPanel {
   Column(Modifier.heightIn(max=560.dp).verticalScroll(rememberScrollState()),
    verticalArrangement=Arrangement.spacedBy(8.dp)) {
    if(mode=="health") {
     Heading("Indicaciones y restricciones")
     val healthExpanded=remember { mutableStateMapOf<String,Boolean>() }
     var injuryZone by remember { mutableStateOf("knee") }
     var injuryType by remember { mutableStateOf("joint") }
     var injuryStage by remember { mutableStateOf("current") }
     val section=store.catalog.profileForm.objects().last()
     HunterProfileFields(section,section.a("fields").objects().map { it.s("name") }.toSet(),
      session.healthValues,healthExpanded,store){}
     for((i,injury) in session.injuries.withIndex())HButton(
      (zoneLabels[injury.s("zone")]?:injury.s("zone"))+" · Quitar",
      Modifier.fillMaxWidth()){session.injuries.removeAt(i)}
     HSelect("Zona",injuryZone,zoneLabels.toList()){injuryZone=it}
     HSelect("Tipo",injuryType,typeLabels.toList()){injuryType=it}
     HSelect("Estado",injuryStage,listOf("current" to "Actual","past" to "Anterior")){injuryStage=it}
     HButton("Añadir zona"){session.injuries+=obj("zone" to injuryZone,"type" to injuryType,"stage" to injuryStage)}
    } else {
     Heading("Ajustes de la aplicación")
     HButton("Alarmas del cazador",Modifier.fillMaxWidth()){close();ui.open("reminders")}
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
     HButton("Fuentes y publicaciones",Modifier.fillMaxWidth()){close();ui.open("sources")}
    }
    HButton("Cerrar",Modifier.fillMaxWidth(),onClick=close)
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

private val zoneLabels=linkedMapOf("neck" to "Cuello","shoulder" to "Hombro","elbow" to "Codo","wrist" to "Muñeca o mano","back" to "Espalda","hip" to "Cadera","knee" to "Rodilla","ankle" to "Tobillo o pie","other" to "Otra zona / no sé")
private val typeLabels=linkedMapOf("muscle" to "Muscular","tendon" to "Tendón","joint" to "Articulación o ligamento","bone" to "Fractura / lesión ósea","surgery" to "Después de una cirugía","unknown" to "Sin diagnóstico / otra")
