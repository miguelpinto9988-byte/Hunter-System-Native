package com.huntersystem.app

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Grouping affects only presentation. Every material and technical skill keeps its
// own explicit, independently confirmed key for the training compatibility rules.
private data class MaterialGroup(val title:String,val ids:List<String>)
private val materialGroups=listOf(
 MaterialGroup("Espacio y apoyo",listOf("space","wall","mat","chair","support","table","towel","stick","doorframe","ball","slider","swissball","foamroller","footanchor")),
 MaterialGroup("Bandas y barras",listOf("band","anchor","pullbar","lowbar","parallels","suspension","rings","abwheel")),
 MaterialGroup("Pesas y gimnasio",listOf("kb","landmine","rack","lightlever","romanchair","sled","strap","medball","slamball","throwwall","battlerope","harness","rope")),
 MaterialGroup("Cardio y coordinación",listOf("plyobox","cones","ladder","hurdle","throwspace","partner","jumprope"))
)
private val specializedTools=setOf("landmine","rack","lightlever","romanchair","sled","strap","battlerope","harness","rope")
private val weightsTools=setOf("kb","medball","slamball","throwwall")
internal fun profileAllowedMaterials(equipment:String):Set<String>{
 val all=materialGroups.flatMap{it.ids}.toSet()
 return when(equipment){
  "gym"->all
  "db"->all-specializedTools
  else->all-specializedTools-weightsTools
 }
}

internal fun selectedProfileMaterials(equipment:String,values:Map<String,String>):List<String>{
 val allowed=profileAllowedMaterials(equipment)
 return values.entries.filter{
  it.key.startsWith("materials46|")&&it.value=="true"&&it.key.substringAfter('|') in allowed
 }.map{it.key.substringAfter('|')}
}
internal fun selectedProfileCapabilities(equipment:String,values:Map<String,String>):List<String>{
 val allowed=profileAllowedCapabilities(equipment)
 return values.entries.filter{
  it.key.startsWith("capabilities46|")&&it.value=="true"&&it.key.substringAfter('|') in allowed
 }.map{it.key.substringAfter('|')}
}
internal fun selectedProfileGym(equipment:String,values:Map<String,String>):List<String>{
 if(equipment!="gym")return emptyList()
 val keys=setOf("barbell","cable","machine","smith","cardio","bench")
 return values.entries.filter{
  it.key.startsWith("gym_")&&it.value=="true"&&it.key.removePrefix("gym_") in keys
 }.map{it.key.removePrefix("gym_")}
}
private val capabilityGroups=listOf(
 MaterialGroup("Equilibrio y control",listOf("singlebalance","landing","rotation","hanging")),
 MaterialGroup("Fuerza y apoyos",listOf("strictpull","supporthold","overhead","inverted","nordic")),
 MaterialGroup("Técnicas avanzadas",listOf("ballistichinge","turkish","olympic","neuralguidance"))
)
internal fun profileAllowedCapabilities(equipment:String):Set<String>{
 val all=capabilityGroups.flatMap{it.ids}.toSet()
 return when(equipment){
  "gym"->all
  "db"->all-"olympic"
  else->all-setOf("overhead","ballistichinge","turkish","olympic")
 }
}
/**
 * Controles compactos y explícitos.
 * Las opciones con varias claves requieren TODOS los complementos indicados:
 * nunca se infiere equipo ni habilidad a partir del nivel de entrenamiento.
 */
internal data class ProfileCompactOption(val label:String,val keys:List<String>)

internal val profileCompactMaterials=listOf(
 ProfileCompactOption("Espacio despejado, pared libre",listOf("materials46|space","materials46|wall")),
 ProfileCompactOption("Colchoneta",listOf("materials46|mat")),
 ProfileCompactOption("Silla estable",listOf("materials46|chair")),
 ProfileCompactOption("Banda elástica, anclaje seguro",listOf("materials46|band","materials46|anchor")),
 ProfileCompactOption("Barra de dominadas",listOf("materials46|pullbar")),
 ProfileCompactOption("Escalón o plataforma baja",listOf("step"))
)

internal val profileCompactGym=listOf(
 ProfileCompactOption("Barra con discos, banco (ambos)",listOf("gym_barbell","gym_bench")),
 ProfileCompactOption("Poleas, máquinas de fuerza (ambas)",listOf("gym_cable","gym_machine")),
 ProfileCompactOption("Máquinas de cardio",listOf("gym_cardio")),
 ProfileCompactOption("Rack con seguros",listOf("materials46|rack"))
)

internal val profileCompactDb=listOf(
 ProfileCompactOption("Banco de entrenamiento",listOf("bench"))
)

internal val profileCompactSkills=listOf(
 ProfileCompactOption("Equilibrio a una pierna durante 30 s",listOf("capabilities46|singlebalance")),
 ProfileCompactOption("Colgarse 20 s con control y sin dolor",listOf("capabilities46|hanging")),
 ProfileCompactOption("Aterrizar saltos bajos con estabilidad",listOf("capabilities46|landing"))
)
internal val profileCompactWeightSkill=ProfileCompactOption(
 "Control de una carga ligera sobre la cabeza, sin dolor",
 listOf("capabilities46|overhead")
)

internal fun profileCompactEquipment(equipment:String):List<ProfileCompactOption> =
 profileCompactMaterials+when(equipment){
  "gym"->profileCompactGym
  "db"->profileCompactDb
  else->emptyList()
 }

internal fun profileCompactCapabilities(equipment:String):List<ProfileCompactOption> =
 profileCompactSkills+if(equipment=="db"||equipment=="gym")listOf(profileCompactWeightSkill)
 else emptyList()

internal fun profileCompactSelected(option:ProfileCompactOption,values:Map<String,String>):Boolean=
 option.keys.all{values[it]=="true"}

/** Las selecciones avanzadas antiguas no desaparecen silenciosamente del perfil. */
internal fun profileLegacySelections(equipment:String,values:Map<String,String>):List<String>{
 val shown=(profileCompactEquipment(equipment)+profileCompactCapabilities(equipment))
  .flatMap{it.keys}.toSet()
 return values.keys.filter{key->
  values[key]=="true"&&key !in shown&&(
   key.startsWith("materials46|")||key.startsWith("capabilities46|")||
   key.startsWith("gym_")||key=="bench"||key=="step"
  )
 }.sorted()
}

@Composable private fun ProfileCompactCheck(
 values:MutableMap<String,String>,
 option:ProfileCompactOption
){
 val n=option.keys.count{values[it]=="true"}
 val partial=if(n in 1 until option.keys.size)" · $n/${option.keys.size} guardados" else ""
 HCheck(option.label+partial,n==option.keys.size){
  val enabled=n!=option.keys.size
  for(key in option.keys) values[key]=enabled.toString()
 }
}

@Composable fun ProfileEquipmentOptions(
 equipment:String,
 values:MutableMap<String,String>,
 expanded:MutableMap<String,Boolean>,
 catalog:Catalog
){
 val options=profileCompactEquipment(equipment)
 val skills=profileCompactCapabilities(equipment)
 val legacy=profileLegacySelections(equipment,values)
 HText(when(equipment){
  "gym"->"Selecciona únicamente el equipo que tu gimnasio tiene disponible."
  "db"->"Mancuernas y complementos opcionales, sin listas extensas."
  else->"Con tu peso corporal puedes empezar sin seleccionar accesorios."
 },size=15,color=LocalHunter.current.muted)
 if(equipment=="db"||equipment=="gym"){
  HInput(values["load"]?:"0",{values["load"]=it},"Peso disponible de una mancuerna · kg",number=true)
 }
 val count=(options+skills).count{profileCompactSelected(it,values)}
 val panelId="profile-compact-options"
 HButton(
  "Complementos y habilidades (opcional) · $count seleccionados ▾",
  Modifier.fillMaxWidth()
 ){expanded[panelId]=expanded[panelId]!=true}
 if(expanded[panelId]==true){
  HText("Complementos habituales",size=16,bold=true)
  HText(
   "Marca solo lo que tengas. Una opción con comas requiere disponer de todos los elementos indicados.",
   size=13,color=LocalHunter.current.muted
  )
  for(option in profileCompactMaterials)ProfileCompactCheck(values,option)
  if(equipment=="db"){
   HText("Para mancuernas",size=16,bold=true)
   for(option in profileCompactDb)ProfileCompactCheck(values,option)
  }
  if(equipment=="gym"){
   HText("Para gimnasio",size=16,bold=true)
   for(option in profileCompactGym)ProfileCompactCheck(values,option)
  }
  HText("Habilidades comprobadas (opcional)",size=16,bold=true)
  HText(
   "Se indican solo si ya las realizas con control. Tu nivel no las activa automáticamente.",
   size=13,color=LocalHunter.current.muted
  )
  for(option in skills)ProfileCompactCheck(values,option)
  if(legacy.isNotEmpty()){
   HText(
    "${legacy.size} opciones específicas de una configuración anterior siguen guardadas. " +
    "No se añadirán otras automáticamente.",
    size=13,color=LocalHunter.current.muted
   )
   HButton("Quitar opciones antiguas · ${legacy.size}",Modifier.fillMaxWidth()){
    legacy.forEach{values[it]="false"}
   }
  }
 }
}
