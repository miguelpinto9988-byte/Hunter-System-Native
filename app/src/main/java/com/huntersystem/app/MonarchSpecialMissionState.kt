package com.huntersystem.app

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Objetivos acumulados durante la semana: nunca son series verificadas ni una dosis diaria. */
data class MonarchMissionGoal(val exerciseId:String,val target:Int)
data class MonarchMissionSpec(
 val id:String,val title:String,val story:String,val weekStart:String,val cycleWeek:Int,
 val deadlineAt:Long,val minimumLevel:Int,val goals:List<MonarchMissionGoal>
)

object MonarchMissionCatalog {
 private val anchor=LocalDate.of(2026,10,25) // domingo: comienzo del ciclo de cuatro semanas
 private val names=mapOf(
  "guardia" to "Guardia del Umbral",
  "fortaleza" to "Asalto a la Fortaleza",
  "trono" to "Juicio del Trono Monarca"
 )
 private val stories=mapOf(
  "guardia" to "Resistencia y disciplina. Objetivos acumulativos a lo largo de la semana.",
  "fortaleza" to "Prueba avanzada de fuerza y resistencia repartida en varios días.",
  "trono" to "Desafío excepcional de alto volumen. No se recomienda completarlo en una sola sesión."
 )
 fun sunday(date:LocalDate):LocalDate=date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY))
 fun weekStart(now:Long,zone:ZoneId=ZoneId.systemDefault())=
  sunday(Instant.ofEpochMilli(now).atZone(zone).toLocalDate())
 fun deadline(start:LocalDate,zone:ZoneId=ZoneId.systemDefault())=
  start.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
 fun cycleIndex(start:LocalDate)=Math.floorMod(ChronoUnit.WEEKS.between(anchor,sunday(start)).toInt(),4)
 fun missionFor(date:LocalDate,level:Int,zone:ZoneId=ZoneId.systemDefault()):MonarchMissionSpec {
  val start=sunday(date)
  val weeks=ChronoUnit.WEEKS.between(anchor,start)
  val cycle=cycleIndex(start)
  // Los intermedios solo reciben Guardia; los avanzados alternan tres exigencias.
  val id=if(level<2)"guardia" else listOf("guardia","fortaleza","trono","fortaleza")[cycle]
  // En ciclos sucesivos rotan los ejercicios complementarios, no las metas mínimas.
  val variant=Math.floorMod(weeks.toInt(),4)
  val goals=when(id){
   "guardia"->listOf(
    MonarchMissionGoal("squat",60),
    listOf(MonarchMissionGoal("jumpjack",90),MonarchMissionGoal("mountain",70),
      MonarchMissionGoal("jumpjack",110),MonarchMissionGoal("mountain",85))[variant],
    listOf(MonarchMissionGoal("plank",90),MonarchMissionGoal("kneelingplank",110),
      MonarchMissionGoal("plank",120),MonarchMissionGoal("wallplank",150))[variant])
   "fortaleza"->listOf(
    MonarchMissionGoal("squat",80),MonarchMissionGoal("new46-burpee",35),
    listOf(MonarchMissionGoal("reverselunge",40),MonarchMissionGoal("stepup",46),
      MonarchMissionGoal("lateralunge",40),MonarchMissionGoal("reverselunge",50))[variant],
    MonarchMissionGoal("plank",120))
   else->listOf(
    MonarchMissionGoal("squat",120),MonarchMissionGoal("new46-burpee",60),
    MonarchMissionGoal("pushup",70),
    listOf(MonarchMissionGoal("plank",180),MonarchMissionGoal("mountain",130),
      MonarchMissionGoal("jumpjack",150),MonarchMissionGoal("kneelingplank",210))[variant])
  }
  return MonarchMissionSpec(id,names.getValue(id),stories.getValue(id),
   start.toString(),cycle+1,deadline(start,zone),if(id=="guardia")1 else 2,goals)
 }
 fun fromRun(run:JSONObject):MonarchMissionSpec {
  val id=run.s("id")
  val goals=run.a("goals").objects().map{MonarchMissionGoal(it.s("exerciseId"),it.i("target"))}
  require(id in names&&goals.isNotEmpty()&&goals.all{it.target>0}){"Datos de misión Monarca incompletos"}
  return MonarchMissionSpec(id,names.getValue(id),stories.getValue(id),
   run.s("weekStart"),run.i("cycleWeek",1),run.optLong("deadlineAt"),
   if(id=="guardia")1 else 2,goals)
 }
 fun status(run:JSONObject?,now:Long):String {
  if(run==null)return "none"
  return if(run.s("status")=="active"&&now>=run.optLong("deadlineAt"))"expired" else run.s("status")
 }
 fun progress(run:JSONObject?,mission:MonarchMissionSpec):Float {
  if(run==null)return 0f
  val counts=run.o("counts")
  val total=mission.goals.sumOf{it.target}.coerceAtLeast(1)
  val done=mission.goals.sumOf{counts.i(it.exerciseId).coerceIn(0,it.target)}
  return (done.toFloat()/total).coerceIn(0f,1f)
 }
 fun remaining(run:JSONObject,now:Long)=maxOf(0L,run.optLong("deadlineAt")-now)
 fun exerciseCompatible(store:NativeStore,exerciseId:String):Boolean {
  val p=store.adjusted()
  val e=store.catalog.byId[exerciseId]?:return false
  return store.profile?.n("age",0.0)?.let{it>=18}==true &&
   !store.rules.active(p)&&store.rules.gate(p).ok &&
   !e.raw.b("sourceExpansion2k")&&store.rules.compatible(e,p)
 }
 fun eligibleReason(store:NativeStore,mission:MonarchMissionSpec,now:Long):String? {
  val profile=store.profile?:return "Completa tu perfil para acceder a desafíos exigentes."
  val p=store.adjusted()
  if(profile.n("age")<18)return "Los desafíos Monarca están reservados a personas adultas."
  if(level(p)<mission.minimumLevel)return "Tu perfil necesita nivel "+(if(mission.minimumLevel==2)"avanzado" else "intermedio")+"."
  if(store.rules.active(p)||!store.rules.gate(p).ok)
   return "Hay dolor, lesiones o limitaciones registrados. Esta misión no está disponible."
  val run=store.state.optJSONObject("monarchSpecialMission")
  if(run!=null&&run.s("weekStart")==mission.weekStart)
   return if(status(run,now)=="active")"Ya has iniciado el desafío de esta semana." else "Esta semana ya se ha utilizado. Nuevo desafío el próximo domingo."
  if(mission.goals.any{!exerciseCompatible(store,it.exerciseId)})
   return "Alguno de los ejercicios exige capacidades o condiciones distintas. Revisa tu perfil."
  return null
 }
}

/** Renueva la oferta cada domingo al abrir o mantener abierta la app, sin servicio remoto. */
fun NativeStore.syncMonarchWeek(now:Long=System.currentTimeMillis(),zone:ZoneId=ZoneId.systemDefault()) {
 val start=MonarchMissionCatalog.weekStart(now,zone).toString()
 val run=state.optJSONObject("monarchSpecialMission")
 if(run!=null&&run.s("weekStart")!=start) {
  val archived=run.copyJson()
  if(MonarchMissionCatalog.status(archived,now)=="expired")archived.put("status","expired")
  state.put("monarchSpecialHistory",arr((state.a("monarchSpecialHistory").objects()+
   archived).takeLast(100)))
  state.remove("monarchSpecialMission")
  state.put("monarchSpecialWeek",start)
  save()
 }else if(state.s("monarchSpecialWeek")!=start) {
  state.put("monarchSpecialWeek",start)
  save()
 }
}
/** Solo un intento voluntario por semana; se conserva el progreso y el historial. */
fun NativeStore.startMonarchMission(now:Long=System.currentTimeMillis(),zone:ZoneId=ZoneId.systemDefault()) {
 syncMonarchWeek(now,zone)
 val spec=MonarchMissionCatalog.missionFor(MonarchMissionCatalog.weekStart(now,zone),
  level(adjusted()),zone)
 val reason=MonarchMissionCatalog.eligibleReason(this,spec,now)
 require(reason==null){reason?:"Misión no disponible"}
 state.put("monarchSpecialMission",obj(
  "id" to spec.id,"weekStart" to spec.weekStart,"cycleWeek" to spec.cycleWeek,
  "status" to "active","startedAt" to now,"deadlineAt" to spec.deadlineAt,
  "goals" to arr(spec.goals.map{obj("exerciseId" to it.exerciseId,"target" to it.target)}),
  "counts" to JSONObject()))
 save()
}
private fun NativeStore.checkedMonarchRun(now:Long,zone:ZoneId):JSONObject {
 val run=state.optJSONObject("monarchSpecialMission")?:error("No hay desafío iniciado")
 require(run.s("weekStart")==MonarchMissionCatalog.weekStart(now,zone).toString() &&
  MonarchMissionCatalog.status(run,now)=="active"){"La misión semanal ya terminó"}
 return run
}
fun NativeStore.recordMonarchProgress(
 exerciseId:String,increment:Int,now:Long=System.currentTimeMillis(),zone:ZoneId=ZoneId.systemDefault()
) {
 require(increment in setOf(-10,-5,-1,1,5,10)){"Incremento no permitido"}
 val run=checkedMonarchRun(now,zone)
 val goal=MonarchMissionCatalog.fromRun(run).goals.firstOrNull{it.exerciseId==exerciseId}
  ?:error("Ejercicio fuera de la misión")
 require(MonarchMissionCatalog.exerciseCompatible(this,exerciseId)){"Revisa las restricciones actuales de tu perfil"}
 val counts=run.o("counts")
 counts.put(exerciseId,(counts.i(exerciseId)+increment).coerceIn(0,goal.target))
 run.put("counts",counts)
 save()
}
fun NativeStore.completeMonarchMission(now:Long=System.currentTimeMillis(),zone:ZoneId=ZoneId.systemDefault()) {
 val run=checkedMonarchRun(now,zone)
 val spec=MonarchMissionCatalog.fromRun(run)
 require(spec.goals.all{run.o("counts").i(it.exerciseId)>=it.target}){"Faltan objetivos"}
 require(spec.goals.all{MonarchMissionCatalog.exerciseCompatible(this,it.exerciseId)}){"Revisa las restricciones actuales"}
 run.put("status","completed").put("finishedAt",now)
 run.put("seal","Sello Monarca: "+spec.title).put("selfReported",true)
 save()
}
fun NativeStore.stopMonarchMission(now:Long=System.currentTimeMillis(),zone:ZoneId=ZoneId.systemDefault()) {
 val run=checkedMonarchRun(now,zone)
 run.put("status","stopped").put("finishedAt",now)
 save()
}
