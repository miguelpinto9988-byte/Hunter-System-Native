package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Hunter System V42.4 — Fase 05: Misiones Monarca.
 *
 * Vista estrictamente nativa y offline. Los datos proceden del motor real.
 * No se crean registros hasta que el usuario marca o guarda una actividad.
 * El registro de series sigue siendo la única fuente de repeticiones y cargas.
 */
private object MissionHue {
    val upper = Color(0xFF438DFF)
    val lower = Color(0xFFF378B4)
    val cardio = Color(0xFFF9AE57)
    val mobility = Color(0xFF48CDB2)
    val rest = Color(0xFF8490A3)
}

private data class MissionDayStyle(
    val title: String,
    val short: String,
    val tint: Color
)

private fun missionDayStyle(store:NativeStore, date:String, day:JSONObject):MissionDayStyle {
    val kind = day.s("kind")
    val focus = store.profile?.a("weekFocus")?.optString(weekday(date), "upper") ?: "upper"
    return when (kind) {
        "strength" -> if (focus == "lower")
            MissionDayStyle("Fuerza · tren inferior", "INF", MissionHue.lower)
        else MissionDayStyle("Fuerza · tren superior", "SUP", MissionHue.upper)
        "cardio" -> MissionDayStyle("Cardio", "CAR", MissionHue.cardio)
        "recovery" -> MissionDayStyle("Movilidad y recuperación", "MOV", MissionHue.mobility)
        "mixed" -> MissionDayStyle("Entrenamiento mixto", "MIX", MissionHue.upper)
        else -> MissionDayStyle("Descanso programado", "DES", MissionHue.rest)
    }
}

private fun missionState(log:JSONObject?, date:String):String = when {
    log?.b("complete") == true -> "COMPLETADA"
    log?.b("pain") == true -> "DETENIDA"
    log != null -> "EN CURSO"
    date > today() -> "PROGRAMADA"
    date < today() -> "SIN REGISTRO"
    else -> "PENDIENTE"
}

@Composable
fun MonarchMissionsScreen(ui:NativeUi) {
    val store=ui.store
    val revision=store.revision.intValue
    var selected by rememberSaveable { mutableStateOf(today()) }
    // Fuerza la relectura sin sustituir los datos del motor por estado UI.
    SceneColumn(ui) {
        TopTabs(ui)
        MonarchMissionsTitle(store)
        TrainingMode(ui)
        key(revision) {
            MonarchMissionsWeek(ui, selected) { selected = it }
        }
        MonarchMissionContent(ui, selected)
        key(revision) { MonarchMissionsProgress(ui) }
        MonarchMissionsActions(ui)
        key(revision) { MonarchMissionsCalendar(ui) }
    }
}

@Composable
private fun MonarchMissionsTitle(store:NativeStore) {
    val colors=LocalHunter.current
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Eyebrow("SISTEMA MONARCA  /  PANEL DE MISIONES")
        Heading("Tus misiones",size=34)
        HText("Entrena según tu calendario, registra avances reales y mantén tu ritmo.",
            size=15,color=colors.muted)
        HText("CICLO ${store.state.a("cycles").length().coerceAtLeast(1).toString().padStart(2,'0')}",
            size=13,color=colors.accent,bold=true)
    }
}

@Composable
private fun MonarchMissionsWeek(ui:NativeUi, selected:String, onSelect:(String)->Unit) {
    val s=ui.store
    val c=LocalHunter.current
    val current=LocalDate.parse(today())
    val monday=current.minusDays((current.dayOfWeek.value-1).toLong())
    val names=listOf("LU","MA","MI","JU","VI","SA","DO")
    val days=(0L..6L).map { monday.plusDays(it).toString() }
    HPanel(padding=14) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically) {
            Column {
                Eyebrow("01  ·  SEMANA ACTUAL")
                Heading("Escoge tu día",size=23)
            }
            HText("${monday.dayOfMonth}–${monday.plusDays(6).dayOfMonth}",
                size=16,bold=true,color=c.accent)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
            days.forEachIndexed { i,date ->
                val day=s.day(date)
                val style=missionDayStyle(s,date,day)
                val complete=s.logs.o(date).b("complete")
                val isSelected=selected==date
                val shape=RoundedCornerShape(12.dp)
                Column(Modifier.weight(1f).defaultMinSize(minHeight=91.dp)
                    .clip(shape)
                    .background(if(isSelected)c.selected else c.input)
                    .border(if(isSelected)1.5.dp else .8.dp,
                        if(isSelected)c.accent else c.line,shape)
                    .clickable(role=Role.Button){onSelect(date)}
                    .semantics { contentDescription = "${names[i]}, ${style.title}, ${if(complete)"completado" else missionState(s.logs.optJSONObject(date),date)}" }
                    .padding(horizontal=1.dp,vertical=9.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    HText(names[i],size=11,color=if(isSelected)c.accent else c.muted,bold=true)
                    HText(if(complete)"✓" else LocalDate.parse(date).dayOfMonth.toString(),
                        size=17,color=c.text,bold=isSelected||complete)
                    Box(Modifier.size(17.dp,4.dp).clip(RoundedCornerShape(3.dp))
                        .background(style.tint))
                    HText(style.short,size=10,color=style.tint,bold=true)
                }
            }
        }
        HText("Azul: superior   ·   Rosa: inferior   ·   Naranja: cardio   ·   Verde: movilidad",
            size=12,color=c.muted)
        HButton("REVISAR OTRAS FECHAS  ↗",Modifier.fillMaxWidth()) {
            ui.open("mission-calendar")
        }
    }
}

/** Utilizado por la pestaña de misiones y por el overlay de fechas históricas. */
@Composable
fun MonarchMissionContent(ui:NativeUi,date:String) {
    val s=ui.store
    val c=LocalHunter.current
    // Suscripción a NativeStore: log y día son objetos JSON, no snapshot state.
    val revision=s.revision.intValue
    key(date,revision) {
        val day=s.day(date)
        val log=s.logs.optJSONObject(date)
        val items=(log?.a("items")?:day.a("items")).objects()
        val tasks=(log?.a("missionTasks")?:day.a("tasks")).objects()
        val style=missionDayStyle(s,date,day)
        val readOnly=date!=today()||log?.b("complete")==true||log?.b("pain")==true
        val canCustomize=date>=today()&&!s.activeLog(log)
        val fraction=s.fraction(log).toFloat().coerceIn(0f,1f)
        val progress=(fraction*100f).roundToInt()
        val hasWorkout=items.isNotEmpty() && day.s("kind")!="rest"
        val canStart=date==today()&&!readOnly&&hasWorkout
        HPanel(padding=18) {
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically) {
                Eyebrow("02  ·  MISIÓN SELECCIONADA")
                HText(missionState(log,date),size=12,bold=true,
                    color=if(log?.b("pain")==true)MissionHue.lower else c.accent)
            }
            Heading(day.s("title","Misión del día"),size=27)
            HText(date+"   ·   "+style.title,size=15,bold=true,color=style.tint)
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically) {
                HText("OBJETIVOS DE LA SESIÓN",size=12,color=c.muted,bold=true)
                HText("$progress %",size=19,color=c.accent,bold=true)
            }
            HProgress(fraction)
            HText("${items.size} ejercicios · ${tasks.size} tareas",
                size=14,color=c.muted)
            if(date>today())HNotice("Vista previa. No se permite confirmar ni registrar actividades futuras.")
            if(date<today())HNotice("Historial de solo lectura. Los días anteriores no pueden modificarse.")
            if(log?.b("complete")==true)HNotice("✓ Misión completada y guardada.")
            if(log?.b("pain")==true)HNotice("Sesión detenida por molestias. Consulta tu perfil antes de entrenar.")
            day.s("blocked381").takeIf{it.isNotBlank()}?.let{HNotice(it)}
            if(items.isEmpty()&&tasks.isEmpty())HNotice(
                if(day.s("kind")=="rest")"Día de descanso elegido en tu calendario."
                else "Esta misión todavía no tiene ejercicios compatibles. Revisa tu Perfil."
            )
            if(items.isNotEmpty()) {
                Eyebrow("EJERCICIOS DE LA MISIÓN")
                items.forEachIndexed { index,item ->
                    val e=s.catalog.byId[item.s("id")]
                    if(e!=null)MonarchMissionExercise(ui,date,index,item,
                        readOnly,style.tint)
                }
            }
            if(tasks.isNotEmpty()) {
                Eyebrow("TAREAS COMPLEMENTARIAS")
                tasks.forEachIndexed { index,task ->
                    val done=task.b("done")
                    MonarchMissionCheck(
                        label=task.s("text","Actividad programada"),
                        done=done,
                        enabled=!readOnly,
                        subtitle=if(done)"Marcada como completada" else "Pendiente"
                    ) {ui.attempt{s.toggleTask(date,index)}}
                }
            }
            HText("Las casillas confirman actividades realizadas; no añaden series, repeticiones ni cargas. Para registrar cantidades utiliza «Registro detallado».",
                size=13,color=c.muted)
            if(canStart) {
                HButton("▶  INICIAR SESIÓN GUIADA",Modifier.fillMaxWidth(),primary=true) {
                    ui.open("session",obj("date" to date))
                }
            }
            if(date==today()) {
                HButton("◈  REGISTRO DETALLADO",Modifier.fillMaxWidth(),
                    enabled=log?.b("pain")!=true) {
                    ui.open("record",obj("date" to date))
                }
            } else if(log!=null) {
                HButton("VER HISTORIAL DEL DÍA  ↗",Modifier.fillMaxWidth()) {
                    ui.open("history",obj("date" to date))
                }
            }
            if(canCustomize) {
                HButton("✎  PERSONALIZAR MISIÓN",Modifier.fillMaxWidth()) {
                    ui.open("custom",obj("date" to date))
                }
            }
            if(date==today()&&log!=null&&!readOnly) {
                HButton("!  DETENER POR MOLESTIAS",Modifier.fillMaxWidth()) {
                    ui.open("pain",obj("date" to date))
                }
            }
        }
    }
}

@Composable
private fun MonarchMissionExercise(
    ui:NativeUi,date:String,index:Int,item:JSONObject,
    readOnly:Boolean,tint:Color
) {
    val s=ui.store
    val colors=LocalHunter.current
    val e=s.catalog.byId[item.s("id")]?:return
    val doneSets=item.a("actual").objects().count{it.b("done")}
    val prescribed=item.i("sets").coerceAtLeast(0)
    val reported=item.b("missionState")||item.b("selfReported")
    val checked=reported||(prescribed>0&&doneSets>=prescribed)
    val inactive=item.b("archived")
    val shape=RoundedCornerShape(17.dp)
    Column(Modifier.fillMaxWidth().clip(shape)
        .background(colors.input)
        .border(1.dp,colors.line,shape)
        .padding(13.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(45.dp)
                .clip(RoundedCornerShape(3.dp)).background(tint))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                HText(e.name,size=17,bold=true,maxLines=2)
                HText("${item.i("sets")} series × ${item.i("target")} " +
                    (if(item.s("unit",e.unit)=="sec")"segundos" else "repeticiones")+
                    (if(e.sides)" por lado" else ""),
                    size=13,color=colors.muted,maxLines=2)
            }
            HButton("VER  ↗") { ui.open("exercise",obj("id" to e.id)) }
        }
        if(inactive)HText("Ejercicio archivado: "+item.s("archiveReason"),
            size=13,color=colors.muted)
        if(doneSets>0&&!reported)HText("$doneSets / $prescribed series registradas",
            size=13,color=colors.accent)
        if(reported)HText("Confirmación manual. No se registraron series automáticamente.",
            size=13,color=colors.muted)
        MonarchMissionCheck(
            label=if(checked)"Actividad completada" else "Marcar ejercicio como realizado",
            done=checked,enabled=!readOnly&&!inactive,
            subtitle=if(readOnly)"Solo lectura" else if(inactive)"Actividad archivada" else ""
        ) {ui.attempt{s.toggleMission(date,index)}}
    }
}

@Composable
private fun MonarchMissionCheck(
    label:String,done:Boolean,enabled:Boolean,subtitle:String="",action:()->Unit
) {
    val c=LocalHunter.current
    val shape=RoundedCornerShape(8.dp)
    Row(Modifier.fillMaxWidth()
        .defaultMinSize(minHeight=48.dp)
        .clip(shape)
        .clickable(enabled=enabled,role=Role.Checkbox,onClick=action)
        .semantics {
            role=Role.Checkbox
            stateDescription=if(done)"Completado" else "Pendiente"
        }
        .padding(vertical=8.dp,horizontal=2.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(25.dp).clip(RoundedCornerShape(7.dp))
            .background(if(done)c.accent else c.surface)
            .border(1.dp,if(done)c.accent else c.line,RoundedCornerShape(7.dp)),
            contentAlignment=Alignment.Center) {
            if(done)HText("✓",size=18,color=c.bg,bold=true)
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            HText(label,size=15,color=if(enabled)c.text else c.muted,maxLines=3)
            if(subtitle.isNotBlank())HText(subtitle,size=12,color=c.muted)
        }
    }
}

@Composable
private fun MonarchMissionsProgress(ui:NativeUi) {
    val s=ui.store
    val c=LocalHunter.current
    val todayLocal=LocalDate.parse(today())
    val monday=todayLocal.minusDays((todayLocal.dayOfWeek.value-1).toLong())
    val eligible=(0L..6L).map{monday.plusDays(it).toString()}
        .filter{it<=today()&&s.day(it).s("kind")!="rest"}
    val completed=eligible.count{s.logs.o(it).b("complete")}
    val inProgress=eligible.count{
        val log=s.logs.optJSONObject(it)
        log!=null&&!log.b("complete")&&!log.b("pain")
    }
    val fraction=if(eligible.isEmpty())0f else completed.toFloat()/eligible.size
    HPanel(padding=17) {
        Eyebrow("03  ·  AVANCE SEMANAL")
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically) {
            Heading("Tu progreso",size=25)
            HText("$completed / ${eligible.size}",size=19,color=c.accent,bold=true)
        }
        HProgress(fraction)
        HText("$completed días completados · $inProgress en curso",
            size=15,color=c.muted)
        HButton("REVISIÓN DE MI SEMANA  ↗",Modifier.fillMaxWidth()) {
            ui.open("review")
        }
        HButton("VER ESTADÍSTICAS COMPLETAS",Modifier.fillMaxWidth()) {
            ui.go("stats")
        }
    }
}

@Composable
private fun MonarchMissionsActions(ui:NativeUi) {
    val colors=LocalHunter.current
    HPanel(padding=16) {
        Eyebrow("04  ·  PLANIFICACIÓN")
        Heading("Preparación y avisos",size=23)
        HText("Consulta al acompañante, configura recordatorios o modifica tus días desde Perfil.",
            size=15,color=colors.muted)
        HButton("✦  CONSULTAR ACOMPAÑANTE",Modifier.fillMaxWidth()) {
            ui.open("coach")
        }
        HButton("♢  CONFIGURAR RECORDATORIOS",Modifier.fillMaxWidth()) {
            ui.open("reminders")
        }
        HButton("AJUSTAR PLAN EN PERFIL",Modifier.fillMaxWidth()) {
            ui.go("profile")
        }
    }
}

/**
 * Calendario mensual: no crea datos y conserva la posibilidad de abrir una
 * misión de otro mes, solo lectura cuando no sea hoy.
 */
@Composable
fun MonarchMissionsCalendar(ui:NativeUi) {
    val s=ui.store
    val c=LocalHunter.current
    var month by remember { mutableStateOf(YearMonth.now()) }
    val locale=Locale("es")
    val offset=month.atDay(1).dayOfWeek.value-1
    val count=month.lengthOfMonth()
    HPanel(padding=14) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically) {
            HButton("‹"){month=month.minusMonths(1)}
            HText(month.format(DateTimeFormatter.ofPattern("MMMM yyyy",locale))
                .replaceFirstChar{it.titlecase(locale)},size=19,bold=true)
            HButton("›"){month=month.plusMonths(1)}
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("L","M","X","J","V","S","D").forEach {
                Box(Modifier.weight(1f),contentAlignment=Alignment.Center) {
                    HText(it,size=13,color=c.muted,bold=true)
                }
            }
        }
        for(row in 0..5) {
            if(row*7>=offset+count)break
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                for(col in 0..6) {
                    val number=row*7+col-offset+1
                    if(number !in 1..count) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val date=month.atDay(number).toString()
                        val day=s.day(date)
                        val style=missionDayStyle(s,date,day)
                        val done=s.logs.o(date).b("complete")
                        val current=date==today()
                        val shape=RoundedCornerShape(9.dp)
                        Column(Modifier.weight(1f)
                            .defaultMinSize(minHeight=55.dp)
                            .clip(shape)
                            .background(if(current)c.selected else c.input)
                            .border(1.dp,if(current)c.accent else c.line,shape)
                            .clickable(role=Role.Button) {
                                ui.open("mission",obj("date" to date))
                            }
                            .semantics {
                                contentDescription="$date, ${style.title}, ${if(done)"completado" else "sin completar"}"
                            }
                            .padding(vertical=7.dp,horizontal=2.dp),
                            horizontalAlignment=Alignment.CenterHorizontally,
                            verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            HText(if(done)"✓" else number.toString(),
                                size=16,bold=done||current)
                            Box(Modifier.width(16.dp).height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(style.tint))
                        }
                    }
                }
            }
        }
        HText("✓ completada · la marca de color indica el tipo de actividad; las fechas futuras son previsiones.",
            size=12,color=c.muted)
    }
}
