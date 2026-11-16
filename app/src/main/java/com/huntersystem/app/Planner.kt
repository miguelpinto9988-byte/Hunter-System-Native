package com.huntersystem.app

import org.json.JSONObject
import org.json.JSONArray
import kotlin.math.*

data class Feedback(val strain:Boolean,val avg:Double,val adherence:Double,val completed:Int,val canProgress:Boolean)
class Planner(val catalog:Catalog,val rules:Rules){
 // Un solo adaptador del catálogo por configuración; evita reanalizar los
 // 2000 registros al dibujar siete días del mismo calendario.
 private var cachedPlanningKey:String?=null
 private var cachedPlanningSource:MonarchActivitySource?=null
 private fun planningSource(p:JSONObject):MonarchActivitySource {
  val key=profileKey(p)
  val previous=cachedPlanningSource
  if(key==cachedPlanningKey&&previous!=null)return previous
  val source=MonarchActivitySource(catalog,rules,p.copyJson())
  cachedPlanningSource=source
  cachedPlanningKey=key
  return source
 }
 val fields=listOf("age","goal","goalGroup","finalGoal","finalTarget","focus","difficulty","experience","equipment","gymEquipment23","bench","step","minutes","frequency","dailyMissions","trainingMode","trainingScale","weekPlan","weekFocus","injuries","restrictions","health","limits","preferredTrainer","load","restSeconds","materials46","capabilities46","dynamicRequestedFocus3847")
 fun profileKey(p:JSONObject)=hash(arr(fields.map{arr(listOf(it,p.opt(it)))}).toString()).toString(36)
 fun recent(logs:JSONObject,date:String,days:Long)=logs.entries().mapNotNull{(d,l)->if(d<date&&d>=plus(date,-days)&&l is JSONObject)d to l else null}
 fun feedback(logs:JSONObject,date:String):Feedback{val rows=recent(logs,date,7);val done=rows.map{it.second}.filter{it.b("complete")&&!it.b("pain")&&!it.b("manual")};val efforts=done.map{it.n("rpe")}.filter{it in 1.0..10.0};val avg=if(efforts.isEmpty())0.0 else efforts.average();val strain=recent(logs,date,14).any{(_,l)->l.b("pain")||l.b("fatigue")||l.n("rpe")>=8};val adherence=if(rows.isEmpty())0.0 else done.size.toDouble()/rows.size;return Feedback(strain,avg,adherence,done.size,done.size>=2&&adherence>=.8&&avg>0&&avg<=7&&!strain)}
 fun rest(p:JSONObject)=p.i("restSeconds").takeIf{it in listOf(30,60,90)}?:listOf(30,60,90)[level(p)]
 fun cap(e:Exercise,p:JSONObject,item:JSONObject,logs:JSONObject=JSONObject(),date:String=today(),status:Gate=rules.gate(p),fb:Feedback=feedback(logs,date)):JSONObject{
  val c=e.raw.optJSONObject("doseCap46")?:return item;val out=item.copyJson();val scale=p.n("trainingScale",1.0).coerceIn(.25,1.0);val risky=rules.active(p)||status.reduced||fb.strain||recent(logs,date,14).any{(_,l)->l.b("pain")}
  // Los límites originales admiten reducción, nunca incremento implícito
  // por completar días o registrar RPE bajo. Solo un perfil numérico validado
  // puede proponer una referencia opcional fuera de la sesión automática.
  var ceiling=max(1,floor(c.n("target")*scale).toInt())
  val start=max(1,floor(c.n("initial")*scale).toInt())
  ceiling=min(ceiling,start)
  out.put("target",out.i("target",start).coerceIn(1,ceiling));out.put("sets",out.i("sets",1).coerceIn(1,if(risky)1 else c.i("sets",1)));out.put("rest",max(c.i("rest"),out.i("rest")).coerceAtMost(90));out.put("catalog46",true);out.put("noAutoLoad",out.b("noAutoLoad")||risky||e.raw.b("highSkill46"));out.put("safetyNote",listOf(out.s("safetyNote"),e.raw.a("restrictions46").strings().joinToString(" ")).filter{it.isNotBlank()}.joinToString(" "));return out
 }
 fun load(logs:JSONObject,date:String):Triple<Map<String,Int>,Map<String,Int>,Map<String,Int>>{val groups=mutableMapOf<String,Int>();val patterns=mutableMapOf<String,Int>();val ids=mutableMapOf<String,Int>();for((_,l) in recent(logs,date,7)){if(l.b("pain"))continue;for(x in l.a("items").objects()){val n=x.a("actual").objects().count{it.b("done")};val e=catalog.byId[x.s("id")]?:continue;if(n>0){groups[e.group]=(groups[e.group]?:0)+n;patterns[e.pattern]=(patterns[e.pattern]?:0)+n;ids[e.oldFamily]=(ids[e.oldFamily]?:0)+n}}};return Triple(groups,patterns,ids)}
 fun supportScore(e:Exercise,p:JSONObject):Int{val affected=rules.injuries(p).map{it.s("zone")};if(affected.isEmpty())return 0;var s=0;if(affected.any{it in listOf("hip","knee","ankle")}){if(e.raw.b("seated"))s+=12;if(e.group in listOf("Brazos","Pecho","Espalda","Hombros"))s+=4};if(affected.any{it in listOf("neck","shoulder","elbow","wrist")}&&e.group in listOf("Piernas","Cadera"))s+=6;if("back" in affected&&e.raw.b("seated"))s+=5;if(e.raw.s("impact")=="Con saltos"||e.equipment in listOf("db","gym")||"db" in e.raw.a("requirements").strings())s-=4;return s}
 fun metadataScore(e:Exercise,p:JSONObject,kind:String):Int{val m=e.raw.o("metadata3812");if(!m.b("usable"))return 0;val pat=m.s("pattern");return when{kind=="strength"&&p.s("goal")=="strength"&&pat in listOf("empuje_horizontal","traccion_horizontal","empuje_vertical","bisagra_cadera","rodilla")->6;kind=="strength"&&p.s("goal")=="muscle"&&pat in listOf("flexion_codo","extension_codo","elevacion_hombro","empuje_horizontal","traccion_horizontal")->4;kind=="cardio"&&p.s("goal")=="endurance"&&m.o("fields").b("pattern")->2;else->0}}
 /**
  * ÚNICA selección de ejercicios para misiones y sesiones históricas.
  * Las políticas técnicas filtran variantes; la dosificación/reproductor
  * siguen siendo los de Planner y Rules. No se escribe otro plan en paralelo.
  */
 fun choose(p:JSONObject,kind:String,seed:String,logs:JSONObject=JSONObject(),
            date:String=today(),usedBases:Set<String> = emptySet(),
            planned:MutableList<PlanRow> = mutableListOf(),weekIndex:Int=0,
            trainingProfile:com.huntersystem.app.planning.TrainingProfile?=null,
            observations:List<com.huntersystem.app.planning.EffortObservation>?=null,
            phases:Map<com.huntersystem.app.planning.Category,com.huntersystem.app.planning.TechnicalPhase> = emptyMap(),
            pinned:com.huntersystem.app.planning.DailyMission?=null):JSONObject{
  val gate=rules.gate(p)
  if(!gate.ok)return obj("items" to JSONArray(),"blocked" to gate.reason,"requested" to kind)
  if(kind=="rest")return obj("items" to JSONArray(),"effective" to "rest","requested" to "rest",
   "progressCategory" to "descanso")
  val localDate=runCatching{java.time.LocalDate.parse(date)}.getOrNull()
   ?:return obj("items" to JSONArray(),"blocked" to "Fecha no válida","requested" to kind)
  val baseProfile=trainingProfile?:MonarchProfileAdapter.from(p,localDate)
   ?:return obj("items" to JSONArray(),"blocked" to "Perfil de planificación incompleto","requested" to kind)
  val current=baseProfile.category(localDate)
  val focus=p.s("dynamicRequestedFocus3847").takeIf{it in listOf("upper","core","lower")}
   ?:p.a("weekFocus").optString(weekday(date),"upper")
  val requested=when(kind){
   "cardio"->com.huntersystem.app.planning.Category.CARDIO
   "recovery","mobility"->com.huntersystem.app.planning.Category.MOBILITY
   "strength"->if(current in listOf(com.huntersystem.app.planning.Category.UPPER,
     com.huntersystem.app.planning.Category.CENTRAL,
     com.huntersystem.app.planning.Category.LOWER))current
     else com.huntersystem.app.planning.CategoryMapper.normalize(focus)
   else->current
  }?:return obj("items" to JSONArray(),"effective" to "rest","requested" to kind)
  val index=localDate.dayOfWeek.value-1
  val week=baseProfile.weekly.toMutableList()
  week[index]=requested.ordinal
  val timeUnit=when(kind){"strength"->5;"cardio"->4;else->3}
  val timeLimit=(p.i("minutes",20).coerceIn(10,90)/timeUnit).coerceIn(1,7)
  val profile=baseProfile.copy(weekly=week,
   dailyLimit=min(baseProfile.dailyLimit,timeLimit))
  val source=planningSource(p)
  val history=observations?:MonarchLegacyHistory.efforts(logs)
  val previous=recent(logs,date,21).filter{it.second.b("complete")}
    .flatMap{it.second.a("items").objects().map{x->x.s("id")}}.toSet()
  val load=load(logs,date)
  val priority=source.all().associate{item->
   val e=catalog.byId[item.id]
   item.id to if(e==null)0 else (
    -min(24,(load.first[e.group]?:0)*2)
    -min(15,(load.second[e.pattern]?:0)*2)
    -min(25,(load.third[e.oldFamily]?:0)*7)
    -(if(e.id in previous)22 else 0)
    -min(20,planned.filter{distance(it.date,date)<=6&&it.group==e.group}.sumOf{it.sets}*2)
    +supportScore(e,p)+metadataScore(e,p,kind)
    +(rules.score(e,p,kind,emptyList())/5.0).coerceIn(-25.0,25.0).roundToInt()
   ).coerceIn(-90,65)
  }
  val candidateFresh=source.all().any{item->
   item.category==requested &&
    catalog.byId[item.id]?.family !in usedBases &&
    com.huntersystem.app.planning.EligibilityPolicy.compatible(item,profile)
  }
  val excluded=if(candidateFresh)source.all().filter{item->
    catalog.byId[item.id]?.let{e->e.family in usedBases}?:true
  }.map{it.id}.toSet() else emptySet()
  val mission=com.huntersystem.app.planning.DailyMissionPlanner.plan(
   localDate,profile,source,history,phases,pinned,priority,excluded)
  val adaptation=com.huntersystem.app.planning.CategoryEffortPolicy.decide(
   history,localDate,requested)
  val stop=gate.reduced||rules.active(p)||
   adaptation==com.huntersystem.app.planning.Adaptation.RECOVER_ONE
  val limited=adaptation==com.huntersystem.app.planning.Adaptation.REDUCE_TWO
  val group=when(requested){
   com.huntersystem.app.planning.Category.UPPER->"upper"
   com.huntersystem.app.planning.Category.CENTRAL->"core"
   com.huntersystem.app.planning.Category.LOWER->"lower"
   com.huntersystem.app.planning.Category.MOBILITY->"mobility"
   com.huntersystem.app.planning.Category.CARDIO->"cardio"
  }
  val oldReview=GroupEvolution.evaluate(logs,date,group,p,catalog)
  val safeReview=oldReview.copy(steps=min(0,oldReview.steps))
  val groupFeedback=GroupEvolution.feedback(logs,date,group,p,catalog)
  val items=mission.ids.mapNotNull{id->
   val e=catalog.byId[id]?:return@mapNotNull null
   val metadata=source.byId(id)?:return@mapNotNull null
   if(!rules.compatible(e,p))return@mapNotNull null
   val first=libraryDose(e,p,kind)
   if(stop)first.put("sets",1)
   else if(limited)first.put("sets",min(2,first.i("sets",1)))
   if(stop)first.put("noAutoLoad",true)
   val capped=cap(e,p,rules.groupDose(e,p,kind,first,logs,date,gate,
    stop||limited||groupFeedback.strain),logs,date,gate,groupFeedback)
   val item=GroupEvolution.apply(e,capped,safeReview)
   item.put("unit",e.unit)
   item.put("typeId",metadata.typeId)
   item.put("technicalLevel",metadata.technicalLevel)
   item.put("progressStatus","PENDING")
   item.put("unifiedPlanning",true)
   item
  }
  for(item in items){val e=catalog.byId[item.s("id")]?:continue
   planned+=PlanRow(date,e.oldFamily,e.group,e.pattern,item.i("sets").coerceAtMost(6))}
  return obj("items" to arr(items),"effective" to kind,"requested" to kind,
   "count" to items.size,"progressCategory" to requested.code,
   "progressSettingsHash" to mission.profileHash,
   "progressCatalogHash" to mission.catalogHash,
   "planningNote" to mission.explanation,
   "blocked" to if(items.isEmpty())mission.explanation else "")
 }
 fun gentle(p:JSONObject)=rules.mode(p).reduced||catalog.editorial.o("goalPlans").o(p.s("finalTarget")).b("gentle")
 fun libraryDose(e:Exercise,p:JSONObject,kind:String):JSONObject{
  val c=e.raw.optJSONObject("doseCap46");if(c!=null)return cap(e,p,obj("id" to e.id,"sets" to 2,"target" to c.i("initial"),"rest" to c.i("rest")))
  val n=level(p);val novice=p.s("experience")=="beginner";val scale=p.n("trainingScale",1.0).coerceIn(.4,1.0)
  if(gentle(p)||kind=="recovery")return obj("id" to e.id,"sets" to 1,"target" to if(e.unit=="sec")15 else if(e.raw.b("pack23"))5 else 6,"rest" to if(e.raw.b("pack23"))rest(p) else 30)
  var sets:Int;var target:Int
  if(e.raw.b("pack23")){val range=e.raw.a("repRange19").ints().ifEmpty{listOf(8,16)};sets=if(novice)2 else listOf(4,4,5)[n];target=if(e.unit=="sec")listOf(30,40,50)[n] else min(range.last(),range.first()+listOf(4,2,0)[n]);target=max(if(e.unit=="sec")10 else 3,floor(target*scale).toInt())}
  else{when{e.raw.s("family")=="power"->{sets=if(novice)2 else listOf(2,3,4)[n];target=listOf(4,5,6)[n]};e.unit=="sec"->{sets=if(novice)2 else listOf(3,4,4)[n];target=if(kind=="cardio")listOf(35,45,50)[n] else listOf(25,35,40)[n]};else->{sets=if(novice)2 else listOf(4,4,5)[n];target=e.raw.a("repRange19").optInt(0,listOf(14,12,10)[n]);if(novice)target=min(target,listOf(10,8,6)[n])}};target=max(if(e.unit=="sec")10 else 2,floor(target*scale).toInt())}
  return obj("id" to e.id,"sets" to sets,"target" to target,"rest" to rest(p))
 }
 fun phaseSeconds(p:JSONObject)=((if(gentle(p))30 else listOf(30,45,60)[level(p)])*p.n("trainingScale",1.0).coerceIn(.4,1.0)/5).let{floor(it+.5).toInt()*5}.coerceIn(20,60)
 fun warmKey(e:Exercise)=when{Regex("box|shadow|punch").containsMatchIn(e.oldFamily)->"boxing";e.oldFamily in listOf("jog","heelback","highknees")->"running";"march" in e.oldFamily->"marching";else->e.oldFamily}
 fun phase(items:List<JSONObject>,p:JSONObject):Exercise?{val used=items.mapNotNull{catalog.byId[it.s("id")]};return listOf("fastmarch","stepjack","heelback","box","heeldig","sidestep","seatedpunch","seatedmarch","seatedstep").mapNotNull{catalog.byId[it]}.filter{e->!e.impact&&!e.power&&used.none{it.family==e.family||warmKey(it)==warmKey(e)}&&rules.compatible(e,p)}.let{xs->xs.firstOrNull{!it.raw.b("seated")}?:xs.firstOrNull()}}
 fun workSeconds(e:Exercise,item:JSONObject):Int{if(item.s("unit",e.unit)=="sec")return item.i("target");return (item.i("target")*e.raw.n("tempoSeconds18",4.0)*(if(e.sides&&item.s("side").isBlank())2 else 1)).roundToInt()}
 fun steps(day:JSONObject,p:JSONObject):List<JSONObject>{val items=day.a("items").objects();if(items.isEmpty())return emptyList();val out=mutableListOf<JSONObject>();val phase=phase(items,p);val sec=phaseSeconds(p)
  if(phase!=null)out+=obj("kind" to "preparation","id" to phase.id,"label" to "Calentamiento progresivo · ${phase.name}","seconds" to sec,"unit" to "sec","target" to sec)
  val source=day.optJSONObject("sourceSequence");if(source!=null){var itemIndex=0;for((bi,b)in source.a("steps").objects().withIndex()){if(b.s("kind")!="exercise"){out+=b.copyJson().put("blockIndex",bi);continue};val e=catalog.byId[b.s("exerciseId")]?:continue;val x=b.copyJson();val unit=b.s("unit",e.unit);for(si in 0 until b.i("sets",1)){out+=obj("kind" to "exercise","id" to e.id,"label" to e.name,"target" to b.i("target"),"unit" to unit,"side" to b.s("side"),"seconds" to if(unit=="sec")b.i("target") else workSeconds(e,x),"itemIndex" to itemIndex,"setIndex" to si,"sets" to b.i("sets"),"blockIndex" to bi);if(b.i("restSeconds")>0&&si<b.i("sets")-1)out+=obj("kind" to "rest","label" to "Descanso","seconds" to b.i("restSeconds").coerceIn(30,90),"blockIndex" to bi)};itemIndex++}}
  else{val rounds=p.s("blockMode22")=="rounds";val sequence=if(rounds)(0 until (items.maxOfOrNull{it.i("sets")}?:0)).flatMap{s->items.withIndex().filter{it.value.i("sets")>s}.map{Triple(it.index,s,it.value)}} else items.withIndex().flatMap{(i,x)->(0 until x.i("sets")).map{Triple(i,it,x)}}
   for((ix,v)in sequence.withIndex()){val (i,s,x)=v;val e=catalog.byId[x.s("id")]?:continue;out+=obj("kind" to "exercise","id" to e.id,"label" to e.name,"target" to x.i("target"),"unit" to x.s("unit",e.unit),"side" to x.s("side"),"seconds" to workSeconds(e,x),"itemIndex" to i,"setIndex" to s,"sets" to x.i("sets"),"blockIndex" to i);if(ix<sequence.lastIndex&&(!rounds||sequence[ix+1].second!=s))out+=obj("kind" to "rest","label" to "Descanso","seconds" to rest(p),"blockIndex" to i)}
  }
  if(phase!=null)out+=obj("kind" to "cooldown","id" to phase.id,"label" to "Cierre suave · ${phase.name}","seconds" to sec,"unit" to "sec","target" to sec);return out
 }
 fun minutes(r:JSONObject,p:JSONObject)=ceil(steps(r,p).sumOf{it.n("seconds")}/60).toInt()
 fun generateRoutine(mode:String,type:String,n:Int,variant:Int,range:String,epoch:Int,p:JSONObject,logs:JSONObject=JSONObject(),date:String=today()):JSONObject?{
  val kind=if(type=="cardio")"cardio" else if(type=="mobility")"recovery" else "strength";val low=if(range=="medium")10 else 5;val high=if(range=="short")10 else if(range=="medium")20 else 30;val adjusted=p.copyJson().put("difficulty",listOf("basic","intermediate","advanced")[n]).put("minutes",high)
  // La biblioteca histórica y la misión usan exactamente el mismo filtro
  // técnico y de equipamiento, además de los filtros previos de Rules.
  val nativeProfile=MonarchProfileAdapter.from(adjusted,
   runCatching{java.time.LocalDate.parse(date)}.getOrNull()?:java.time.LocalDate.now())?:return null
  val validated=planningSource(adjusted)
  val maxTechnical=when(n){0->2;1->3;else->3}
  val safeIds=validated.all().filter{
   com.huntersystem.app.planning.EligibilityPolicy.compatible(
    it,nativeProfile,maxTechnical)
  }.map{it.id}.toSet()
  val candidates=rules.pool(adjusted,kind).filter{it.id in safeIds &&
   (if(mode=="gym")it.gym||it.power else !it.gym) &&
   rules.matches(it,type) &&
   (mode!="gym"||it.raw.a("gymRequirements23").strings().all{
     key->key in adjusted.a("gymEquipment23").strings()})}
  if(candidates.isEmpty())return null
  val seed="$epoch|$mode|$type|$n|$variant";val status=rules.gate(adjusted);val fb=GroupEvolution.feedback(logs,date,type,adjusted,catalog);val rawEvolution=GroupEvolution.evaluate(logs,date,type,adjusted,catalog);val evolution=if(status.reduced||rules.active(adjusted))rawEvolution.copy(steps=min(0,rawEvolution.steps)) else rawEvolution;val ready=rules.powerReady(adjusted,logs,date,status,fb.strain);var winner:List<JSONObject>?=null;var winMinutes=0;var winScore=Double.POSITIVE_INFINITY
  for(count in 2..min(candidates.size,if(range=="short")5 else 7)){val picked=mutableListOf<Exercise>();val used=mutableSetOf<String>();val groups=mutableMapOf<String,Int>();while(picked.size<count){val possible=candidates.filter{it.family !in used};val allowed=rules.selection(possible,picked,count,adjusted,kind,type,ready);if(allowed.isEmpty())break;val e=allowed.sortedWith(compareByDescending<Exercise>{rules.score(it,adjusted,kind,picked)-(groups[it.group]?:0)*14}.thenBy{hash("$seed|${it.id}")}).first();picked+=e;used+=e.family;groups[e.group]=(groups[e.group]?:0)+1};if(picked.size<count)break;if(mode=="gym"&&picked.none{it.gym})continue
   for(sets in 1..3){val items=picked.map{e->val base=libraryDose(e,adjusted,kind);val x=obj("id" to e.id,"sets" to min(sets,base.i("sets",1)).coerceAtLeast(1),"target" to base.i("target",8).coerceIn(1,if(e.unit=="sec")50 else 15),"rest" to max(30,base.i("rest",60)),"libraryGen3845" to true);GroupEvolution.apply(e,cap(e,adjusted,rules.groupDose(e,adjusted,kind,x,logs,date,status,fb.strain),logs,date,status,fb),evolution)};val mins=minutes(obj("items" to arr(items)),adjusted);if(mins !in low..high)continue;val aim=if(range=="short")8 else if(range=="medium")15 else 25;val score=abs(aim-mins)*10.0+abs(count-if(range=="short")3 else 5)*2;if(score<winScore){winner=items;winMinutes=mins;winScore=score}}
  }
  val result=winner?:return null;return obj("id" to "hlib3845-$mode-$epoch-$type-$n-$variant","name" to "${groupLabels[type]} · ${listOf("Básico","Intermedio","Avanzado")[n]} · ${if(mode=="gym")"Gimnasio" else "Sin conexión"} ${variant+1}","category" to kindLabel(kind),"focus3845" to type,"mode3845" to mode,"level" to n,"items" to arr(result),"gymOnly23" to (mode=="gym"),"gymRequirements23" to arr(result.flatMap{catalog.byId[it.s("id")]!!.raw.a("gymRequirements23").strings()}.distinct()),"minutes" to winMinutes,"impact" to if(result.any{catalog.byId[it.s("id")]!!.impact})"Con saltos" else "Sin saltos","family" to "dynamic","safe381" to true,"description" to "Secuencia generada desde las demostraciones 2D locales, filtrada por categoría, equipo y restricciones del perfil. Estimación de tiempo con descansos. No altera las misiones del día.")
 }
}
