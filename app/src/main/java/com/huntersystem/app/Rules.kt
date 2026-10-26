package com.huntersystem.app

import org.json.JSONObject
import kotlin.math.*

data class Gate(val ok:Boolean,val reduced:Boolean=false,val reason:String="")
class Rules(val catalog:Catalog){
 fun injuries(p:JSONObject)=p.a("injuries").objects().filter{it.s("stage")!="past"}
 fun active(p:JSONObject):Boolean{val h=p.o("health");return injuries(p).isNotEmpty()||p.b("limits")||h.b("special")||h.b("acute")||h.b("redFlags")||h.s("pain","none")!="none"}
 fun impactStatus(p:JSONObject):Gate{
  val a=injuries(p);val h=p.o("health");val zones=a.map{it.s("zone")};val known=setOf("neck","shoulder","elbow","wrist","back","hip","knee","ankle")
  if(p.n("age")<18)return Gate(false,reason="Se requiere una planificación individual apropiada para la edad.")
  if(h.b("acute")||h.s("pain")=="strong"||a.any{it.s("type") in listOf("bone","surgery")}||listOf("emergency","neurological","unstable","redFlags").any{h.b(it)})return Gate(false,reason="Se registró dolor intenso, una lesión reciente grave o una condición que requiere valoración. No es seguro asignar automáticamente ejercicios, incluso para otras zonas.")
  if(zones.any{it !in known})return Gate(false,reason="Indica qué zona está afectada para excluirla de todos los movimientos. Una zona desconocida no puede filtrarse de forma fiable.")
  if(h.b("special")&&!h.b("approved"))return Gate(false,reason="Existe una condición especial que necesita indicaciones individuales antes de programar ejercicio.")
  if(p.b("limits")&&a.isEmpty())return Gate(false,reason="Marca la zona afectada en Perfil para encontrar grupos musculares alternativos.")
  if(h.s("pain") in listOf("mild","moderate")&&a.isEmpty())return Gate(false,reason="Selecciona la ubicación del dolor para evitar ejercicios que involucren esa zona.")
  return Gate(true,a.isNotEmpty()||h.s("pain") in listOf("mild","moderate"),if(a.isNotEmpty())"Alternativas de baja exigencia para grupos alejados de las zonas declaradas. El dolor puede limitar también movimientos de otras zonas." else "Planificación general.")
 }
 fun mode(p:JSONObject):Gate{
  val h=p.o("health");val a=injuries(p)
  if((a.isNotEmpty()||p.b("limits")||h.s("pain","none")!="none")&&impactStatus(p).ok)return Gate(true,true,"Entrenamiento limitado a otros grupos musculares. Ejercicios orientativos de intensidad reducida; no representan autorización médica.")
  if(p.n("age")<18)return Gate(false,reason="Organizador disponible. Para menores, utiliza un plan indicado por un profesional.")
  if(p.b("limits")&&!p.has("health"))return Gate(false,reason="Actualiza las zonas y restricciones de tu perfil para revisar la selección.")
  if(h.s("pain") in listOf("strong","moderate")||h.b("acute"))return Gate(false,reason="Dolor actual o lesión reciente: conserva tus misiones personales y revisa con un profesional antes de generar ejercicios.")
  if(a.any{it.s("type") in listOf("bone","surgery")||it.s("zone")=="other"})return Gate(false,reason="Este caso requiere indicaciones individuales. Puedes organizar aquí el plan que recibas.")
  if((a.isNotEmpty()||h.b("special"))&&(!h.b("approved")||!h.b("translated")))return Gate(false,reason="Completa las recomendaciones recibidas y confirma qué actividad te han permitido antes de filtrar ejercicios.")
  return Gate(true,a.isNotEmpty()||h.b("special")||p.n("age")>=65||h.s("pain")=="mild","Selección según objetivo, experiencia, equipo y calendario.")
 }
 fun gate(p:JSONObject):Gate{val h=p.o("health");return if(injuries(p).isNotEmpty()||p.b("limits")||h.b("special")||h.s("pain") in listOf("mild","moderate","strong"))impactStatus(p) else mode(p)}
 fun reduced(p:JSONObject,status:Gate?=null)=status?.reduced==true||active(p)||(if(p.has("_adaptationReduced"))p.b("_adaptationReduced") else mode(p).reduced)
 fun groupReasons(e:Exercise,p:JSONObject):List<String>{val out=mutableListOf<String>();val caps=p.a("capabilities46").strings();val noImpact=listOf("restrictions","physicalLimits","preferences").any{p.o(it).b("noImpact")}
  if(e.impact&&noImpact)out+="Saltos e impactos excluidos por el perfil"
  if((e.power||e.impact)&&(reduced(p)||p.n("age")<18))out+="Potencia e impactos pendientes de revisión individual"
  if(e.impact&&e.type!="upper"&&"landing" !in caps)out+="Confirma primero tu control de aterrizaje en Perfil"
  if(e.power&&e.type=="upper"&&"supporthold" !in caps)out+="Falta confirmar control de apoyos para el empuje explosivo"
  if(e.power&&"clean" in e.family&&("ballistichinge" !in caps||"olympic" !in caps))out+="Falta práctica previa de la cargada y su recepción"
  return out
 }
 fun materialReasons(e:Exercise,p:JSONObject):List<String>{val out=mutableListOf<String>();val raw=e.raw;val gear=p.s("equipment","body");val gym=p.a("gymEquipment23").strings();val mats=p.a("materials46").strings();val caps=p.a("capabilities46").strings();val req=raw.optJSONArray("materialRequirements")
  if(req==null)out+="Requisitos de material no disponibles" else for(k in req.strings())if(when(k){"db"->gear !in listOf("db","gym");"gym"->gear!="gym";"bench"->!p.b("bench")&&k !in gym;"step"->!p.b("step");else->gear!="gym"||k !in gym})out+="Falta material: $k"
  if(e.gym&&gear!="gym"||e.equipment=="gym"&&gear!="gym"||e.equipment=="db"&&gear !in listOf("db","gym"))out+="Equipo no configurado"
  if(raw.i("progressionTierNative")>level(p)&&!mode(p).reduced&&!catalog.editorial.o("goalPlans").o(p.s("finalTarget")).b("gentle"))out+="Nivel superior al seleccionado"
  if(e.planningLevel>level(p))out+="Nivel de planificación superior al seleccionado"
  if(raw.b("pack46")&&raw.has("materials46")){
   if(raw.i("level")>level(p))out+="Nivel superior al seleccionado"
   for(k in raw.a("materials46").strings()){
    val has=when(k){"db"->gear in listOf("db","gym");"bench"->p.b("bench")||"bench" in gym;"step"->p.b("step");"barbell","cable"->gear=="gym"&&k in gym;else->k in mats}
    if(!has)out+="Falta material: "+catalog.materials.s(k,k)
   }
   for(k in raw.a("capabilities46").strings())if(k !in caps)out+="Capacidad previa pendiente: "+catalog.capabilities.s(k,k)
   if(raw.b("highSkill46")&&(p.n("age")<18||active(p)))out+="Movimiento complejo: revisar capacidad y restricciones individuales antes de asignar"
   if(raw.b("loaded46")&&p.o("restrictions").b("noLoad"))out+="Carga o resistencia externa excluida"
  }
  return out.distinct()
 }
 fun clinicalReasons(e:Exercise,p:JSONObject,risk:Boolean):List<String>{val r=p.o("restrictions");val raw=e.raw;val out=mutableListOf<String>();val affected=injuries(p).map{it.s("zone")}.toSet();val floor=raw.b("floor");val seated=raw.b("seated");val standing=raw.b("standing");val hand=raw.b("wristLoad");val overhead=raw.b("overhead");val knee=raw.b("kneeBend");val spine=raw.b("spineMove");val external=e.equipment in listOf("db","gym")||"db" in raw.a("requirements").strings();val txt=(e.id+" "+e.name+" "+raw.s("steps")).lowercase()
  fun reject(v:Boolean,s:String){if(v)out+=s}
  reject(raw.a("zones").strings().any{it in affected},"Zona declarada afectada")
  reject(r.b("noImpact")&&raw.s("impact")=="Con saltos","Saltos excluidos")
  reject(r.b("noFloor")&&floor,"Trabajo en suelo excluido");reject(r.b("noOverhead")&&overhead,"Elevación sobre la cabeza excluida");reject(r.b("noKnee")&&knee,"Flexión de rodilla excluida");reject(r.b("noWrist")&&hand,"Apoyo sobre manos excluido");reject(r.b("noLoad")&&external,"Carga externa excluida");reject(r.b("seatedOnly")&&!seated,"Solo movimientos sentado");reject(r.b("noSpine")&&spine,"Flexión o giro de tronco excluido")
  if(!risk){reject(!mode(p).ok,"Pendiente de indicaciones individuales");reject(raw.i("level")>(if(mode(p).reduced)0 else if(level(p)==0)1 else 2),"Nivel superior al seleccionado");reject(mode(p).reduced&&raw.s("impact")=="Con saltos","Saltos excluidos");return out}
  reject(raw.b("sourceOnly")||raw.s("variant")!="base"||raw.i("level")>1||raw.s("impact")=="Con saltos","Ejercicio fuera del filtro de baja exigencia")
  reject("knee" in affected&&(knee||(!seated&&floor)||Regex("rodillas apoyadas|cuatro apoyos|cuadrupedia|arrodillad").containsMatchIn(txt)),"Rodilla implicada")
  reject("ankle" in affected&&(Regex("puente de gl[uú]teos|elevaci[oó]n de talones|heel raise|calf raise").containsMatchIn(txt)||!seated&&!floor||standing),"Tobillo implicado")
  reject("hip" in affected&&!seated&&Regex("sentadilla|zancada|estocada|peso muerto|bisagra").containsMatchIn(txt),"Cadera implicada")
  reject("wrist" in affected&&(hand||Regex("boxeo|golpes|puños|punch").containsMatchIn(txt)),"Muñeca implicada")
  reject("shoulder" in affected&&(overhead||hand||Regex("plancha|flexiones|push.?up|brazos elevados").containsMatchIn(txt)),"Hombro implicado")
  reject("neck" in affected&&(overhead||spine),"Cuello implicado");reject("back" in affected&&(spine||floor&&e.group in listOf("Core","Abdomen")||external&&standing),"Tronco implicado")
  val floorHand=floor&&!seated&&e.group in listOf("Pecho","Hombros","Core","Abdomen")
  reject((r.b("noWrist")||"wrist" in affected)&&(hand||floorHand),"Evitar apoyo en manos");reject("knee" in affected&&floor&&knee,"Rodilla en apoyo")
  return out
 }
 fun reasons(e:Exercise,p:JSONObject):List<String>{if(e.raw.b("sourceExpansion2k")&&!e.raw.b("approvedForAutoplan"))return listOf("Pendiente de validación biomecánica y de requisitos; excluido de los generadores.");val g=gate(p);if(!g.ok)return listOf(g.reason);val risk=injuries(p).isNotEmpty()||p.b("limits")||p.o("health").b("special")||p.o("health").s("pain") in listOf("mild","moderate","strong");return (groupReasons(e,p)+materialReasons(e,p)+clinicalReasons(e,p,risk)).distinct()}
 fun compatible(e:Exercise,p:JSONObject)=reasons(e,p).isEmpty()
 fun matches(e:Exercise,type:String)=e.type==type||type=="cardio"&&e.meta.b("jumpPower")
 fun inKind(e:Exercise,kind:String)=when(kind){"strength"->e.type in listOf("upper","lower","core");"cardio"->matches(e,"cardio");"recovery"->e.type=="mobility";else->false}
 fun pool(p:JSONObject,kind:String)=if(!gate(p).ok) emptyList() else catalog.exercises.filter{inKind(it,kind)&&compatible(it,p)}
 fun powerReady(p:JSONObject,logs:JSONObject,date:String,status:Gate=gate(p),strain:Boolean=false,planned:List<PlanRow> = emptyList()):Boolean{
  if(reduced(p,status)||strain||p.n("trainingScale",1.0)<1)return false
  val l=logs.o(date);if(l.b("pain")||l.b("fatigue")||l.n("rpe")>=8)return false
  val dates=mutableSetOf<String>();fun powerId(id:String)=catalog.byId[id]?.let{it.power||it.impact}?:catalog.exercises.firstOrNull{it.family==id}?.let{it.power||it.impact}?:false
  for((d,v) in logs.entries()){val log=v as? JSONObject?:continue;if(d>=date||!Regex("\\d{4}-\\d{2}-\\d{2}").matches(d))continue;if(log.a("items").objects().any{powerId(it.s("id"))&&(log.b("complete")||it.a("actual").objects().any{a->a.b("done")})}){if(distance(d,date)<2)return false;if(distance(d,date)<7)dates+=d}}
  for(row in planned)if(powerId(row.id)){if(distance(row.date,date)<2)return false;if(row.date<date&&distance(row.date,date)<7)dates+=row.date}
  return dates.size<3
 }
 fun selection(entries:List<Exercise>,selected:List<Exercise>,count:Int,p:JSONObject,kind:String,type:String,ready:Boolean):List<Exercise>{
  var list=entries.filter{ready||!it.power&&!it.impact};if(selected.any{it.power||it.impact})list=list.filter{!it.power&&!it.impact};if(list.isEmpty())return list
  val slot=if(kind=="cardio")count>=2 else count>=3&&type in listOf("lower","general")
  if(selected.isEmpty()&&slot&&ready){val powers=list.filter{it.power};if(powers.isNotEmpty())return powers}
  if(kind=="strength"){val needed=min(count-if(slot)1 else 0,max(1,ceil(count*.6).toInt()));val primaries=list.filter{it.primary};if(primaries.isNotEmpty()&&selected.count{it.primary}<needed)list=primaries else{val accessories=selected.count{it.role=="accessory"};if(accessories>=1||count<3)list=list.filter{it.role!="accessory"};if(selected.size==count-1&&count>=4&&accessories==0){val xs=list.filter{it.role=="accessory"};if(xs.isNotEmpty())list=xs}}}
  return list
 }
 fun score(e:Exercise,p:JSONObject,kind:String,selected:List<Exercise>):Double{var s=-38.0*selected.count{it.pattern==e.pattern};val n=level(p);if(kind=="strength"){s+=if(e.primary)30 else if(e.role=="accessory")-12 else 0;if(!reduced(p))s-=max(0,n-e.planningLevel)*35;if(p.s("equipment")=="gym"&&e.gym&&e.primary)s+=14;if(e.role=="accessory"&&Regex("tobillo|plantar").containsMatchIn(e.pattern))s-=35};if(kind=="cardio"&&!reduced(p))s+=e.meta.i("conditioning")*18;if(e.power)s-=abs(n-e.planningLevel)*16;return s}
 fun groupDose(e:Exercise,p:JSONObject,kind:String,item:JSONObject,logs:JSONObject=JSONObject(),date:String=today(),status:Gate=gate(p),strain:Boolean=false):JSONObject{
  val out=item.copyJson();val n=level(p);val l=logs.o(date);val limited=reduced(p,status)||strain||l.b("pain")||l.b("fatigue")||l.n("rpe")>=8;val scale=p.n("trainingScale",1.0).coerceIn(.25,1.0)
  if(e.power){val target=if(e.unit=="sec")listOf(8,10,12)[n] else listOf(3,4,5)[n];out.put("target",max(1,min(out.i("target",target),floor(target*scale).toInt())));out.put("sets",out.i("sets",1).coerceIn(1,if(limited)1 else 2));out.put("rest",90);out.put("noAutoLoad",true);out.put("safetyNote",listOf(out.s("safetyNote"),"Potencia: repeticiones de calidad; termina si pierdes la recepción o el control.").filter{it.isNotBlank()}.joinToString(" "))}
  else if(e.impact){out.put("target",max(1,min(out.i("target",1),if(e.unit=="sec")listOf(15,20,25)[n] else listOf(4,5,6)[n])));out.put("sets",out.i("sets",1).coerceIn(1,2));out.put("rest",out.i("rest").coerceIn(60,90))}
  else if(kind=="cardio"&&e.unit=="sec"&&!limited&&scale==1.0&&e.meta.i("conditioning")>=2)out.put("target",max(out.i("target",1),listOf(35,40,45)[n]))
  out.put("rest",out.i("rest",30).coerceIn(30,90));out.put("planning49",obj("group" to e.type,"role" to e.role,"pattern" to e.pattern));return out
 }
}
data class PlanRow(val date:String,val id:String,val group:String,val pattern:String,val sets:Int)
