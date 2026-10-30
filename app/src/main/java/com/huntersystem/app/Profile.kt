package com.huntersystem.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
   ui.profileSaved()
   ui.message("Apariencia guardada. Regresaste al Panel.")
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
  s.saveProfile(p,appearance,ui.pendingAvatar);initialValues.clear();initialValues.putAll(values);ui.pendingAvatar=null;HunterAlarms.sync(s);CreatorFeedNative.schedule(s.context,s.state.b("creatorAuto",true));ui.profileSaved();ui.message("Perfil guardado. Tu planificación ya está aplicada en el Panel.")
 }}
 var profileStage by rememberSaveable{mutableIntStateOf(0)}
 // Los campos y el guardado son comunes a las tres etapas; navegar entre ellas
 // nunca genera un perfil nuevo ni descarta ediciones todavía sin guardar.
 key(profileStage){
 SceneColumn(ui,showHeader=false){
  if(old!=null)HButton("← VOLVER AL PANEL"){ui.closeProfile()}
  HunterProfileBanner(isNew=old==null,stage=profileStage)
  HunterProfileStageTabs(profileStage){profileStage=it}

  if(profileStage==0){
   HunterProfileSection(1,"Identidad del cazador",
    "Cada historia comienza con un nombre."){
    Row(verticalAlignment=Alignment.CenterVertically,
     horizontalArrangement=Arrangement.spacedBy(12.dp)){
     Avatar(ui.pendingAvatar?:s.state.s("avatar"),values["name"]?:"C",Modifier.size(72.dp))
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
      Eyebrow("AVATAR DEL PERFIL")
      HText("Personaliza tu fotografía. Se guarda solo en este dispositivo.",
       size=13,color=LocalHunter.current.muted)
      HButton("Cambiar fotografía ↗"){ui.chooseAvatar()}
     }
    }
    HunterProfileFields(form[0],setOf("name","age","height","weight"),
     values,expanded,s){manualGoalGroup=true}
   }
   HunterProfileSection(2,"Escoge tu cazador",
    "El protagonista de tu propia evolución. Desliza para escoger."){
    HunterPicker(values["collection33"]?:"hunter",values["gender"]?:"unspecified"){choice->
     values["collection33"]=choice
     if(choice=="hunter-male")values["gender"]="male"
     if(choice=="hunter-female")values["gender"]="female"
    }
    HunterProfileFields(form[1],setOf("gender"),values,expanded,s){manualGoalGroup=true}
   }
   HunterProfileSection(3,"Personalización del entorno",
    "Lugares mágicos para la tarjeta Monarca, sin ajustes adicionales."){
    BackgroundPicker(values["scene33"]?:"auto",
     values["collection33"]?:"hunter",values["gender"]?:"unspecified"){
       values["scene33"]=it
     }
   }
   HunterProfileSection(4,"Vista previa",
    "Así se mostrará la tarjeta de tu cazador en Sistema Monarca."){
    HunterProfileCardPreview(
     collection=values["collection33"]?:"hunter",
     gender=values["gender"]?:"unspecified",
     scene=values["scene33"]?:"auto",
     name=values["name"]?:"Cazador",
     level=s.stats().i("level",1),xp=s.stats().i("xp"))
   }
   HButton("CONTINUAR AL PLAN  →",Modifier.fillMaxWidth(),primary=true){
    profileStage=1
   }
  } else if(profileStage==1){
   HunterProfileSection(5,"Objetivo de tu historia",
    "Tus metas determinan tu ruta de entrenamiento."){
    HunterProfileFields(form[2],setOf("goalGroup","finalGoal"),
     values,expanded,s){manualGoalGroup=true}
   }
   HunterProfileSection(6,"Nivel y modalidad",
    "Escoge un punto de partida adecuado para ti."){
    Eyebrow("NIVEL DE ENTRENAMIENTO")
    HunterProfileFields(form[2],setOf("difficulty"),
     values,expanded,s){manualGoalGroup=true}
    Eyebrow("MODALIDAD")
    HunterProfileFields(form[3],setOf("trainingMode"),
     values,expanded,s){manualGoalGroup=true}
    // El descanso pertenece al nivel y la modalidad, no al calendario.
    HSelect("Descanso entre series",values["restSeconds"]?:"30",
     listOf("30" to "30 segundos","60" to "60 segundos","90" to "90 segundos")){
      values["restSeconds"]=it
     }
   }
   HunterProfileSection(7,"Tu calendario",
    "Selecciona tus días principales; el descanso también cuenta."){
    V39WeekPicker(week,focus,onChanged={})
    if(values["trainingMode"]=="challenges"){
     HSelect("Número de misiones diarias",values["dailyMissions"]?:"3",
      (1..7).map{it.toString() to (if(it==1)"1 misión" else it.toString()+" misiones")}){
        values["dailyMissions"]=it
       }
    }
   }
   HunterProfileSection(8,"Equipo disponible",
    "Solo se propondrán ejercicios compatibles con lo que tengas."){
    HunterProfileFields(form[2],setOf("equipment"),
     values,expanded,s){manualGoalGroup=true}
   }
   HunterProfileSection(9,"Salud y limitaciones",
    "Apartado privado y desplegable, sin sobrecargar el perfil."){
    Row(Modifier.fillMaxWidth().clickable{
      expanded["health"]=expanded["health"]!=true
     },horizontalArrangement=Arrangement.SpaceBetween,
     verticalAlignment=Alignment.CenterVertically){
     HText("Indicaciones y restricciones",size=17,bold=true)
     HText(if(expanded["health"]==true)"−" else "＋",size=24,
      color=LocalHunter.current.accent)
    }
    HText(if(injuries.isEmpty())"Sin zonas registradas · tocar para editar"
      else injuries.size.toString()+" zonas registradas · tocar para editar",
      size=13,color=LocalHunter.current.muted)
    if(expanded["health"]==true){
     HunterProfileFields(form.last(),
      form.last().a("fields").objects().map{it.s("name")}.toSet(),
      values,expanded,s){manualGoalGroup=true}
     Heading("Zonas e historial",size=23)
     for((i,injury)in injuries.withIndex()){
      Row(verticalAlignment=Alignment.CenterVertically){
       HText((zoneLabels[injury.s("zone")]?:injury.s("zone"))+" · "+
        (typeLabels[injury.s("type")]?:injury.s("type"))+" · "+
        (if(injury.s("stage")=="past")"Anterior" else "Actual"),
        Modifier.weight(1f),size=15)
       HButton("×"){injuries.removeAt(i)}
      }
     }
     HSelect("Zona",injuryZone,zoneLabels.toList()){injuryZone=it}
     HSelect("Tipo",injuryType,typeLabels.toList()){injuryType=it}
     Chips(listOf("current" to "Actual","past" to "Anterior"),injuryStage){
       injuryStage=it
     }
     HButton("+ Añadir zona"){
      injuries+=obj("zone" to injuryZone,"type" to injuryType,"stage" to injuryStage)
     }
     HText("El filtro por zonas no constituye un diagnóstico ni un programa de rehabilitación.",
      size=13,color=LocalHunter.current.muted)
    }
   }
   HButton("CONTINUAR AL SISTEMA  →",Modifier.fillMaxWidth(),primary=true){
    profileStage=2
   }
   HButton("← MI CAZADOR",Modifier.fillMaxWidth()){profileStage=0}
  } else {
   HunterProfileSection(10,"Tu compañero",
    "Desliza horizontalmente y selecciona tu acompañante."){
    // Se reutiliza el carrusel horizontal EXISTENTE, sin crear uno diferente.
    MonarchCompanionPicker(values["companionCharacter"]?:"knight"){
      values["companionCharacter"]=it
    }
   }
   HunterProfileSection(11,"Alarmas del cazador",
    "Organiza tus avisos sin saturar la edición del perfil."){
    HText("Personaliza las alarmas y sus días desde la pantalla dedicada.",
      size=15,color=LocalHunter.current.muted)
    HButton("Abrir mis alarmas  ↗",Modifier.fillMaxWidth()){
      ui.open("reminders")
    }
   }
   HunterProfileSection(12,"Rendimiento visual",
    "Personaliza la apariencia del sistema y su rendimiento."){
    ThemeGallery(values["theme"]?:"eclipse"){values["theme"]=it}
    for((id,label)in listOf(
      "max" to "Máxima calidad",
      "balanced" to "Equilibrado",
      "saver" to "Ahorro de energía")){
     HButton(label,Modifier.fillMaxWidth(),primary=visualTier(s).id==id){
      s.state.put("visualQuality",id);s.save()
     }
    }
    when(visualTier(s)){
     VisualTier.MAX->HText("Imágenes detalladas y animaciones según las preferencias del dispositivo.",
      size=14,color=LocalHunter.current.muted)
     VisualTier.BALANCED->HText("Calidad y rendimiento equilibrados.",
      size=14,color=LocalHunter.current.muted)
     VisualTier.SAVER->HText("Efectos reducidos para mejorar fluidez y ahorrar energía.",
      size=14,color=LocalHunter.current.muted)
    }
   }
   HunterProfileSection(13,"Copia y privacidad",
    "Tu progreso se conserva en tu dispositivo."){
    HText("Guarda tu perfil, calendario y registros en un archivo local.",
     size=15,color=LocalHunter.current.muted)
    HButton("Exportar copia de seguridad",Modifier.fillMaxWidth()){ui.export()}
    HButton("Restaurar copia",Modifier.fillMaxWidth()){ui.import()}
    HText("ENTRENADORES Y CONTENIDO",size=14,bold=true,
     color=LocalHunter.current.accent)
    HCheck("Actualizar entrenadores automáticamente",s.state.b("creatorAuto",true)){
     s.state.put("creatorAuto",!s.state.b("creatorAuto",true))
     s.save()
     CreatorFeedNative.schedule(s.context,s.state.b("creatorAuto",true))
    }
    HButton("Actualizar publicaciones",Modifier.fillMaxWidth()){ui.open("creator-refresh")}
    HButton("Fuentes y publicaciones",Modifier.fillMaxWidth()){ui.open("sources")}
   }
   HButton(if(old==null)"CREAR MI PERFIL  →" else "GUARDAR MI PERFIL  →",
    Modifier.fillMaxWidth(),primary=true){saveProfile()}
   HButton("← MI PLAN",Modifier.fillMaxWidth()){profileStage=1}
   HText("Al guardar regresarás al Panel principal, tanto si creas tu perfil como si lo editas.",
    size=13,color=LocalHunter.current.muted)
  }
 }
 }
}

val zoneLabels=linkedMapOf("neck" to "Cuello","shoulder" to "Hombro","elbow" to "Codo","wrist" to "Muñeca o mano","back" to "Espalda","hip" to "Cadera","knee" to "Rodilla","ankle" to "Tobillo o pie","other" to "Otra zona / no sé")
val typeLabels=linkedMapOf("muscle" to "Muscular","tendon" to "Tendón","joint" to "Articulación o ligamento","bone" to "Fractura / lesión ósea","surgery" to "Después de una cirugía","unknown" to "Sin diagnóstico / otra")
