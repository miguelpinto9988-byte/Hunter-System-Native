package com.huntersystem.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.*
import org.json.JSONObject

private val fieldLabels=mapOf("name" to "Nombre de cazador","age" to "Edad","height" to "Altura cm","weight" to "Peso kg","gender" to "Género","theme" to "Tema","difficulty" to "Nivel de entrenamiento","goalGroup" to "Objetivo final","finalGoal" to "Mi meta personal","goal" to "Objetivo principal","frequency" to "Sesiones principales por semana","equipment" to "Equipo","load" to "Peso disponible de una mancuerna · kg","restSeconds" to "Descanso entre series","blockMode22" to "Orden de los bloques","dailyMissions" to "Misiones diarias · 1 a 7","pain" to "¿Tienes dolor actualmente?","recommendations" to "Recomendaciones de tu profesional · opcional","collection33" to "Colección visual","scene33" to "Escenario")
private val healthFields=setOf("pain","acute","special","recommendations","approved","translated","redFlags")
@Composable fun ProfileScreen(ui:NativeUi){val s=ui.store;val old=s.profile;val form=s.catalog.profileForm.objects();val fields=form.flatMap{it.a("fields").objects()}
 val values=remember(old==null){mutableStateMapOf<String,String>().apply{
  val defaults=mapOf("gender" to "unspecified","theme" to "eclipse","difficulty" to "basic","goalGroup" to "habit","goal" to "general","frequency" to "2","equipment" to "body","restSeconds" to "30","dailyMissions" to "3","blockMode22" to "exercise","collection33" to "hunter","scene33" to "auto","trainingMode" to "challenges","companionCharacter" to "knight","trainingScale" to "1","pain" to "none")
  putAll(defaults)
  // Only visible fields belong to profile-form.json. Load historical settings
  // explicitly so removing obsolete form controls cannot discard saved preferences.
  for(f in fields){
   val name=f.s("name");val value=f.s("value");val type=f.s("type")
   val raw:Any?=when{
    old==null->null
    name.startsWith("r_")->old.o("restrictions").opt(name.removePrefix("r_"))
    name in healthFields->old.o("health").opt(name)
    else->old.opt(name)
   }
   if(type=="radio"){
    if(raw!=null&&raw!=JSONObject.NULL)put(name,raw.toString())
    else if(f.b("checked"))put(name,value)
   }else if(raw!=null&&raw!=JSONObject.NULL)put(name,raw.toString())
   else if(type=="checkbox")put(name,f.b("checked").toString())
   else if(!containsKey(name))put(name,value.ifBlank{f.a("options").objects().firstOrNull()?.s("value")?:""})
  }
  if(old!=null){
   put("theme",resolveV39Theme(old.s("theme"),old.s("gender")))
   for(name in listOf("restSeconds","dailyMissions","load","trainingScale","frequency","goal","blockMode22")){
    old.opt(name)?.takeUnless{it==JSONObject.NULL}?.let{put(name,it.toString())}
   }
   put("bench",old.b("bench").toString())
   put("step",old.b("step").toString())
   val equipment=old.a("gymEquipment23").strings().toSet()
   for(name in listOf("barbell","cable","machine","smith","cardio","bench"))
    put("gym_$name",(name in equipment).toString())
   for(name in listOf("materials46","capabilities46"))
    for(value in old.a(name).strings())put("$name|$value","true")
  }
  put("collection33",s.state.o("appearance33").s("collection","hunter"))
  put("scene33",s.state.o("appearance33").s("scene","auto"))
 }}
 val initialValues=remember(old==null){mutableStateMapOf<String,String>().apply{putAll(values)}}
 val injuries=remember(old==null){mutableStateListOf<JSONObject>().apply{addAll(old?.a("injuries")?.objects()?.map{it.copyJson()}?:emptyList())}}
 val week=remember(old==null){mutableStateListOf<String>().apply{addAll(old?.a("weekPlan")?.strings()?.takeIf{it.size==7}?:listOf("rest","train","rest","train","rest","rest","rest"))}}
 val focus=remember(old==null){mutableStateListOf<String>().apply{addAll(old?.a("weekFocus")?.strings()?.takeIf{it.size==7}?:List(7){"upper"})}}
 var injuryZone by remember{mutableStateOf("knee")};var injuryType by remember{mutableStateOf("joint")};var injuryStage by remember{mutableStateOf("current")}
 var manualGoalGroup by remember(old==null){mutableStateOf(old?.b("manualGoalGroupNative")?:false)}
 val expanded=remember{mutableStateMapOf<String,Boolean>()}
 fun saveProfile(){ui.attempt{
  // Appearance-only changes must leave the training profile and its plan intact.
  val visualOnly=old!=null&&
   values.all{(k,v)->k in listOf("theme","collection33","scene33")||v==initialValues[k]}&&
   week.toList()==old.a("weekPlan").strings()&&focus.toList()==old.a("weekFocus").strings()&&
   arr(injuries).toString()==old.a("injuries").toString()&&
   manualGoalGroup==old.b("manualGoalGroupNative")
  if(visualOnly){
   s.updateAppearanceOnly(values["theme"]?:"eclipse",
    obj("collection" to values["collection33"],"scene" to values["scene33"]),ui.pendingAvatar)
   initialValues.clear();initialValues.putAll(values);ui.pendingAvatar=null
   ui.open("saved",obj("title" to "Apariencia guardada",
    "body" to "Se actualizó la apariencia sin cambiar objetivos, equipo ni entrenamiento."))
   return@attempt
  }
  val p=old?.copyJson()?:JSONObject();val health=old?.o("health")?.copyJson()?:JSONObject();val restrictions=old?.o("restrictions")?.copyJson()?:JSONObject()
  // Preserve existing values and array order when only appearance is edited.
  // Defaults shown for a legacy profile are not implicit training changes.
  for((name,value)in values){if('|' in name||name.startsWith("gym_")||name.endsWith("33")||(old!=null&&value==initialValues[name]))continue;when{ name.startsWith("r_")->restrictions.put(name.removePrefix("r_"),value=="true");name in healthFields->health.put(name,if(name in listOf("pain","recommendations"))value else value=="true");name in listOf("age","height","weight","load","frequency","restSeconds","dailyMissions","trainingScale")->p.put(name,value.replace(',','.').toDoubleOrNull()?.takeIf{it.isFinite()}?:error("Completa "+(fieldLabels[name]?:"los datos del perfil")));else->p.put(name,value)}}
  p.put("manualGoalGroupNative",manualGoalGroup);p.put("injuries",arr(injuries));p.put("health",health);p.put("restrictions",restrictions);p.put("weekPlan",arr(week));p.put("weekFocus",arr(focus))
  // Save only the equipment actually relevant to the selected training environment.
  // In particular, old checked gym machines cannot remain active in bodyweight mode.
  val selectedEquipment=values["equipment"]?:"body"
  p.put("gymEquipment23",arr(selectedProfileGym(selectedEquipment,values)))
  p.put("materials46",arr(selectedProfileMaterials(selectedEquipment,values)))
  p.put("capabilities46",arr(selectedProfileCapabilities(selectedEquipment,values)))
  p.put("bench",selectedEquipment=="db"&&values["bench"]=="true")
  p.put("step",values["step"]=="true")
  if(selectedEquipment=="body")p.put("load",0.0)
  if(old==null||arr(injuries).toString()!=old.a("injuries").toString())p.put("limits",injuries.any{it.s("stage")!="past"})
  val appearance=obj("collection" to values["collection33"],"scene" to values["scene33"])
  s.saveProfile(p,appearance,ui.pendingAvatar);initialValues.clear();initialValues.putAll(values);ui.pendingAvatar=null;ReminderScheduler.schedule(s.context,s.state.o("reminders"));CreatorFeedNative.schedule(s.context,s.state.b("creatorAuto",true));ui.open("saved",obj("title" to "Configuración guardada","body" to "Tu acompañante aplicó la interpretación local de tus preferencias. Se revisó el plan de la semana actual. Los registros realizados se conservan."))
 }}
 SceneColumn(ui){Eyebrow("CAZADOR · CONFIGURACIÓN");Heading(if(old==null)"Crea tu perfil" else "Tu perfil de cazador");HText("Tu objetivo, nivel, equipo y calendario ajustan la selección de ejercicios.",color=LocalHunter.current.muted)
  for((sectionIndex,sec)in form.withIndex()){
   if(sectionIndex==1)HPanel{ThemeGallery(values["theme"]?:"eclipse"){values["theme"]=it}}
   HPanel{
   val title=when(sectionIndex){3->"Personaliza tu semana";form.lastIndex->"Lesiones y salud";else->sec.s("title")}
   if(sectionIndex==form.lastIndex){Row(Modifier.fillMaxWidth().clickable{expanded["health"]=expanded["health"]!=true},horizontalArrangement=Arrangement.SpaceBetween){Heading(title,size=30);HText(if(expanded["health"]==true)"−" else "+",size=28)};HText(if(injuries.isEmpty())"Sin lesiones registradas · tocar para editar" else "${injuries.size} zonas registradas · tocar para editar",size=15,color=LocalHunter.current.muted);if(expanded["health"]!=true)return@HPanel}else Heading(title,size=30)
   if(sectionIndex==0){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){Avatar(ui.pendingAvatar?:s.state.s("avatar"),values["name"]?:"C",Modifier.size(72.dp));Column(verticalArrangement=Arrangement.spacedBy(6.dp)){HButton("Elegir fotografía"){ui.chooseAvatar()};HText("Se guarda en este dispositivo",size=14,color=LocalHunter.current.muted)}}}
   val done=mutableSetOf<String>()
   for(f in sec.a("fields").objects()){
    val name=f.s("name");val type=f.s("type");if(name in listOf("age","height","weight")){if(name!="age")continue;Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){for(k in listOf("age","height","weight"))HInput(values[k]?:"",{values[k]=it},fieldLabels[k]?:k,Modifier.weight(1f),number=true)};continue}
    if(name=="r_noImpact")HButton("Restricciones e indicaciones adicionales ▾",Modifier.fillMaxWidth()){expanded["restrictions"]=expanded["restrictions"]!=true}
    if((name.startsWith("r_")||name in listOf("recommendations","approved","translated"))&&expanded["restrictions"]!=true)continue
    val key=name
    val label=fieldLabels[name]?:f.s("label").substringBefore("Seleccionado").trim()
    when{
     name=="equipment"->{
      HSelect(label,values[name]?:"body",f.a("options").objects().map{it.s("value") to it.s("label")}){values[name]=it}
      ProfileEquipmentOptions(values[name]?:"body",values,expanded,s.catalog)
     }
     name=="companionCharacter"->{if(!done.add(name))continue;MonarchCompanionPicker(values[name]?:"knight"){values[name]=it}}
     type=="radio"->{
      if(!done.add(name))continue
      val opts=sec.a("fields").objects().filter{it.s("name")==name}.map{
       it.s("value") to when(it.s("value")){
        "challenges"->"Desafíos diarios";"routines"->"Rutinas completas";else->it.s("label")
       }
      }
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
       for((choice,text)in opts)
        HButton(text,Modifier.weight(1f),primary=values[name]==choice){values[name]=choice}
      }
     }
     type=="checkbox"->HCheck(label,values[key]=="true"){values[key]=(values[key]!="true").toString()}
     type=="select"->HSelect(label,values[name]?:"",f.a("options").objects().map{it.s("value") to it.s("label")}){v->values[name]=v;if(name=="goalGroup")manualGoalGroup=true;if(name=="difficulty")values["restSeconds"]=mapOf("basic" to "30","intermediate" to "60","advanced" to "90")[v]?:"30"}
     else->HInput(values[name]?:"",{values[name]=it},label,number=type=="number",multiline=type=="textarea")
    }
    if(name=="finalGoal"){val result=s.goals.interpret(values[name]?:"",values["goalGroup"]?:"habit");val group=s.catalog.editorial.o("goalGroups").o(result.s("primary"));HNotice(group.s("name","Constancia y bienestar")+" · "+s.catalog.editorial.o("goalPlans").o(result.s("target")).s("why"))}
   }

   if(sectionIndex==3){
    HSelect("Descanso entre series",values["restSeconds"]?:"30",
     listOf("30" to "30 segundos","60" to "60 segundos","90" to "90 segundos")){values["restSeconds"]=it}
    V39WeekPicker(week,focus,onChanged={})
    if(values["trainingMode"]=="challenges"){
     HSelect("Número de misiones diarias",values["dailyMissions"]?:"3",
      (1..7).map{it.toString() to (if(it==1)"1 misión" else "$it misiones")}){values["dailyMissions"]=it}
    }
   }
   if(sectionIndex==1){
    HunterPicker(values["collection33"]?:"hunter",values["gender"]?:"unspecified"){values["collection33"]=it}
    BackgroundPicker(values["scene33"]?:"auto",values["collection33"]?:"hunter",values["gender"]?:"unspecified"){values["scene33"]=it}
   }
   if(sectionIndex==form.lastIndex){Heading("Zonas e historial",size=26);for((i,injury)in injuries.withIndex()){Row(verticalAlignment=Alignment.CenterVertically){HText("${zoneLabels[injury.s("zone")]?:injury.s("zone")} · ${typeLabels[injury.s("type")]?:injury.s("type")} · ${if(injury.s("stage")=="past")"Anterior" else "Actual"}",Modifier.weight(1f),size=16);HButton("×"){injuries.removeAt(i)}}};HSelect("Zona",injuryZone,zoneLabels.toList()){injuryZone=it};HSelect("Tipo",injuryType,typeLabels.toList()){injuryType=it};Chips(listOf("current" to "Actual","past" to "Anterior"),injuryStage){injuryStage=it};HButton("+ Añadir zona"){injuries+=obj("zone" to injuryZone,"type" to injuryType,"stage" to injuryStage)};HText("El filtro por zonas no constituye un diagnóstico ni un programa de rehabilitación.",size=14,color=LocalHunter.current.muted)}
   }
  }
  HButton("Guardar perfil",Modifier.fillMaxWidth(),primary=true){saveProfile()}
  HPanel{
   Heading("Rendimiento visual",size=29)
   HText("Un solo ajuste para imágenes, partículas, animaciones y mensajes del compañero.",size=16,color=LocalHunter.current.muted)
   for((id,label)in listOf(
    "max" to "Máxima calidad","balanced" to "Equilibrado","saver" to "Ahorro de energía"
   )){
    HButton(label,Modifier.fillMaxWidth(),primary=visualTier(s).id==id){
     s.state.put("visualQuality",id);s.save()
    }
   }
   when(visualTier(s)){
    VisualTier.MAX->HText("Imágenes hasta 4K si el archivo y la pantalla lo permiten; más partículas, efectos y orientación contextual detallada.",size=15,color=LocalHunter.current.muted)
    VisualTier.BALANCED->HText("Imágenes hasta Full HD, efectos moderados y mensajes adaptados a la misión y al ejercicio.",size=15,color=LocalHunter.current.muted)
    VisualTier.SAVER->HText("Imágenes hasta HD, efectos mínimos y mensajes breves predefinidos. Se respetan los ajustes de animación del dispositivo.",size=15,color=LocalHunter.current.muted)
   }
  }
  HPanel{Heading("Copia de seguridad",size=29);HText("Guarda tu perfil, calendario y registros en un archivo.",size=16,color=LocalHunter.current.muted);HButton("Exportar copia de seguridad"){ui.export()};HButton("Restaurar copia"){ui.import()}}
  HPanel{Heading("Entrenadores y contenido",size=29);HCheck("Actualizar entrenadores automáticamente",s.state.b("creatorAuto",true)){s.state.put("creatorAuto",!s.state.b("creatorAuto",true));s.save();CreatorFeedNative.schedule(s.context,s.state.b("creatorAuto",true))};HButton("Actualizar ahora"){ui.open("creator-refresh")};HButton("Recordatorios del sistema"){ui.open("reminders")};HButton("Fuentes y publicaciones"){ui.open("sources")};HText("Hunter System · Eclipse 41.0",size=14,color=LocalHunter.current.muted)}
 }
}
val zoneLabels=linkedMapOf("neck" to "Cuello","shoulder" to "Hombro","elbow" to "Codo","wrist" to "Muñeca o mano","back" to "Espalda","hip" to "Cadera","knee" to "Rodilla","ankle" to "Tobillo o pie","other" to "Otra zona / no sé")
val typeLabels=linkedMapOf("muscle" to "Muscular","tendon" to "Tendón","joint" to "Articulación o ligamento","bone" to "Fractura / lesión ósea","surgery" to "Después de una cirugía","unknown" to "Sin diagnóstico / otra")
