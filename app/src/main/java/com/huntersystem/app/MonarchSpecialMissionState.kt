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
  // La rotación Monarca es universal: NUNCA depende del nivel/objetivo del perfil.
  @Suppress("UNUSED_VARIABLE") val legacyLevel=level
  val id=listOf("guardia","fortaleza","trono","fortaleza")[cycle]
  // En ciclos sucesivos rotan los ejercicios complementarios, no las metas mínimas.
  val variant=Math.floorMod(weeks.toInt()+Math.floorDiv(weeks.toInt(),4),4)
  // Desafíos muy exigentes, pero acumulados en 7 días, nunca una sesión forzada.
  // La asignación efectiva adapta esta base al EQUIPO realmente declarado.
  val goals=when(id){
   "guardia"->listOf(
    MonarchMissionGoal("squat",100),
    listOf(MonarchMissionGoal("jumpjack",180),MonarchMissionGoal("mountain",160),
      MonarchMissionGoal("jumpjack",200),MonarchMissionGoal("mountain",175))[variant],
    listOf(MonarchMissionGoal("plank",180),MonarchMissionGoal("kneelingplank",200),
      MonarchMissionGoal("plank",210),MonarchMissionGoal("wallplank",240))[variant],
    MonarchMissionGoal("pushup",45))
   "fortaleza"->listOf(
    MonarchMissionGoal("squat",140),MonarchMissionGoal("new46-burpee",50),
    listOf(MonarchMissionGoal("reverselunge",90),MonarchMissionGoal("stepup",90),
      MonarchMissionGoal("lateralunge",90),MonarchMissionGoal("reverselunge",110))[variant],
    MonarchMissionGoal("plank",240),MonarchMissionGoal("pushup",65))
   else->listOf(
    MonarchMissionGoal("squat",180),MonarchMissionGoal("new46-burpee",75),
    MonarchMissionGoal("pushup",90),
    listOf(MonarchMissionGoal("plank",300),MonarchMissionGoal("mountain",240),
      MonarchMissionGoal("jumpjack",300),MonarchMissionGoal("kneelingplank",320))[variant],
    MonarchMissionGoal("reverselunge",120))
  }
  return MonarchMissionSpec(id,names.getValue(id),stories.getValue(id),
   start.toString(),cycle+1,deadline(start,zone),0,goals)
 }
 fun fromRun(run:JSONObject):MonarchMissionSpec {
  val id=run.s("id")
  val goals=run.a("goals").objects().map{MonarchMissionGoal(it.s("exerciseId"),it.i("target"))}
  require(id in names&&goals.isNotEmpty()&&goals.all{it.target>0}){"Datos de misión Monarca incompletos"}
  return MonarchMissionSpec(id,names.getValue(id),stories.getValue(id),
   run.s("weekStart"),run.i("cycleWeek",1),run.optLong("deadlineAt"),
   0,goals)
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
 /**
  * Única condición del perfil que influye en las misiones Monarca: equipo real.
  * Sin auto-planificación y sin incluir los 800 ejercicios todavía no auditados.
  * Los requisitos secundarios de cada ejercicio deben estar seleccionados.
  */
 fun equipmentReady(store:NativeStore,exerciseId:String):Boolean {
  val e=store.catalog.byId[exerciseId]?:return false
  if(e.raw.b("sourceExpansion2k")||e.raw.b("sourceOnly"))return false
  val p=store.profile?:JSONObject()
  val gear=p.s("equipment","body")
  val gym=p.a("gymEquipment23").strings().toSet()
  val mats=p.a("materials46").strings().toSet()
  if((e.gym||e.equipment=="gym")&&gear!="gym")return false
  if(e.equipment=="db"&&gear !in listOf("db","gym"))return false
  if(e.equipment !in setOf("body","db","gym",""))return false
  val needed=(e.raw.a("materialRequirements").strings()+e.raw.a("materials46").strings()).distinct()
  return needed.all{k->
   when(k){
    "db"->gear in listOf("db","gym")
    "gym"->gear=="gym"
    "bench"->p.b("bench")||"bench" in gym
    "step"->p.b("step")||"step" in mats
    "barbell","cable","machine","smith","cardio"->gear=="gym"&&k in gym
    "body"->true
    else->k in mats || (gear=="gym"&&k in gym)
   }
  }
 }
 private fun goalFor(e:Exercise,id:String):MonarchMissionGoal {
  // Repeticiones semanales totales, sin recomendación automática de carga.
  val points=when(id){"guardia"->0;"fortaleza"->1;else->2}
  val amount=when {
   e.unit=="sec"->listOf(180,240,300)[points]
   e.equipment!="body"->listOf(36,48,60)[points]
   e.group in setOf("Piernas","Cadera")->listOf(100,140,180)[points]
   e.group in setOf("Core","Abdomen")->listOf(65,90,120)[points]
   else->listOf(45,65,90)[points]
  }
  return MonarchMissionGoal(e.id,amount)
 }
 /** Oferta congelable: ciclo Monarca independiente del perfil, equipo adaptable. */
 fun offerFor(store:NativeStore,date:LocalDate,zone:ZoneId=ZoneId.systemDefault()):MonarchMissionSpec {
  val base=missionFor(date,2,zone)
  val gear=store.profile?.s("equipment","body")?:"body"
  val eligible=store.catalog.originalExercises.filter{equipmentReady(store,it.id)}
  val picks=base.goals.mapNotNull{g->
   store.catalog.byId[g.exerciseId]?.takeIf{equipmentReady(store,it.id)}
  }.distinctBy{it.id}.toMutableList()
  // Si el perfil declara mancuernas o equipamiento de gimnasio, aparecen
  // desafíos que realmente usan esos materiales; sin equipamiento, nunca.
  val equipmentPicks=if(gear=="body")emptyList() else eligible.filter{
   if(gear=="db")it.equipment=="db" else it.equipment=="gym"||it.equipment=="db"
  }.filter{!it.raw.b("highSkill46")&&!it.power&&!it.impact}
   .sortedWith(compareByDescending<Exercise>{it.equipment=="gym"&&gear=="gym"}
      .thenBy{it.name}.thenBy{it.id})
  val desired=if(base.id=="guardia")4 else 5
  val gearCount=if(gear=="body")0 else if(gear=="gym")2 else 1
  val chosenGear=mutableListOf<Exercise>()
  for(e in equipmentPicks){
   if(chosenGear.size>=gearCount)break
   if(chosenGear.none{it.group==e.group})chosenGear+=e
  }
  // Se garantiza variedad de movimiento, sin requerir aparatos no marcados.
  val selected=mutableListOf<Exercise>()
  selected+=picks.take(desired-chosenGear.size)
  val bodyPool=eligible.filter{it.equipment=="body"&&!it.gym}
   .filter{!it.raw.b("highSkill46")&&!it.power&&!it.impact}
   .sortedWith(compareBy<Exercise>{it.name}.thenBy{it.id})
  for(e in bodyPool){
   if(selected.size>=desired-chosenGear.size)break
   if(selected.none{it.id==e.id||it.group==e.group})selected+=e
  }
  for(e in bodyPool){
   if(selected.size>=desired-chosenGear.size)break
   if(selected.none{it.id==e.id})selected+=e
  }
  selected+=chosenGear
  return base.copy(goals=selected.distinctBy{it.id}.map{goalFor(it,base.id)})
 }
 fun exerciseCompatible(store:NativeStore,exerciseId:String):Boolean =
  equipmentReady(store,exerciseId)
 fun eligibleReason(store:NativeStore,mission:MonarchMissionSpec,now:Long):String? {
  val run=store.state.optJSONObject("monarchSpecialMission")
  if(run!=null&&run.s("weekStart")==mission.weekStart)
   return if(status(run,now)=="active")"Ya has iniciado el desafío de esta semana."
    else "Esta semana ya se ha utilizado. Nuevo desafío el próximo domingo."
  if(mission.goals.isEmpty())return "No se encontraron ejercicios con el equipo declarado."
  if(mission.goals.any{!equipmentReady(store,it.exerciseId)})
   return "La misión necesita equipo que no está marcado como disponible. Revisa solo Equipo en Perfil."
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
 val spec=MonarchMissionCatalog.offerFor(this,MonarchMissionCatalog.weekStart(now,zone),zone)
 val reason=MonarchMissionCatalog.eligibleReason(this,spec,now)
 require(reason==null){reason?:"Misión no disponible"}
 syncMonarchWeek(now,zone)
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
 require(MonarchMissionCatalog.exerciseCompatible(this,exerciseId)){"Este ejercicio requiere equipo que ya no está disponible en Perfil"}
 val counts=run.o("counts")
 counts.put(exerciseId,(counts.i(exerciseId)+increment).coerceIn(0,goal.target))
 run.put("counts",counts)
 save()
}
fun NativeStore.completeMonarchMission(now:Long=System.currentTimeMillis(),zone:ZoneId=ZoneId.systemDefault()) {
 val run=checkedMonarchRun(now,zone)
 val spec=MonarchMissionCatalog.fromRun(run)
 require(spec.goals.all{run.o("counts").i(it.exerciseId)>=it.target}){"Faltan objetivos"}
 require(spec.goals.all{MonarchMissionCatalog.equipmentReady(this,it.exerciseId)}){"Revisa el equipo de la misión antes de reclamar el sello"}
 run.put("status","completed").put("finishedAt",now)
 run.put("seal","Sello Monarca: "+spec.title).put("selfReported",true)
 save()
}
fun NativeStore.stopMonarchMission(now:Long=System.currentTimeMillis(),zone:ZoneId=ZoneId.systemDefault()) {
 val run=checkedMonarchRun(now,zone)
 run.put("status","stopped").put("finishedAt",now)
 save()
}
