package com.huntersystem.app

import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Los desafíos especiales no modifican las misiones ni las series diarias. */
data class MonarchMissionGoal(val exerciseId:String,val target:Int)
data class MonarchMissionSpec(
 val id:String,val title:String,val story:String,val minutes:Int,val cooldownDays:Long,
 val minimumLevel:Int,val goals:List<MonarchMissionGoal>
)
object MonarchMissionCatalog{
 val missions=listOf(
  MonarchMissionSpec("guardia","Guardia del Umbral",
   "Desafío intenso de entrada. Prioriza el control y haz pausas.",
   30,4,1,listOf(MonarchMissionGoal("squat",40),MonarchMissionGoal("jumpjack",60),MonarchMissionGoal("plank",45))),
  MonarchMissionSpec("fortaleza","Asalto a la Fortaleza",
   "Prueba de resistencia voluntaria para cazadores avanzados.",
   45,4,2,listOf(MonarchMissionGoal("squat",60),MonarchMissionGoal("new46-burpee",20),MonarchMissionGoal("reverselunge",24))),
  MonarchMissionSpec("trono","Juicio del Trono Monarca",
   "Desafío extremo: 100 sentadillas, 50 burpees y 30 flexiones. Solo para personas preparadas.",
   60,7,2,listOf(MonarchMissionGoal("squat",100),MonarchMissionGoal("new46-burpee",50),MonarchMissionGoal("pushup",30)))
 )
 fun spec(id:String)=missions.firstOrNull{it.id==id}?:error("Misión Monarca desconocida")
 fun status(run:JSONObject?,now:Long):String{
  if(run==null)return "none"
  return if(run.s("status")=="active"&&now>=run.optLong("deadlineAt"))"expired" else run.s("status")
 }
 fun progress(run:JSONObject?,mission:MonarchMissionSpec):Float{
  if(run==null)return 0f
  val counts=run.o("counts")
  val total=mission.goals.sumOf{it.target}.coerceAtLeast(1)
  val done=mission.goals.sumOf{counts.i(it.exerciseId).coerceIn(0,it.target)}
  return (done.toFloat()/total).coerceIn(0f,1f)
 }
 fun remaining(run:JSONObject,now:Long)=maxOf(0L,run.optLong("deadlineAt")-now)
 fun cooldownEnd(run:JSONObject?,mission:MonarchMissionSpec):Long{
  if(run==null)return 0L
  val prev=spec(run.s("id"))
  val end=if(status(run,Long.MAX_VALUE)=="expired")run.optLong("deadlineAt")
   else run.optLong("finishedAt",run.optLong("deadlineAt"))
  return end+TimeUnit.DAYS.toMillis(maxOf(prev.cooldownDays,mission.cooldownDays))
 }
 fun exerciseCompatible(store:NativeStore,exerciseId:String):Boolean{
  val p=store.adjusted()
  val e=store.catalog.byId[exerciseId]?:return false
  return store.profile?.n("age",0.0)?.let{it>=18}==true &&
    !store.rules.active(p)&&store.rules.gate(p).ok &&
    !e.raw.b("sourceExpansion2k")&&store.rules.compatible(e,p)
 }
 fun eligibleReason(store:NativeStore,mission:MonarchMissionSpec,now:Long):String?{
  val profile=store.profile?:return "Completa tu perfil antes de acceder a desafíos intensos."
  val adjusted=store.adjusted()
  if(profile.n("age")<18)return "Disponible únicamente para personas adultas."
  if(level(adjusted)<mission.minimumLevel)return "Necesitas nivel "+(if(mission.minimumLevel==2)"avanzado" else "intermedio")+"."
  if(store.rules.active(adjusted)||!store.rules.gate(adjusted).ok)
   return "Tu perfil indica lesiones, dolor o limitaciones. Revisa tu actividad con un profesional."
  val current=store.state.optJSONObject("monarchSpecialMission")
  if(current!=null){
   if(status(current,now)=="active")return "Ya hay una misión especial en curso."
   val ready=cooldownEnd(current,mission)
   if(now<ready)return "Recuperación pendiente: aproximadamente "+((ready-now+3599999)/3600000)+" h."
  }
  return if(mission.goals.any{!exerciseCompatible(store,it.exerciseId)})
   "Hay ejercicios incompatibles con tu perfil o capacidades. Revisa Perfil." else null
 }
}

fun NativeStore.startMonarchMission(id:String,now:Long=System.currentTimeMillis()){
 val mission=MonarchMissionCatalog.spec(id)
 val reason=MonarchMissionCatalog.eligibleReason(this,mission,now)
 require(reason==null){reason?:"Misión no disponible"}
 state.optJSONObject("monarchSpecialMission")?.let{prev->
  val history=state.a("monarchSpecialHistory").objects().toMutableList()
  history+=prev.copyJson()
  state.put("monarchSpecialHistory",arr(history.takeLast(50)))
 }
 state.put("monarchSpecialMission",obj(
  "id" to id,"status" to "active","startedAt" to now,
  "deadlineAt" to (now+TimeUnit.MINUTES.toMillis(mission.minutes.toLong())),
  "counts" to JSONObject()))
 save()
}
fun NativeStore.recordMonarchProgress(exerciseId:String,increment:Int,now:Long=System.currentTimeMillis()){
 require(increment in setOf(-10,-5,-1,1,5,10)){"Incremento no permitido"}
 val run=state.optJSONObject("monarchSpecialMission")?:error("No hay desafío activo")
 require(MonarchMissionCatalog.status(run,now)=="active"){"El tiempo ha expirado o el desafío está cerrado"}
 val goal=MonarchMissionCatalog.spec(run.s("id")).goals.firstOrNull{it.exerciseId==exerciseId}
  ?:error("Ejercicio fuera del desafío")
 require(MonarchMissionCatalog.exerciseCompatible(this,exerciseId)){"Revisa las restricciones de tu perfil"}
 val counts=run.o("counts")
 counts.put(exerciseId,(counts.i(exerciseId)+increment).coerceIn(0,goal.target))
 run.put("counts",counts)
 save()
}
fun NativeStore.completeMonarchMission(now:Long=System.currentTimeMillis()){
 val run=state.optJSONObject("monarchSpecialMission")?:error("No hay misión Monarca")
 require(MonarchMissionCatalog.status(run,now)=="active"){"La misión ya terminó"}
 val mission=MonarchMissionCatalog.spec(run.s("id"))
 require(mission.goals.all{run.o("counts").i(it.exerciseId)>=it.target}){"Aún faltan objetivos"}
 require(mission.goals.all{MonarchMissionCatalog.exerciseCompatible(this,it.exerciseId)}){"Revisa las restricciones actuales"}
 run.put("status","completed")
 run.put("finishedAt",now)
 run.put("seal","Sello Monarca: "+mission.title)
 run.put("selfReported",true)
 save()
}
fun NativeStore.stopMonarchMission(now:Long=System.currentTimeMillis()){
 val run=state.optJSONObject("monarchSpecialMission")?:error("No hay misión Monarca")
 require(MonarchMissionCatalog.status(run,now)=="active"){"La misión ya terminó"}
 run.put("status","stopped")
 run.put("finishedAt",now)
 save()
}