package com.huntersystem.app

import org.json.JSONObject
import kotlin.math.*

/**
 * Adaptación conservadora del volumen por cinco familias independientes.
 * Nunca cambia peso externo, nivel declarado, restricciones ni sesiones registradas.
 * Un esfuerzo muy bajo permite un paso pequeño; dolor y fatiga impiden progresión.
 */
object GroupEvolution {
 val families=listOf("upper","lower","core","mobility","cardio")
 fun label(group:String)=mapOf(
  "upper" to "Tren superior","lower" to "Tren inferior",
  "core" to "Tren central","mobility" to "Movilidad","cardio" to "Cardio"
 )[group]?:group

 fun exerciseGroup(e:Exercise?):String? = e?.type?.takeIf{it in families}

 fun dominant(items:List<JSONObject>,catalog:Catalog):String? {
  val groups=items.mapNotNull{exerciseGroup(catalog.byId[it.s("id")])}.groupingBy{it}.eachCount()
  val max=groups.values.maxOrNull()?:return null
  return groups.entries.singleOrNull{it.value==max}?.key
 }

 // Prioritize the exercises actually completed, including custom routines.
 // The planned family is used only for a day without recognized exercises.
 fun sessionGroup(day:JSONObject,p:JSONObject?,catalog:Catalog):String?{
  val items=day.a("items").objects()
  dominant(items,catalog)?.let{return it}
  // Mixed sessions without a majority must not train an arbitrary group.
  if(items.any{exerciseGroup(catalog.byId[it.s("id")])!=null})return null
  return when(day.s("kind")){
   "cardio"->"cardio"
   "recovery","mobility"->"mobility"
   "strength"->p?.a("weekFocus")?.optString(weekday(day.s("date")),"upper")?.takeIf{it in families}
   else->null
  }
 }

 fun logGroup(log:JSONObject,p:JSONObject?,catalog:Catalog):String? {
  log.s("trainingGroup").takeIf{it in families}?.let{return it}
  return sessionGroup(log,p,catalog)
 }

 data class Adjustment(
  val group:String, val steps:Int=0, val lastDate:String="",
  val lastRpe:Int=0, val samples7:Int=0, val samples14:Int=0,
  val samples21:Int=0
 )

 fun evaluate(logs:JSONObject,date:String,group:String,p:JSONObject?,catalog:Catalog):Adjustment {
  if(group !in families)return Adjustment(group)
  val past=logs.entries().mapNotNull{(d,raw)->
   val log=raw as? JSONObject?:return@mapNotNull null
   if(d>=date||!Regex("\\d{4}-\\d{2}-\\d{2}").matches(d)||!log.b("complete")||log.b("pain")||log.i("rpe") !in 1..10)return@mapNotNull null
   if(logGroup(log,p,catalog)!=group)return@mapNotNull null
   d to log
  }.sortedBy{it.first}
  if(past.isEmpty())return Adjustment(group)
  val last=past.last()
  // Ventanas móviles de 7, 14 y 21 días, independientes por grupo.
  // Un registro anterior a los 21 días no aumenta la exigencia.
  val seven=past.filter{it.first>=plus(date,-7)}
  val fourteen=past.filter{it.first>=plus(date,-14)}
  val twentyOne=past.filter{it.first>=plus(date,-21)}
  if(twentyOne.isEmpty())return Adjustment(group,0,last.first,last.second.i("rpe"),0,0,0)
  val latestRpe=last.second.i("rpe")
  val high=last.second.b("fatigue")||latestRpe>=8
  val low=latestRpe in 1..3&&!last.second.b("fatigue")
  var steps=if(high)-1 else if(low)1 else 0

  // Revisiones basadas en sesiones completas: el tiempo por sí solo no progresa.
  // Cada ventana exige más evidencia para agregar un paso.
  fun review(records:List<Pair<String,JSONObject>>,minimum:Int):Int{
   if(records.size<minimum)return 0
   val avg=records.map{it.second.i("rpe").toDouble()}.average()
   val difficult=records.count{it.second.b("fatigue")||it.second.i("rpe")>=8}
   return when{
    difficult>=2||avg>=7.5 -> -1
    difficult==0&&avg<=4.0 -> 1
    else -> 0
   }
  }
  steps+=review(seven,2)
  steps+=review(fourteen,3)
  steps+=review(twentyOne,5)
  // A stopped or painful session must block progression even if a previous
  // completed session was easy. It remains visible to the existing risk filters.
  val recentProblem=logs.entries().any{(d,v)->
   val l=v as? JSONObject
   l!=null&&d<date&&d>=plus(date,-14)&&logGroup(l,p,catalog)==group&&
    (l.b("pain")||l.b("fatigue")||l.i("rpe")>=8)
  }
  if(high||recentProblem)steps=steps.coerceAtMost(0)
  // Global safety and medical gates are still enforced by the existing Rules.
  return Adjustment(group,steps.coerceIn(-2,2),last.first,latestRpe,seven.size,fourteen.size,twentyOne.size)
 }

 fun feedback(logs:JSONObject,date:String,group:String,p:JSONObject?,catalog:Catalog):Feedback {
  val rows=logs.entries().mapNotNull{(d,raw)->
   val log=raw as? JSONObject?:return@mapNotNull null
   if(d>=date||d<plus(date,-14)||logGroup(log,p,catalog)!=group)return@mapNotNull null
   d to log
  }
  val week=rows.filter{it.first>=plus(date,-7)}
  val done=week.map{it.second}.filter{it.b("complete")&&!it.b("pain")&&!it.b("manual")}
  val scores=done.map{it.n("rpe")}.filter{it in 1.0..10.0}
  val avg=if(scores.isEmpty())0.0 else scores.average()
  val strain=rows.any{it.second.b("pain")||it.second.b("fatigue")||it.second.n("rpe")>=8}
  val adherence=if(week.isEmpty())0.0 else done.size.toDouble()/week.size
  return Feedback(strain,avg,adherence,done.size,done.size>=2&&adherence>=.8&&avg>0&&avg<=7&&!strain)
 }

 fun apply(e:Exercise,item:JSONObject,adjustment:Adjustment):JSONObject {
  if(adjustment.steps==0||item.i("target")<1||exerciseGroup(e)!=adjustment.group)return item
  val out=item.copyJson()
  val unit=out.s("unit",e.unit)
  val base=out.i("target")
  val increment=if(unit=="sec")5 else 1
  val maximum=if(unit=="sec")min(90,base+10) else min(24,base+2)
  val minimum=if(unit=="sec")max(5,base-10) else max(1,base-2)
  var updated=(base+adjustment.steps*increment).coerceIn(minimum,maximum)
  // Specialized catalog limits must not be bypassed by adaptation.
  val limit=e.raw.optJSONObject("doseCap46")
  if(adjustment.steps>0&&limit!=null)updated=min(updated,base)
  // Never raise high-skill, impact or power doses automatically.
  if(adjustment.steps>0&&(e.raw.b("highSkill46")||e.impact||e.power))updated=base
  if(updated==base)return item
  out.put("target",updated)
  out.put("groupAdaptation",obj(
   "group" to adjustment.group,"previousDate" to adjustment.lastDate,
   "rpe" to adjustment.lastRpe,"difference" to (updated-base),
   "sessions7" to adjustment.samples7,"sessions14" to adjustment.samples14,
   "sessions21" to adjustment.samples21,
   "why" to (if(updated<base)"Exigencia reducida según la evaluación anterior de este grupo." else "Progresión pequeña según las evaluaciones de este grupo.")
  ))
  return out
 }
}
