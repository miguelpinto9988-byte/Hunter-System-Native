package com.huntersystem.app

import android.content.Context
import org.json.JSONObject
import org.json.JSONArray

class Exercise(val raw:JSONObject){
 val id=raw.s("id");val name=raw.s("name");val group=raw.s("group");val category=raw.s("category");val unit=raw.s("unit");val equipment=raw.s("equipment");val meta=raw.o("planning49");val type=meta.s("group");val family=meta.s("family",id);val oldFamily=raw.s("baseId",raw.s("family",id));val pattern=meta.s("pattern");val role=meta.s("role");val primary=meta.b("primary");val power=meta.b("power");val impact=meta.b("impact");val planningLevel=meta.i("level");val sides=raw.b("sides");val sport=raw.s("sport")
 val hunter2000=raw.b("sourceExpansion2k")
 val gym=raw.b("gymOnly23")||hunter2000&&(equipment=="gym"||
  raw.a("materialRequirements").strings().any{it in setOf("barbell","trapbar","safetybar","cable","machine","smith")})
 val validationBlocked=!hunter2000&&(raw.b("pendingTechnicalReview")||raw.b("sourceOnly"))
 val isBaseVariant=raw.s("variant")=="base"||hunter2000&&raw.s("variant")=="pending_review"
 val gymRequirements=if(hunter2000)
  raw.a("materialRequirements").strings().filter{it in setOf("barbell","cable","machine","smith","cardio","bench")}
 else raw.a("gymRequirements23").strings()
 val cardioEquipment=when{
  id.startsWith("gym-bike")||id.startsWith("gym-airbike")||id.startsWith("gym-recumbent")->"bike"
  id.startsWith("gym-row")->"rower"
  id.startsWith("gym-elliptical")->"elliptical"
  else->null
 }
 val demo=if(hunter2000||sport.isNotBlank()||id.startsWith("new46-")||id.startsWith("hs379-")) "" else "media2d/$id.svg"
}
class Catalog(val context:Context?=null,val loader:((String)->String)?=null){
 fun text(path:String)=loader?.invoke(path)?:context!!.assets.open(path).bufferedReader().use{it.readText()}
 val originalExercises=JSONArray(text("catalog.json")).objects().map(::Exercise)
 val expansionExercises=runCatching{JSONArray(text("catalog-hunter2000-extra.json")).objects().map(::Exercise)}.getOrElse{emptyList()}
 val sportExercises=JSONArray(text("catalog-sports.json")).objects().map(::Exercise)
 private val sourceExercises=originalExercises+expansionExercises+sportExercises
 private val sourceById=sourceExercises.associateBy{it.id}
 private val aliases=JSONObject(text("exercise-aliases.json")).entries().associate{(id,value)->
  id to (value as? String ?: error("Alias inválido: $id"))
 }
 private val namesByExercise=sourceExercises.groupBy(
  { aliases[it.id] ?: it.id }, { normalize(it.name) }
 )
 fun matchesExercise(exercise:Exercise,normalizedQuery:String)=
  namesByExercise[exercise.id].orEmpty().any{normalizedQuery in it}
 val exercises=sourceExercises.filterNot{it.id in aliases}
 val byId=sourceById+aliases.mapValues{(_,canonical)->sourceById.getValue(canonical)}
 val materials=JSONObject(text("materials.json"));val capabilities=JSONObject(text("capabilities.json"))
 val routines=JSONArray(text("routines.json")).objects();val editorial=JSONObject(text("editorial.json"));val design=JSONObject(text("design.json"));val defaultState=JSONObject(text("default-state.json"));val profileForm=JSONArray(text("profile-form.json"))
 init{require(originalExercises.size==1200&&originalExercises.map{it.id}.toSet().size==1200);require(expansionExercises.isEmpty()||expansionExercises.size==800);require(sourceById.size==sourceExercises.size);require(aliases.all{(id,target)->id in sourceById&&target in sourceById&&target !in aliases})}
}
