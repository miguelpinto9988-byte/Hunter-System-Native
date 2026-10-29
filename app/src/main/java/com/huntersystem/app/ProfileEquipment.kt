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
private val gymGroups=listOf(
 MaterialGroup("Barras y bancos",listOf("gym_barbell","gym_bench")),
 MaterialGroup("Máquinas y poleas",listOf("gym_cable","gym_machine","gym_smith")),
 MaterialGroup("Cardio de gimnasio",listOf("gym_cardio"))
)
private val gymLabels=mapOf(
 "gym_barbell" to "Barra y discos","gym_bench" to "Banco y soportes",
 "gym_cable" to "Poleas","gym_machine" to "Máquinas de fuerza",
 "gym_smith" to "Máquina Smith","gym_cardio" to "Cinta o ergómetro"
)
@Composable private fun ProfileEquipmentCheck(values:MutableMap<String,String>,key:String,label:String){
 HCheck(label,values[key]=="true"){values[key]=(values[key]!="true").toString()}
}
/**
 * Accesos rápidos distintos para cada modalidad; son una vista, no un cambio
 * de permisos. Los demás materiales siguen disponibles bajo "Otros accesorios".
 */
internal fun profileQuickMaterialKeys(equipment:String):List<String> = when(equipment){
 "db" -> listOf("mat","band","chair")
 "gym" -> listOf("mat","band")
 else -> listOf("space","mat","chair","band")
}
internal fun profileQuickCapabilityKeys(equipment:String):List<String> = when(equipment){
 "db" -> listOf("singlebalance","overhead")
 "gym" -> listOf("singlebalance","hanging","overhead")
 else -> listOf("singlebalance","hanging")
}

@Composable
fun ProfileEquipmentOptions(
 equipment:String,
 values:MutableMap<String,String>,
 expanded:MutableMap<String,Boolean>,
 catalog:Catalog
){
 val selected=if(equipment in setOf("body","db","gym"))equipment else "body"
 val isGym=selected=="gym"
 val isDumbbell=selected=="db"
 val quickMaterials=profileQuickMaterialKeys(selected)
 val quickCapabilities=profileQuickCapabilityKeys(selected)
 val allowedMaterials=profileAllowedMaterials(selected)
 val allowedCapabilities=profileAllowedCapabilities(selected)
 val chosenMaterials=selectedProfileMaterials(selected,values)
 val chosenCapabilities=selectedProfileCapabilities(selected,values)
 val gymCount=selectedProfileGym(selected,values).size
 val extrasCount=chosenMaterials.count{it !in quickMaterials}
 val extrasCapabilities=chosenCapabilities.count{it !in quickCapabilities}
 val equipmentCount=chosenMaterials.size+gymCount+
     (if(values["step"]=="true")1 else 0)+
     (if(isDumbbell&&values["bench"]=="true")1 else 0)
 val equipmentOpenKey="profile-equipment-$selected"
 val capabilitiesOpenKey="profile-capabilities-$selected"
 val accessoryOpenKey="profile-extras-$selected"
 val equipmentOpen=expanded[equipmentOpenKey]==true
 val capabilitiesOpen=expanded[capabilitiesOpenKey]==true

 HText(when(selected){
  "gym"->"Selecciona solo el equipamiento que tengas disponible. Lo demás es opcional."
  "db"->"Indica el peso de tus mancuernas; agrega accesorios solo si los tienes."
  else->"Con tu peso corporal basta para empezar. Los accesorios son opcionales."
 },size=15,color=LocalHunter.current.muted)

 // Este dato se usa para adaptar las cargas. No se esconde en el modo mancuernas.
 if(isDumbbell){
  HInput(values["load"]?:"0",{values["load"]=it},
   "Peso de una mancuerna · kg",number=true)
 }
 HButton(
  (when(selected){
   "gym"->"Equipo y accesorios de gimnasio"
   "db"->"Complementos para mancuernas"
   else->"Accesorios para peso corporal"
  })+" · $equipmentCount elegidos "+(if(equipmentOpen)"▴" else "▾"),
  Modifier.fillMaxWidth()
 ){expanded[equipmentOpenKey]=!equipmentOpen}

 if(equipmentOpen){
  if(isGym){
   HText("Máquinas y estaciones disponibles",size=16,bold=true)
   for(group in gymGroups){
    val id="gym-$selected-${group.title}"
    val count=group.ids.count{values[it]=="true"}
    HButton("${group.title} · $count elegidos "+(if(expanded[id]==true)"▴" else "▾"),
     Modifier.fillMaxWidth()){expanded[id]=expanded[id]!=true}
    if(expanded[id]==true)for(key in group.ids)
     ProfileEquipmentCheck(values,key,gymLabels[key]?:key)
   }
   HInput(values["load"]?:"0",{values["load"]=it},
    "Peso de mancuerna disponible · kg (opcional)",number=true)
  }

  HText("Accesorios habituales (opcional)",size=16,bold=true)
  if(isDumbbell)
   ProfileEquipmentCheck(values,"bench","Banco de entrenamiento")
  ProfileEquipmentCheck(values,"step","Escalón o plataforma baja")
  for(key in quickMaterials){
   if(key in allowedMaterials)
    ProfileEquipmentCheck(values,"materials46|"+key,catalog.materials.s(key,key))
  }

  // Todas las claves antiguas siguen configurables; ninguna se marca automáticamente.
  val extraMaterials=allowedMaterials-quickMaterials.toSet()
  HButton("Otros accesorios · $extrasCount elegidos "+
    (if(expanded[accessoryOpenKey]==true)"▴" else "▾"),
    Modifier.fillMaxWidth()){
    expanded[accessoryOpenKey]=expanded[accessoryOpenKey]!=true
  }
  if(expanded[accessoryOpenKey]==true){
   for(group in materialGroups){
    val choices=group.ids.filter{it in extraMaterials}
    if(choices.isEmpty())continue
    val id="material-$selected-${group.title}"
    val count=choices.count{values["materials46|"+it]=="true"}
    HButton("${group.title} · $count elegidos "+
      (if(expanded[id]==true)"▴" else "▾"),
      Modifier.fillMaxWidth()){expanded[id]=expanded[id]!=true}
    if(expanded[id]==true)for(key in choices)
     ProfileEquipmentCheck(values,"materials46|"+key,catalog.materials.s(key,key))
   }
  }
 }

 HButton("Habilidades específicas (opcional) · ${chosenCapabilities.size} elegidas "+
   (if(capabilitiesOpen)"▴" else "▾"),
   Modifier.fillMaxWidth()){
   expanded[capabilitiesOpenKey]=!capabilitiesOpen
 }
 if(capabilitiesOpen){
  HText("Marca únicamente movimientos que ya puedes realizar con control. "+
   "No se deducen de tu nivel de entrenamiento.",size=14,color=LocalHunter.current.muted)
  for(key in quickCapabilities){
   if(key in allowedCapabilities)
    ProfileEquipmentCheck(values,"capabilities46|"+key,
     catalog.capabilities.s(key,key))
  }
  val extraAllowed=allowedCapabilities-quickCapabilities.toSet()
  for(group in capabilityGroups){
   val choices=group.ids.filter{it in extraAllowed}
   if(choices.isEmpty())continue
   val id="capability-$selected-${group.title}"
   val count=choices.count{values["capabilities46|"+it]=="true"}
   HButton("${group.title} · $count elegidas "+
     (if(expanded[id]==true)"▴" else "▾"),
     Modifier.fillMaxWidth()){expanded[id]=expanded[id]!=true}
   if(expanded[id]==true)for(key in choices)
    ProfileEquipmentCheck(values,"capabilities46|"+key,
     catalog.capabilities.s(key,key))
  }
 }
}
