package com.huntersystem.app

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/**
 * Renderer único de campos: mantiene los nombres y valores del formulario V43.12.
 * Las tres etapas reorganizan su presentación, nunca los datos que se guardan.
 */
@Composable
internal fun HunterProfileFields(
 section:JSONObject,
 allowed:Set<String>,
 values:MutableMap<String,String>,
 expanded:MutableMap<String,Boolean>,
 store:NativeStore,
 onManualGoalGroup:()->Unit
){
 val done=mutableSetOf<String>()
 for(field in section.a("fields").objects()){
  val name=field.s("name")
  if(name !in allowed)continue
  val type=field.s("type")
  val label=when(name){
   "name"->"Nombre de cazador"
   "age"->"Edad"
   "height"->"Altura cm"
   "weight"->"Peso kg"
   "difficulty"->"Nivel de entrenamiento"
   "goalGroup"->"Objetivo principal"
   "finalGoal"->"Mi meta personal"
   "equipment"->"Equipo"
   "gender"->"Género"
   else->field.s("label").substringBefore("Seleccionado").trim()
  }
  if(name in setOf("age","height","weight")){
   if(name!="age")continue
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    for(key in listOf("age","height","weight"))HInput(
     values[key]?:"",{values[key]=it},
     when(key){"age"->"Edad";"height"->"Altura cm";else->"Peso kg"},
     Modifier.weight(1f),number=true)
   }
   continue
  }
  if(name=="r_noImpact")HButton("Restricciones e indicaciones adicionales ▾",
   Modifier.fillMaxWidth()){expanded["restrictions"]=expanded["restrictions"]!=true}
  if((name.startsWith("r_")||name in listOf("recommendations","approved","translated"))
    &&expanded["restrictions"]!=true)continue

  when {
   name=="equipment"->{
    HSelect(label,values[name]?:"body",
     field.a("options").objects().map{it.s("value") to it.s("label")}){values[name]=it}
    ProfileEquipmentOptions(values[name]?:"body",values,expanded,store.catalog)
   }
   type=="radio"->{
    if(!done.add(name))continue
    val options=section.a("fields").objects().filter{it.s("name")==name}
      .map{it.s("value") to when(it.s("value")){
        "challenges"->"Desafíos diarios"
        "routines"->"Rutinas completas"
        else->it.s("label")
      }}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
     options.forEach{(choice,text)->
      HButton(text,Modifier.weight(1f),primary=values[name]==choice){values[name]=choice}
     }
    }
   }
   type=="checkbox"->HCheck(label,values[name]=="true"){
    values[name]=(values[name]!="true").toString()
   }
   type=="select"->HSelect(label,values[name]?:"",
    field.a("options").objects().map{it.s("value") to it.s("label")}){v->
    values[name]=v
    if(name=="goalGroup")onManualGoalGroup()
    if(name=="difficulty")values["restSeconds"]=mapOf(
     "basic" to "30","intermediate" to "60","advanced" to "90")[v]?:"30"
   }
   else->HInput(values[name]?:"",{values[name]=it},label,
    number=type=="number",multiline=type=="textarea")
  }
  if(name=="finalGoal"&&!values[name].isNullOrBlank()){
   val result=store.goals.interpret(values[name]?:"",values["goalGroup"]?:"habit")
   val group=store.catalog.editorial.o("goalGroups").o(result.s("primary"))
   HNotice(group.s("name","Constancia y bienestar")+" · "+
    store.catalog.editorial.o("goalPlans").o(result.s("target")).s("why"))
  }
 }
}
