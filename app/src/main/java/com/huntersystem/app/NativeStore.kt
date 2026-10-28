package com.huntersystem.app

import android.content.Context
import android.util.AtomicFile
import androidx.compose.runtime.mutableIntStateOf
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.time.LocalDate
import kotlin.math.*

class NativeStore(val context:Context,val catalog:Catalog=Catalog(context)){
 val rules=Rules(catalog);val planner=Planner(catalog,rules);val goals=Goals(catalog)
 private val file=AtomicFile(File(context.filesDir,"hunter-system-native.json"))
 var state:JSONObject=catalog.defaultState.copyJson();private set
 val revision=mutableIntStateOf(0)
 var readError:String?=null;private set
 private val previews=mutableMapOf<String,JSONObject>()
 init{if(file.baseFile.exists())try{state=validateBackup(JSONObject(file.openRead().bufferedReader().use{it.readText()}))}catch(e:Exception){readError="No se pudo leer el guardado. Puedes restaurar una copia sin sobrescribir el archivo existente."}}
 val profile:JSONObject? get()=state.optJSONObject("profile")
 val logs get()=state.o("logs")
 fun adjusted()=goals.adjusted(profile?:JSONObject(),state.a("goals").objects().filter{it.s("repeat")=="daily"||it.a("dates").length()==0}.map{it.s("title",it.s("text"))})
 @Synchronized fun save(){check(readError==null){readError?:"Error de lectura"};val stream=file.startWrite();try{stream.write(state.toString().toByteArray());file.finishWrite(stream)}catch(e:Exception){file.failWrite(stream);throw e};previews.clear();revision.intValue++}
 fun export()=state.toString(2)
 fun validateBackup(data:JSONObject):JSONObject{
  require(data.has("version")||data.has("profile")){"No es una copia de Hunter System"};require(data.optJSONObject("logs")!=null&&data.optJSONArray("cycles")!=null){"La copia no contiene registros y ciclos válidos"};require(data.toString().length<=40*1024*1024){"Copia demasiado grande"}
  data.optJSONObject("profile")?.let{p->require(p.n("age") in 13.0..100.0&&p.n("height") in 100.0..250.0&&p.n("weight") in 30.0..350.0){"Medidas del perfil fuera de rango"}}
  for((date,v)in data.o("logs").entries()){LocalDate.parse(date);val l=v as? JSONObject?:error("Registro inválido");for(x in l.a("items").objects()){require(catalog.byId.containsKey(x.s("id"))){"Ejercicio desconocido en la copia"};require(x.i("sets") in 1..30&&x.n("target")>0&&x.n("target")<=3600){"Dosis inválida"};for(a in x.a("actual").objects())require(a.n("value") in 0.0..3600.0&&a.n("kg") in 0.0..1000.0){"Serie inválida"}}}
  val out=catalog.defaultState.copyJson();for((k,v)in data.entries())out.put(k,v);return out
 }
 fun restore(data:JSONObject){val checked=validateBackup(data);state=checked;readError=null;save();refreshWeek()}
 fun saveProfile(p:JSONObject,appearance:JSONObject?=null,avatar:String?=null){
  val q=p.copyJson();for((k,range)in mapOf("age" to 13.0..100.0,"height" to 100.0..250.0,"weight" to 30.0..350.0)){val n=q.n(k,Double.NaN);require(n in range){"Revisa "+mapOf("age" to "la edad","height" to "la estatura","weight" to "el peso")[k]};q.put(k,n)};require(q.s("name").isNotBlank()){ "Escribe tu nombre de cazador" }
  q.put("name",q.s("name").take(60));q.put("finalGoal",q.s("finalGoal").take(300));q.put("minutes",q.i("minutes",20));q.put("dailyMissions",q.i("dailyMissions",3).coerceIn(1,7));q.put("frequency",q.i("frequency",2));q.put("experience",if(level(q)==0)"beginner" else "regular");q.put("load",q.n("load").coerceIn(0.0,200.0));q.put("trainingScale",q.n("trainingScale",1.0).coerceIn(.4,1.0))
  if(q.a("weekPlan").length()!=7)q.put("weekPlan",arr(listOf("rest","train","rest","train","rest","rest","rest")));if(q.a("weekFocus").length()!=7)q.put("weekFocus",arr(List(7){"upper"}))
  val interpreted=goals.apply(q,q.b("manualGoalGroupNative"));if(interpreted.o("interpretation374").b("medical"))require(rules.injuries(interpreted).isNotEmpty()||interpreted.o("health").b("special")){"Tu meta menciona dolor o lesión. Completa Lesiones y salud antes de guardar el plan."}
  for(d in allDays().filter{it.s("date")>=today()&&!activeLog(logs.optJSONObject(it.s("date")))})d.remove("reviewReducedNative")
  state.put("profile",interpreted);appearance?.let{state.put("appearance33",it.copyJson())};avatar?.let{state.put("avatar",it)}
  // Confirmed work remains immutable; pending incompatible records are archived in place.
  val ap=adjusted();for((date,v)in logs.entries()){val l=v as? JSONObject?:continue;if(date<today()||l.b("complete"))continue;for(x in l.a("items").objects()){val e=catalog.byId[x.s("id")]?:continue;if(!rules.compatible(e,ap)){x.put("archived",true);x.put("archiveReason","Perfil actualizado: revisar compatibilidad antes de continuar.")}}}
  save();refreshWeek()
 }
 fun activeLog(l:JSONObject?)=l!=null&&(l.b("complete")||l.b("manual")||l.b("pain")||l.b("fatigue")||l.b("selfReported")||l.a("missionTasks").objects().any{it.b("done")}||l.a("items").objects().any{it.b("missionState")||it.b("selfReported")||it.a("actual").objects().any{a->a.b("done")}})
 fun locked(day:JSONObject)=day.b("customized")||day.b("reviewReducedNative")||day.has("routineId")||day.b("creatorAssigned")||day.b("manual")||activeLog(logs.optJSONObject(day.s("date")))||day.a("tasks").objects().any{it.b("done")}
 fun allDays()=state.a("cycles").objects().flatMap{it.a("days").objects()}
 fun makeDay(date:String,planned:MutableList<PlanRow> = mutableListOf(),used:Set<String> = emptySet()):JSONObject{
  val p=profile?:return obj("date" to date,"kind" to "rest","items" to JSONArray(),"tasks" to JSONArray(),"title" to "Completa tu perfil")
  val choice=p.a("weekPlan").optString(weekday(date),"rest");val kind=mapOf("train" to "strength","cardio" to "cardio","mobility" to "recovery")[choice]?:"rest";val q=adjusted();val result=planner.choose(q,kind,planner.profileKey(q)+"|"+date+"|1",logs,date,used,planned)
  val focus=p.a("weekFocus").optString(weekday(date),"upper");val title=if(kind=="rest")"Descanso elegido" else if(kind=="strength")groupLabels[focus]?:"Tren superior" else kindLabel(kind)
  return obj("date" to date,"kind" to kind,"scheduleChoice" to choice,"items" to result.a("items"),"tasks" to if(result.has("blocked"))arr(listOf(obj("id" to "review-dynamic381","text" to "Revisar restricciones para encontrar ejercicios compatibles","done" to false))) else JSONArray(),"title" to if(result.has("blocked"))kindLabel(kind)+" · requiere revisión" else title+if(kind=="rest")"" else " · plan dinámico","dynamic381" to true,"blocked381" to result.opt("blocked"),"planningNote" to "Generado bajo demanda desde demostraciones 2D existentes, sin rutinas combinatorias precargadas.")
 }
 fun refreshWeek(){if(profile==null||readError!=null)return;val monday=LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value-1).toLong()).toString();val cycles=state.a("cycles");var c=cycles.objects().firstOrNull{it.s("start")==monday};if(c==null){c=obj("index" to cycles.length()+1,"start" to monday,"days" to JSONArray(),"reviews" to JSONArray());cycles.put(c);state.put("cycles",cycles)};val q=adjusted();val key=planner.profileKey(q)+"|"+today()+"|"+hash(logs.toString()).toString(36);if(c.s("nativeKey")==key)return
  if(c.a("reviews").objects().lastOrNull()?.s("action")=="pausar")return
  val days=c.a("days").objects().associateBy{it.s("date")};val planned=allDays().filter{it.s("date")>=today()&&locked(it)}.flatMap{d->d.a("items").objects().mapNotNull{x->catalog.byId[x.s("id")]?.let{PlanRow(d.s("date"),it.oldFamily,it.group,it.pattern,x.i("sets").coerceAtMost(6))}}}.toMutableList();val used=mutableSetOf<String>();val newDays=(0L..6L).map{offset->val date=plus(monday,offset);val old=days[date];val d=if(old!=null&&(date<today()||locked(old)))old else makeDay(date,planned,used);used+=d.a("items").objects().mapNotNull{catalog.byId[it.s("id")]?.family};d};c.put("days",arr(newDays));c.put("nativeKey",key);c.put("profile",profile!!.copyJson());c.put("dynamicVersion381","38.49");c.put("decision","Generación dinámica de sesiones: calendario, objetivo, historial y restricciones. Se conservan sesiones registradas.");save()
 }
 fun day(date:String)=allDays().firstOrNull{it.s("date")==date}?:previews.getOrPut(date){makeDay(date)}
 fun mutableDay(date:String):JSONObject{LocalDate.parse(date);allDays().firstOrNull{it.s("date")==date}?.let{return it};val start=LocalDate.parse(date).minusDays((LocalDate.parse(date).dayOfWeek.value-1).toLong()).toString();val cycles=state.a("cycles");var cycle=cycles.objects().firstOrNull{it.s("start")==start};if(cycle==null){cycle=obj("start" to start,"index" to cycles.length()+1,"days" to arr((0L..6L).map{day(plus(start,it)).copyJson()}));cycles.put(cycle);state.put("cycles",cycles)};return cycle.a("days").objects().first{it.s("date")==date}}
 fun log(date:String=today()):JSONObject{require(date==today()){ "El registro de series está disponible en la fecha de hoy" };logs.optJSONObject(date)?.let{return it};val d=day(date);val l=obj("date" to date,"kind" to d.s("kind"),"items" to arr(d.a("items").objects().map{x->x.copyJson().put("actual",arr((0 until x.i("sets")).map{obj("done" to false,"value" to x.i("target"),"kg" to 0)}))}),"missionTasks" to d.a("tasks").copyJson(),"complete" to false,"pain" to false,"rpe" to 0,"notes" to "");val logs=state.o("logs");logs.put(date,l);state.put("logs",logs);save();return l}
 fun toggleMission(date:String,index:Int){val l=log(date);require(!l.b("complete")&&!l.b("pain")){"La sesión está cerrada"};val x=l.a("items").getJSONObject(index);require(!x.b("archived")){x.s("archiveReason")};val e=catalog.byId[x.s("id")]?:error("Ejercicio desconocido");require(rules.compatible(e,adjusted())){"Revisa la compatibilidad del ejercicio en Perfil"};x.put("missionState",!x.b("missionState"));save()}
 fun toggleTask(date:String,index:Int){val l=log(date);require(!l.b("complete")&&!l.b("pain"));val x=l.a("missionTasks").getJSONObject(index);x.put("done",!x.b("done"));save()}
 fun confirmSet(date:String,itemIndex:Int,setIndex:Int,value:Double,kg:Double,done:Boolean=true){val l=log(date);require(!l.b("complete")&&!l.b("pain")){"La sesión está cerrada"};val x=l.a("items").getJSONObject(itemIndex);require(!x.b("archived")){x.s("archiveReason")};val e=catalog.byId[x.s("id")]?:error("Ejercicio desconocido");require(rules.compatible(e,adjusted())){"Revisa la compatibilidad del ejercicio en Perfil"};require(value.isFinite()&&value>0&&value<=if(x.s("unit",e.unit)=="sec")3600 else 300);require(kg.isFinite()&&kg in 0.0..200.0);val actual=x.a("actual");require(setIndex in 0 until x.i("sets"));actual.put(setIndex,obj("done" to done,"value" to value,"kg" to if(e.equipment=="body")0 else kg));x.put("actual",actual);save()}
 fun feedback(date:String,rpe:Int,notes:String){require(rpe in 0..10);val l=log(date);require(!l.b("complete"));l.put("rpe",rpe);l.put("notes",notes.take(3000));save()}
 fun finish(date:String){val l=log(date);require(!l.b("complete")&&!l.b("pain"));require(l.a("items").objects().all{it.b("archived")||it.b("missionState")||it.b("selfReported")||it.a("actual").objects().count{a->a.b("done")}>=it.i("sets")}&&l.a("missionTasks").objects().all{it.b("done")}){"Completa los objetivos o registra las series pendientes"};require(l.s("kind")=="rest"||l.i("rpe") in 1..10){"Indica tu esfuerzo del 1 al 10"};l.put("complete",true);l.put("finishedAt",java.time.Instant.now().toString());save();refreshWeek()}
 fun pain(date:String,notes:String){val l=log(date);require(!l.b("complete"));l.put("pain",true);l.put("notes",notes.take(3000));save()}
 fun assign(routine:JSONObject,date:String){require(date>=today()){ "No se pueden sustituir días pasados" };require(!activeLog(logs.optJSONObject(date))){"Este día ya tiene actividad registrada"};val p=adjusted();require(rules.gate(p).ok);val items=routine.a("items").objects();require(items.isNotEmpty()&&items.all{catalog.byId[it.s("id")]?.let{e->rules.compatible(e,p)}==true}){"Revisa compatibilidad y equipo en Perfil"};val d=mutableDay(date);d.put("items",arr(items.map{it.copyJson()}));d.put("kind",when(routine.s("category")){"Cardio"->"cardio";"Movilidad"->"recovery";"Mixta"->"mixed";else->"strength"});d.put("title",routine.s("name"));d.put("routineId",routine.s("id"));d.put("customized",true);d.put("tasks",JSONArray());routine.optJSONObject("sourceSequence")?.let{d.put("sourceSequence",it.copyJson())};val logs=state.o("logs");logs.remove(date);state.put("logs",logs);save()}
 fun customize(date:String,items:List<JSONObject>,tasks:List<String>,title:String){require(date>=today()&&!activeLog(logs.optJSONObject(date))){"No se puede sustituir una sesión con actividad"};require(items.all{catalog.byId[it.s("id")]?.let{e->rules.compatible(e,adjusted())}==true}){"Revisa las restricciones del perfil"};val d=mutableDay(date);d.put("items",arr(items));d.put("tasks",arr(tasks.filter{it.isNotBlank()}.mapIndexed{i,t->obj("id" to "custom-$i","text" to t.take(300),"done" to false)}));d.put("customized",true);d.put("title",title.take(160));d.remove("blocked381");save()}
 fun fraction(l:JSONObject?):Double{if(l==null||l.b("pain"))return 0.0;if(l.b("complete"))return 1.0;val tasks=l.a("missionTasks").objects();val items=l.a("items").objects();val total=tasks.size+items.size;if(total==0)return 0.0;return (tasks.count{it.b("done")}.toDouble()+items.sumOf{x->if(x.b("selfReported")||x.b("missionState"))1.0 else min(1.0,x.a("actual").objects().count{it.b("done")}.toDouble()/max(1,x.i("sets")))} )/total}
 fun stats():JSONObject{val entries=logs.entries().filter{it.first<=today()}.mapNotNull{(d,l)->(l as? JSONObject)?.let{d to it}};val completed=entries.filter{it.second.b("complete")};var series=0;var reps=0.0;var seconds=0.0;var volume=0.0;var exercises=0
  for((_,l)in entries)for(x in l.a("items").objects()){val e=catalog.byId[x.s("id")]?:continue;var count=0;for(a in x.a("actual").objects())if(a.b("done")){series++;count++;val n=a.n("value")*(if(e.sides&&(x.s("side").isBlank()||x.s("side")=="both"))2 else 1);if(x.s("unit",e.unit)=="sec")seconds+=n else{reps+=n;volume+=n*a.n("kg")}};if(count>0)exercises++}
  val dates=completed.map{it.first}.toSet();var cursor=if(today() in dates)today() else plus(today(),-1);var streak=0;while(cursor in dates){streak++;cursor=plus(cursor,-1)};val xp=entries.sumOf{(_,l)->floor(fraction(l)*when(l.s("kind")){"strength","mixed"->100;"cardio"->70;"recovery"->40;else->20}).toInt()};return obj("days" to completed.size,"series" to series,"reps" to reps,"seconds" to seconds,"volume" to volume,"exerciseCount" to exercises,"streak" to streak,"xp" to xp,"level" to 1+xp/300)
 }
 fun addGoal(title:String,repeat:String){require(title.isNotBlank());val goals=state.a("goals");goals.put(obj("id" to "goal-"+System.currentTimeMillis(),"title" to title.take(200),"text" to title.take(200),"repeat" to repeat,"dates" to JSONArray()));state.put("goals",goals);save()}
 fun toggleGoal(id:String,date:String=today()){val g=state.a("goals").objects().first{it.s("id")==id};val dates=g.a("dates").strings().toMutableSet();if(!dates.add(date))dates.remove(date);g.put("dates",arr(dates));save()}
 fun removeGoal(id:String){state.put("goals",arr(state.a("goals").objects().filter{it.s("id")!=id}));save()}
 fun review(action:String){
  require(action in listOf("mantener","reducir","pausar"))
  val c=state.a("cycles").objects().lastOrNull()?:return
  val rs=c.a("reviews");rs.put(obj("date" to today(),"action" to action));c.put("reviews",rs)
  for(d in c.a("days").objects().filter{it.s("date")>=today()&&!locked(it)}){
   if(action=="pausar"){d.put("items",JSONArray());d.put("kind","rest");d.put("title","Revisión de indicaciones");d.put("tasks",arr(listOf(obj("id" to "coach-review","text" to "Revisar el dolor y las indicaciones antes de volver a entrenar","done" to false))))}
   else if(action=="reducir"){for(x in d.a("items").objects()){val e=catalog.byId[x.s("id")]?:continue;x.put("target",max(if(e.unit=="sec")1 else 1,floor(x.n("target")*.8).toInt()))};d.put("reviewReducedNative",true)}
  }
  if(action=="mantener")c.remove("nativeKey")
  save();if(action=="mantener")refreshWeek()
 }

}
object HunterRepository{@Volatile private var instance:NativeStore?=null;fun get(context:Context):NativeStore=instance?:synchronized(this){instance?:NativeStore(context.applicationContext).also{instance=it}}}
