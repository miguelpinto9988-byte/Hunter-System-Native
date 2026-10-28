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
@Composable fun ProfileEquipmentOptions(
 equipment:String,
 values:MutableMap<String,String>,
 expanded:MutableMap<String,Boolean>,
 catalog:Catalog
){
 val quickMaterials=when(equipment){
  "db"->listOf("mat","space","band","chair","pullbar")
  "gym"->listOf("mat","space","band")
  else->listOf("space","mat","chair","wall","band","pullbar")
 }
 HText(when(equipment){
  "gym"->"Elige solo las máquinas y accesorios disponibles en tu gimnasio."
  "db"->"Entrenas con mancuernas. Marca los accesorios que realmente tienes."
  else->"Entrenas con tu peso corporal. No necesitas máquinas."
 },size=15,color=LocalHunter.current.muted)
 if(equipment=="db"||equipment=="gym"){
  HInput(values["load"]?:"0",{values["load"]=it},"Peso de una mancuerna · kg",number=true)
 }
 if(equipment=="db")ProfileEquipmentCheck(values,"bench","Banco de entrenamiento disponible")
 ProfileEquipmentCheck(values,"step","Escalón o plataforma baja disponible")
 if(equipment=="gym"){
  HText("Equipo de gimnasio",size=16,bold=true)
  for(group in gymGroups){
   val selected=group.ids.count{values[it]=="true"}
   val id="gym-section-"+group.title
   HButton(group.title+" · "+selected+" seleccionados ▾",Modifier.fillMaxWidth()){expanded[id]=expanded[id]!=true}
   if(expanded[id]==true)for(key in group.ids)ProfileEquipmentCheck(values,key,gymLabels[key]?:key)
  }
 }
 HText("Accesorios disponibles (opcional)",size=16,bold=true)
 for(key in quickMaterials)ProfileEquipmentCheck(values,"materials46|"+key,catalog.materials.s(key,key))
 val extras=profileAllowedMaterials(equipment)-quickMaterials.toSet()
 HButton("Más accesorios (opcional) ▾",Modifier.fillMaxWidth()){expanded["extra-materials"]=expanded["extra-materials"]!=true}
 if(expanded["extra-materials"]==true){
  for(group in materialGroups){
   val choices=group.ids.filter{it in extras}
   if(choices.isEmpty())continue
   val id="material-section-"+group.title
   val n=choices.count{values["materials46|"+it]=="true"}
   HButton(group.title+" · "+n+" ▾",Modifier.fillMaxWidth()){expanded[id]=expanded[id]!=true}
   if(expanded[id]==true)for(key in choices)ProfileEquipmentCheck(values,"materials46|"+key,catalog.materials.s(key,key))
  }
 }
 HText("Lo que puedes hacer con control (opcional)",size=16,bold=true)
 HText("Marca solo capacidades que ya realizas con seguridad; no se deducen de tu nivel.",size=14,color=LocalHunter.current.muted)
 val quickCaps=when(equipment){
  "db"->listOf("singlebalance","hanging","overhead")
  "gym"->listOf("singlebalance","hanging","overhead")
  else->listOf("singlebalance","hanging","landing")
 }
 for(key in quickCaps)ProfileEquipmentCheck(values,"capabilities46|"+key,catalog.capabilities.s(key,key))
 HButton("Otras habilidades (avanzado) ▾",Modifier.fillMaxWidth()){expanded["extra-capabilities"]=expanded["extra-capabilities"]!=true}
 if(expanded["extra-capabilities"]==true){
  val allowed=profileAllowedCapabilities(equipment)-quickCaps.toSet()
  for(group in capabilityGroups){
   val choices=group.ids.filter{it in allowed}
   if(choices.isEmpty())continue
   val id="capability-section-"+group.title
   val n=choices.count{values["capabilities46|"+it]=="true"}
   HButton(group.title+" · "+n+" ▾",Modifier.fillMaxWidth()){expanded[id]=expanded[id]!=true}
   if(expanded[id]==true)for(key in choices)ProfileEquipmentCheck(values,"capabilities46|"+key,catalog.capabilities.s(key,key))
  }
 }
}
