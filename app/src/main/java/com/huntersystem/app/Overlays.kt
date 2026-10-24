package com.huntersystem.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONArray
import java.time.LocalDate
import kotlin.math.*

@Composable fun OverlayRoot(ui:NativeUi,overlay:Overlay){val c=LocalHunter.current;Dialog(onDismissRequest={ui.back()},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=true)){Column(Modifier.fillMaxWidth().fillMaxHeight(.97f).padding(horizontal=8.dp,vertical=6.dp).background(Brush.verticalGradient(listOf(c.surface,c.panel,c.bg)),RoundedCornerShape(23.dp)).border(1.dp,c.accent.copy(alpha=.42f),RoundedCornerShape(23.dp)).padding(15.dp)){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){HButton("‹"){ui.back()};Column(horizontalAlignment=Alignment.CenterHorizontally){Eyebrow("✦  HUNTER SYSTEM");HText("SISTEMA DE SOMBRAS",size=10,color=c.muted)};HButton("×"){ui.stack.clear()}}
 Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top=18.dp,bottom=20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){key(overlay){when(overlay.type){
 "exercise"->ExerciseDetail(ui,overlay.data.s("id"))
 "mission"->{val date=overlay.data.s("date",today());Eyebrow(date);MissionContent(ui,date);if(date==today())HButton("Abrir sesión paso a paso",primary=true){ui.open("session",obj("date" to date))}}
 "record"->SessionRecord(ui,overlay.data.s("date",today()))
 "series"->SeriesEditor(ui,overlay.data)
 "routine"->RoutineDetail(ui,overlay.data.s("id"))
 "assign"->AssignRoutine(ui,overlay.data.s("id"))
 "session"->SessionPlayer(ui,overlay.data)
 "custom","routine-editor"->CustomEditor(ui,overlay.type,overlay.data)
 "academy"->Academy(ui)
 "guide"->GuideDetail(ui,overlay.data.i("index"))
 "body"->BodyLibrary(ui)
 "evolution"->Evolution(ui)
 "sources"->Sources(ui)
 "creator"->CreatorDetail(ui,overlay.data.s("key"))
 "creator-refresh"->CreatorRefresh(ui)
 "coach"->CoachPanel(ui)
 "reminders"->ReminderPanel(ui)
 "goal-edit"->GoalEditor(ui,overlay.data)
 "delete-goal"->{Heading("Eliminar objetivo");HText(overlay.data.s("title",overlay.data.s("text")));HButton("Eliminar",primary=true){ui.store.removeGoal(overlay.data.s("id"));ui.back()}}
 "confirm-import"->{Heading("Restaurar copia");HText("Se sustituirá el guardado de este dispositivo por el contenido de la copia seleccionada.");HText("Perfil: ${overlay.data.o("profile").s("name")} · ${overlay.data.o("logs").length()} registros");HButton("Restaurar esta copia",primary=true){ui.attempt{ui.store.restore(overlay.data);ui.stack.clear();ui.go(if(ui.store.profile==null)"profile" else "home");ui.message("Copia restaurada")}}}
 "saved"->{Heading(overlay.data.s("title"));HNotice(overlay.data.s("body"));HButton("Continuar",Modifier.fillMaxWidth(),primary=true){ui.back()}}
 "pain"->{Heading("Detener por molestias");HNotice("Si aparece dolor nuevo o empeoran las molestias, detén la sesión y actualiza las zonas y restricciones de tu perfil.");var note by remember{mutableStateOf("")};HInput(note,{note=it},"Anota lo que sentiste",multiline=true);HButton("Detener y guardar",primary=true){ui.attempt{ui.store.pain(overlay.data.s("date",today()),note);ui.stack.clear();ui.go("plan")}}}
 "video"->{Heading("Demostración sin conexión",size=31);NativeVideo(overlay.data.s("path"),Modifier.fillMaxWidth().height(450.dp))}
 "history"->HistoryDetail(ui,overlay.data.s("date"))
 "review"->ReviewPanel(ui)
 else->HNotice("Selecciona una sección para continuar.")
 }}}
 }}}
@Composable fun ExerciseDetail(ui:NativeUi,id:String){val s=ui.store;val e=s.catalog.byId[id]?:return;val c=LocalHunter.current;var playing by remember(id){mutableStateOf(s.profile?.b("animate",true)?:true)};var mirror by remember(id){mutableStateOf(false)};var speed by remember(id){mutableFloatStateOf(s.state.n("demoSpeed",1.0).toFloat())};var seek by remember(id){mutableFloatStateOf(0f)}
 Eyebrow("${e.group} · ${e.category}");Heading(e.name,size=35);HText("${if(e.unit=="sec")"Por tiempo" else "Por repeticiones"}${if(e.sides)" · Cantidad por lado" else ""} · ${if(e.impact)"Con saltos" else "Sin saltos"}",size=16,color=c.muted)
 Box(Modifier.fillMaxWidth().height(352.dp).clip(RoundedCornerShape(21.dp)).background(Brush.verticalGradient(listOf(c.surface,c.panel))).border(1.dp,c.accent.copy(alpha=.38f),RoundedCornerShape(21.dp)).padding(8.dp)){OriginalSvg(e.demo,Modifier.fillMaxSize(),play=playing,speed=speed,mirror=mirror,progress=seek)}
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){HButton(if(playing)"Ⅱ Pausar" else "▶ Reproducir",Modifier.weight(1f),primary=true){playing=!playing};HButton("⇄ Espejo",Modifier.weight(1f)){mirror=!mirror}}
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){HSelect("Velocidad",speed.toString(),listOf("0.5" to "0,5×","1.0" to "1×","1.5" to "1,5×","2.0" to "2×"),Modifier.weight(1f)){speed=it.toFloat();s.state.put("demoSpeed",speed.toDouble());s.save()};HButton("Reiniciar",Modifier.weight(1f)){playing=false;seek=0f}}
 HText("Ver posiciones",size=14,color=c.muted);Slider(seek,{seek=it;playing=false})
 HText("La velocidad visual no cambia el reloj ni las cantidades de la sesión.",size=14,color=c.muted)
 s.catalog.editorial.o("clips").optJSONObject(id)?.let{clip->val local="offline2d/"+clip.s("file");if(runCatching{s.context.assets.open(local).close()}.isSuccess)HButton("Ver clip local"){playing=false;ui.open("video",obj("path" to local))}}
 for((key,label)in listOf("setup" to "Preparación","instructions" to "Ejecución","control" to "Control","breath" to "Respiración","tempo" to "Ritmo","muscles" to "Músculos implicados","cue" to "Punto clave")){val text=if(key=="instructions")e.raw.a(key).strings().mapIndexed{i,t->"${i+1}. $t"}.joinToString("\n") else e.raw.s(key);if(text.isNotBlank())HPanel(decor=false,padding=16){Heading(label,size=25);HText(text,size=17)}}
 if(e.sides)HNotice("Realiza la cantidad indicada en cada lado. El registro especifica si corresponde a un lado o a ambos.")
 if(s.profile!=null){val reasons=s.rules.reasons(e,s.adjusted());HPanel(decor=false,padding=16){Heading("Compatibilidad con tu perfil",size=26);HText(if(reasons.isEmpty())"Incluido por los filtros del perfil actual" else reasons.joinToString("\n"){"· $it"},size=16)}}
 HText("Demostración esquemática original. Consulta las instrucciones; la animación no evalúa tu ejecución.",size=14,color=c.muted)
}
@Composable fun SessionRecord(ui:NativeUi,date:String){val s=ui.store;val l=remember(date){runCatching{s.log(date)}.getOrNull()};if(l==null){HNotice("El registro está disponible en la fecha de hoy.");return};val closed=l.b("complete")||l.b("pain");var rpe by remember(date){mutableStateOf(l.i("rpe").toString())};var notes by remember(date){mutableStateOf(l.s("notes"))};var fatigue by remember(date){mutableStateOf(l.b("fatigue"))};val latestRpe by rememberUpdatedState(rpe);val latestNotes by rememberUpdatedState(notes);val latestFatigue by rememberUpdatedState(fatigue)
 DisposableEffect(date){onDispose{if(!l.b("complete")&&!l.b("pain")){val n=latestRpe.toIntOrNull()?.coerceIn(0,10)?:0;if(l.i("rpe")!=n||l.s("notes")!=latestNotes||l.b("fatigue")!=latestFatigue){l.put("rpe",n);l.put("notes",latestNotes.take(3000));l.put("fatigue",latestFatigue);s.save()}}}}
 Eyebrow("REGISTRO DETALLADO · $date");Heading("Cada serie cuenta.");HNotice("Confirma únicamente lo que hayas realizado. Marcar la misión no añade series ni cantidades a este registro.")
 for((i,x)in l.a("items").objects().withIndex()){val e=s.catalog.byId[x.s("id")]?:continue;HPanel(padding=15){Heading(e.name,size=27);if(x.b("archived"))HNotice(x.s("archiveReason"));HText("${x.i("sets")} × ${x.i("target")} ${if(e.unit=="sec")"s" else "repeticiones"}${if(e.sides)" por lado" else ""}",size=15);for(si in 0 until x.i("sets")){val actual=x.a("actual").optJSONObject(si);Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){HText("${if(actual?.b("done")==true)"✓" else "○"} Serie ${si+1}",Modifier.weight(1f),size=17);HText(if(actual?.b("done")==true)"${actual.n("value")} ${if(e.unit=="sec")"s" else "rep"} · ${actual.n("kg")} kg" else "Pendiente",size=15,color=LocalHunter.current.muted);if(!closed&&!x.b("archived"))HButton("Registrar"){ui.open("series",obj("date" to date,"itemIndex" to i,"setIndex" to si))}}}}}
 if(!closed){HSelect("Esfuerzo percibido · RPE",rpe,listOf("0" to "Seleccionar esfuerzo")+(1..10).map{it.toString() to "$it / 10"}){rpe=it};HInput(notes,{notes=it},"Notas del día",multiline=true);HCheck("He sentido fatiga",fatigue){fatigue=!fatigue};HButton("Guardar registro parcial",Modifier.fillMaxWidth()){ui.attempt{s.feedback(date,rpe.toIntOrNull()?:0,notes);l.put("fatigue",fatigue);s.save();ui.message("Registro parcial guardado")}};HButton("Completar sesión",Modifier.fillMaxWidth(),primary=true){ui.attempt{s.feedback(date,rpe.toIntOrNull()?:0,notes);l.put("fatigue",fatigue);s.finish(date);ui.message("Misión completada");ui.stack.clear()}};HButton("Detener por molestias",Modifier.fillMaxWidth()){ui.open("pain",obj("date" to date))}}else HNotice(if(l.b("complete"))"✓ Sesión completada. Los registros se conservan." else "Sesión detenida por molestias.")
}
@Composable fun SeriesEditor(ui:NativeUi,data:JSONObject){val s=ui.store;val date=data.s("date",today());val l=s.logs.optJSONObject(date)?:return;val itemIndex=data.i("itemIndex");val setIndex=data.i("setIndex");val x=l.a("items").optJSONObject(itemIndex)?:return;val e=s.catalog.byId[x.s("id")]?:return;val a=x.a("actual").optJSONObject(setIndex)?:JSONObject();var amount by remember{mutableStateOf(a.n("value",x.n("target")).let{if(it%1==0.0)it.toInt().toString() else it.toString()})};var load by remember{mutableStateOf(a.n("kg").toString())}
 Heading(e.name,size=32);Eyebrow("SERIE ${setIndex+1} DE ${x.i("sets")}");HInput(amount,{amount=it},if(x.s("unit",e.unit)=="sec")"Segundos realizados" else "Repeticiones realizadas",number=true);if(e.sides)HText("Indica la cantidad por lado.",size=16,color=LocalHunter.current.muted);if(e.equipment!="body")HInput(load,{load=it},"Carga utilizada · kg",number=true)
 HButton("Confirmar serie realizada",Modifier.fillMaxWidth(),primary=true){ui.attempt{s.confirmSet(date,itemIndex,setIndex,amount.replace(',','.').toDouble(),load.replace(',','.').toDouble());ui.back()}}
 if(a.b("done"))HButton("Desmarcar esta serie",Modifier.fillMaxWidth()){ui.attempt{s.confirmSet(date,itemIndex,setIndex,amount.replace(',','.').toDouble(),load.replace(',','.').toDouble(),false);ui.back()}}
}
@Composable fun RoutineDetail(ui:NativeUi,id:String){val s=ui.store;val r=ui.routine(id)?:return;Eyebrow(r.s("category"));Heading(r.s("name"),size=35);HText(r.s("description"),size=17,color=LocalHunter.current.muted);HText("≈ ${s.planner.minutes(r,s.adjusted())} min · ${r.a("items").length()} ejercicios",size=16);val phase=s.planner.phase(r.a("items").objects(),s.adjusted());if(phase!=null)HPanel(onClick={ui.open("exercise",obj("id" to phase.id))},padding=15){HText("Calentamiento progresivo · ${phase.name}",bold=true);HText("${s.planner.phaseSeconds(s.adjusted())} s",size=15)}
 for(x in r.a("items").objects()){val e=s.catalog.byId[x.s("id")]?:continue;HPanel(onClick={ui.open("exercise",obj("id" to e.id))},padding=16){Heading(e.name,size=26);HText("${x.i("sets")} × ${x.i("target")} ${if(e.unit=="sec")"segundos" else "repeticiones"}${if(e.sides)" por lado" else ""} · ${x.i("rest",s.planner.rest(s.adjusted()))} s descanso  ↗",size=16,color=LocalHunter.current.muted)}}
 if(phase!=null)HNotice("Cierre suave · ${phase.name} · ${s.planner.phaseSeconds(s.adjusted())} s")
 val reasons=r.a("items").objects().flatMap{x->s.catalog.byId[x.s("id")]?.let{s.rules.reasons(it,s.adjusted())}?:listOf("Ejercicio desconocido")}.distinct();if(reasons.isNotEmpty())HNotice("Revisa compatibilidad y equipo en Perfil:\n"+reasons.joinToString("\n"){"· $it"})
 HButton("Ver sesión paso a paso",Modifier.fillMaxWidth()){ui.open("session",obj("routineId" to id))};HButton("Usar esta rutina",Modifier.fillMaxWidth(),primary=true,enabled=reasons.isEmpty()){ui.open("assign",obj("id" to id))}
}
@Composable fun AssignRoutine(ui:NativeUi,id:String){val r=ui.routine(id)?:return;var date by remember{mutableStateOf(today())};Heading("Usar esta rutina",size=35);HText(r.s("name"),bold=true);HText("Los ejercicios y cantidades mostrados se copiarán a la fecha elegida.",size=17,color=LocalHunter.current.muted);HSelect("Fecha",date,(0L..20L).map{val d=plus(today(),it);d to "$d${if(it==0L)" · Hoy" else ""}"}){date=it};for(x in r.a("items").objects())HText("${ui.store.catalog.byId[x.s("id")]?.name} · ${x.i("sets")} × ${x.i("target")}",size=16);HButton("Confirmar asignación",Modifier.fillMaxWidth(),primary=true){ui.attempt{ui.store.assign(r,date);ui.stack.clear();ui.go("plan");ui.message("Rutina asignada a $date")}}}
@Composable fun SessionPlayer(ui:NativeUi,data:JSONObject){val s=ui.store;val date=data.s("date",today());val preview=data.has("routineId");val day=if(preview)ui.routine(data.s("routineId"))?:return else s.day(date);val p=s.adjusted();val reasons=day.a("items").objects().flatMap{item->s.catalog.byId[item.s("id")]?.let{s.rules.reasons(it,p)}?:listOf("Ejercicio desconocido")};if(reasons.isNotEmpty()){Heading("Revisa tu perfil");HNotice(reasons.distinct().joinToString("\n"));return};val steps=remember(day.toString(),p.toString()){s.planner.steps(day,p)};if(steps.isEmpty()){Heading("No hay ejercicios para esta sesión");return};val signature=hash(arr(steps).toString()).toString(16);val saved=if(preview)JSONObject() else s.state.o("guidedSessions").o(date).takeIf{it.s("nativeSignature")==signature}?:JSONObject();var index by remember(signature){mutableIntStateOf(saved.i("index").coerceIn(0,steps.size))};var remaining by remember(signature){mutableIntStateOf(saved.i("remaining",steps.getOrNull(index)?.i("seconds")?:0).coerceAtLeast(0))};var running by remember{mutableStateOf(false)};var pending by remember(index){mutableStateOf(false)};var speed by remember{mutableFloatStateOf(s.state.n("demoSpeed",1.0).toFloat())};var mirror by remember{mutableStateOf(false)};val scope=rememberCoroutineScope();val latestIndex by rememberUpdatedState(index);val latestRemaining by rememberUpdatedState(remaining)
 fun persist(){if(!preview){val all=s.state.o("guidedSessions");all.put(date,obj("nativeSignature" to signature,"index" to latestIndex,"remaining" to latestRemaining,"mode22" to p.s("blockMode22","exercise")));s.state.put("guidedSessions",all);s.save()}}
 val lifecycle=LocalLifecycleOwner.current.lifecycle;DisposableEffect(lifecycle,signature){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP){running=false;persist()}};lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer);persist()}}
 LaunchedEffect(running,index){while(running&&remaining>0){delay(1000);remaining--;if(remaining==0){running=false;TimerSound.play(s.context)}}}
 Eyebrow(if(preview)"VISTA PREVIA DE LA SESIÓN" else "SESIÓN GUIADA · $date");Heading(day.s("title",day.s("name")),size=31);HProgress(index.toFloat()/steps.size);HText("Paso ${min(index+1,steps.size)} de ${steps.size}",size=15,color=LocalHunter.current.muted)
 if(index>=steps.size){Heading("Secuencia terminada",size=32);HNotice("El registro se completa únicamente con las series o misiones que confirmes.");if(!preview&&date==today())HButton("Revisar y completar sesión",primary=true){ui.open("record",obj("date" to date))};return}
 val step=steps[index];Heading(step.s("label"),size=28);val e=s.catalog.byId[step.s("id")];if(e!=null)OriginalSvg(e.demo,Modifier.fillMaxWidth().height(330.dp),play=running,speed=speed,mirror=mirror)
 Heading("${remaining/60}:${(remaining%60).toString().padStart(2,'0')}",size=58);if(step.s("kind")=="exercise")HText("Serie ${step.i("setIndex")+1}/${step.i("sets")} · ${step.i("target")} ${if(step.s("unit")=="sec")"segundos" else "repeticiones"}${if(e?.sides==true)" por lado" else ""}",bold=true)
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){HButton(if(running)"Ⅱ Pausar" else "▶ Iniciar",Modifier.weight(1f),primary=true){running=!running};HButton("Reiniciar",Modifier.weight(1f)){running=false;remaining=step.i("seconds")}}
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){HSelect("Velocidad visual",speed.toString(),listOf("0.5" to "0,5×","1.0" to "1×","1.5" to "1,5×"),Modifier.weight(1f)){speed=it.toFloat()};HButton("⇄ Espejo",Modifier.weight(1f)){mirror=!mirror}}
 if(step.s("kind")=="exercise"&&!preview&&date==today())HButton("Registrar esta serie",Modifier.fillMaxWidth()){running=false;ui.attempt{s.log(date);ui.open("series",obj("date" to date,"itemIndex" to step.i("itemIndex"),"setIndex" to step.i("setIndex")))}}
 if(pending)HNotice("La serie sigue pendiente. Pulsa de nuevo «Siguiente» para avanzar sin registrarla.")
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){HButton("‹ Anterior",Modifier.weight(1f),enabled=index>0){running=false;index--;remaining=steps[index].i("seconds");pending=false};HButton("Siguiente ›",Modifier.weight(1f)){running=false;val done=preview||step.s("kind")!="exercise"||s.logs.o(date).a("items").optJSONObject(step.i("itemIndex"))?.a("actual")?.optJSONObject(step.i("setIndex"))?.b("done")==true;if(!done&&!pending)pending=true else{index++;remaining=steps.getOrNull(index)?.i("seconds")?:0;pending=false;persist()}}}
 if(!preview&&date==today())HButton("Detener por molestias"){running=false;ui.open("pain",obj("date" to date))}
}
