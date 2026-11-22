package com.huntersystem.app

import android.content.Context
import com.huntersystem.app.planning.Category
import org.json.JSONObject
import org.json.JSONArray

private val catalogEquipmentLabels = mapOf(
 "gym" to "Gimnasio", "machine" to "Máquinas guiadas", "smith" to "Máquina Smith",
 "cardio" to "Máquinas de cardio", "partner" to "Compañero de entrenamiento"
)

// Ejercicios base retirados según la auditoría de rendimiento. Las variantes heredan la decisión.
private val withdrawnExerciseFamilies = setOf(
 "balance", "breathing", "childpose", "elbowmob", "fingers", "forearmmob", "hamstretch", "heel",
 "heelcontrolled", "heeltoe", "hs379-isobiceps", "hs379-palms", "hs379-scapular",
 "hs379-shoulderset", "latstretch", "legextend", "legextend-control18", "linewalk", "march",
 "neck", "new46-benchprayer", "new46-bentcalfstretch", "new46-bigtoestretch", "new46-butterfly",
 "new46-chairthoracic", "new46-chinretract", "new46-couchstretch", "new46-crossshoulder",
 "new46-doorpec", "new46-foampec", "new46-frogrock", "new46-hip9090lift", "new46-hipcapsule",
 "new46-mediannerve", "new46-pelvicclock", "new46-pelvictilt", "new46-peronealnerve",
 "new46-pronehipcontrol", "new46-pronequad", "new46-quadthoracic", "new46-scapcar",
 "new46-sciaticnerve", "new46-seatedhipir", "new46-shortfoot", "new46-sleeper",
 "new46-stickextension", "new46-tendonglides", "new46-thoracicroller", "new46-thumbopposition",
 "new46-toetowel", "new46-toeyoga", "new46-tricepsstretch", "new46-ulnarnerve", "pau-armcircle-b",
 "pau-armcircle-f", "pau-armcurl", "pau-armfly", "pau-armpulses", "pau-armraise", "pau-armsback",
 "pau-armupdown", "pau-armworld", "seatedcalf", "seatedheel", "seatedhip", "seatedmarch",
 "seatedmarchinterval", "seatedpunch", "seatedreach", "seatedside", "seatedstep", "seatedturn",
 "shoulder", "sidebendstand", "slowmarch", "standingcore", "supinearms", "tandem", "toelift",
 "wristmob",
)

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
 private val materialKeys = (raw.a("materialRequirements").strings() +
  raw.a("materials46").strings() + raw.a("requirements").strings() + gymRequirements).toSet()
 internal val equipmentKeys = materialKeys - "body" +
  (if(equipment=="db") setOf("db") else emptySet()) +
  (if(gym||equipment=="gym") setOf("gym") else emptySet())
 val cardioEquipment=when{
  id.startsWith("gym-bike")||id.startsWith("gym-airbike")||id.startsWith("gym-recumbent")->"bike"
  id.startsWith("gym-row")->"rower"
  id.startsWith("gym-elliptical")->"elliptical"
  else->null
 }
}
class Catalog(val context:Context?=null,val loader:((String)->String)?=null){
 fun text(path:String)=loader?.invoke(path)?:context!!.assets.open(path).bufferedReader().use{it.readText()}
 val originalExercises=JSONArray(text("catalog.json")).objects().map(::Exercise)
 private val originalById=originalExercises.associateBy{it.id}
 internal fun originalExercise(id:String)=originalById[id]
 internal val libraryExercises=originalExercises.filterNot {
  it.oldFamily in withdrawnExerciseFamilies || it.isRedundantCardioVariant
 }
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
  normalizedQuery in normalize(exercise.name)||
   namesByExercise[exercise.id].orEmpty().any{normalizedQuery in it}
 internal val historicalExercises=sourceExercises.filterNot{it.id in aliases}
 val byId=sourceById+aliases.mapValues{(_,canonical)->sourceById.getValue(canonical)}
 private val classifications=historicalExercises.mapNotNull{exercise->
  ExerciseTaxonomy.classify(exercise,byId::get)?.let{exercise.id to it}
 }.toMap()
 val exercises=libraryExercises.filter{it.id in classifications}
 internal val automaticExercises=exercises.filter { exercise ->
  val category=classifications.getValue(exercise.id).category
  category==Category.MOBILITY || exercise.isMainTraining(category)
 }
 private val automaticExerciseIds=automaticExercises.mapTo(hashSetOf()){it.id}
 private fun isTrainingExercise(id:String)=byId[id]?.id?.let(automaticExerciseIds::contains)==true
 internal fun isWithdrawn(id:String)=
  originalById[id]?.oldFamily in withdrawnExerciseFamilies || byId[id]?.oldFamily in withdrawnExerciseFamilies
 internal fun classification(id:String)=byId[id]?.id?.let(classifications::get)
 val materials=JSONObject(text("materials.json"));val capabilities=JSONObject(text("capabilities.json"))
 val routines=JSONArray(text("routines.json")).objects().filter{routine->
  routine.a("items").objects().all{classification(it.s("id"))!=null}
 }
 val trainingRoutines=routines.filter{routine->
  routine.a("items").objects().all{isTrainingExercise(it.s("id"))} &&
   routine.o("sourceSequence").a("steps").objects().all{step->
    step.s("kind")!="exercise"||isTrainingExercise(step.s("exerciseId"))
   }
 }
 val editorial=JSONObject(text("editorial.json"));val design=JSONObject(text("design.json"));val defaultState=JSONObject(text("default-state.json"));val profileForm=JSONArray(text("profile-form.json"))
 internal fun equipmentLabel(key:String)=catalogEquipmentLabels[key] ?: materials.s(key,key)
 internal val originalEquipmentOptions by lazy {
  libraryExercises.flatMap{it.equipmentKeys}.distinct()
   .map{it to equipmentLabel(it)}.sortedBy{normalize(it.second)}
 }
 init{require(originalExercises.size==1200&&originalExercises.map{it.id}.toSet().size==1200);require(expansionExercises.isEmpty()||expansionExercises.size==800);require(sourceById.size==sourceExercises.size);require(aliases.all{(id,target)->id in sourceById&&target in sourceById&&target !in aliases})}
}
