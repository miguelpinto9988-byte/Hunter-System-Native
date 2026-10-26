package com.huntersystem.app

import android.content.Context
import org.json.JSONObject
import org.json.JSONArray

class Exercise(val raw:JSONObject){
 val id=raw.s("id");val name=raw.s("name");val group=raw.s("group");val category=raw.s("category");val unit=raw.s("unit");val equipment=raw.s("equipment");val meta=raw.o("planning49");val type=meta.s("group");val family=meta.s("family",id);val oldFamily=raw.s("baseId",raw.s("family",id));val pattern=meta.s("pattern");val role=meta.s("role");val primary=meta.b("primary");val power=meta.b("power");val impact=meta.b("impact");val planningLevel=meta.i("level");val gym=raw.b("gymOnly23");val sides=raw.b("sides");val demo="demos-rive/$id.riv"
}
class Catalog(val context:Context?=null,val loader:((String)->String)?=null){
 fun text(path:String)=loader?.invoke(path)?:context!!.assets.open(path).bufferedReader().use{it.readText()}
 val originalExercises=JSONArray(text("catalog.json")).objects().map(::Exercise)
 val expansionExercises=runCatching{JSONArray(text("catalog-hunter2000-extra.json")).objects().map(::Exercise)}.getOrElse{emptyList()}
 val exercises=originalExercises+expansionExercises
 val byId=exercises.associateBy{it.id}
 val materials=JSONObject(text("materials.json"));val capabilities=JSONObject(text("capabilities.json"))
 val routines=JSONArray(text("routines.json")).objects();val editorial=JSONObject(text("editorial.json"));val design=JSONObject(text("design.json"));val defaultState=JSONObject(text("default-state.json"));val profileForm=JSONArray(text("profile-form.json"))
 init{require(originalExercises.size==1200&&originalExercises.map{it.id}.toSet().size==1200);require(expansionExercises.isEmpty()||expansionExercises.size==800);require(byId.size==exercises.size)}
}
