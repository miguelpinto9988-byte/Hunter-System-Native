package com.huntersystem.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Fase 09: panel estadístico derivado exclusivamente de registros reales.
 * No crea estadísticas persistidas, XP extra ni logros artificiales.
 */
data class MonarchSeriesBucket(val title:String,val sets:Int,val sessions:Int)
data class MonarchAnalyticsSnapshot(
    val periodDays:Int, val completed:Int, val trainingDays:Int, val plannedCompleted:Int,
    val loggedSets:Int,val loggedExercises:Int,val quickCompletions:Int,
    val strength:Int,val cardio:Int,val mobility:Int,
    val planned:Int,val buckets:List<MonarchSeriesBucket>
)

object MonarchAnalytics {
    fun calculate(s:NativeStore, period:Int):MonarchAnalyticsSnapshot {
        val days=period.coerceIn(7,90)
        val end=LocalDate.parse(today())
        val start=end.minusDays(days.toLong()-1)
        val recorded=s.logs.entries().mapNotNull{(key,value)->
            val date=runCatching{LocalDate.parse(key)}.getOrNull()
            val log=value as? JSONObject
            if(date==null||log==null||date.isBefore(start)||date.isAfter(end))null
            else date to log
        }.sortedBy{it.first}
        val complete=recorded.filter{it.second.b("complete")}
        val confirmed=recorded.filter{(_,l)->l.b("complete")&&
            l.a("items").objects().none{x->x.a("actual").objects().any{it.b("done")}}}
        // "Series" cuenta únicamente valores done registrados en actual, no confirmaciones rápidas.
        fun realSets(log:JSONObject)=log.a("items").objects().sumOf{x->
            x.a("actual").objects().count{it.b("done")}
        }
        val setTotal=recorded.sumOf{realSets(it.second)}
        val exercises=recorded.sumOf{(_,l)->l.a("items").objects().count{x->
            x.a("actual").objects().any{it.b("done")}
        }}
        val dates=(0 until days).map{start.plusDays(it.toLong())}
        val byDay=recorded.associateBy{it.first}
        val chart=if(days==7)dates.map{date->
            val log=byDay[date]?.second
            val dayName=date.dayOfWeek.getDisplayName(TextStyle.SHORT,Locale("es","PE"))
                .replaceFirstChar{it.uppercase()}
            MonarchSeriesBucket(dayName,if(log==null)0 else realSets(log),
                if(log?.b("complete")==true)1 else 0)
        }else (0..6).map{bin->
            val slice=dates.filterIndexed{i,_->i*7/days==bin}
            MonarchSeriesBucket("B${bin+1}",
                slice.sumOf{date->byDay[date]?.second?.let(::realSets)?:0},
                slice.count{byDay[it]?.second?.b("complete")==true})
        }
        // Un día del plan solo cuenta si existe un ciclo histórico guardado con ese día.
        val plannedDates=s.allDays().mapNotNull{d->
            val date=runCatching{LocalDate.parse(d.s("date"))}.getOrNull()
            if(date!=null&&!date.isBefore(start)&&!date.isAfter(end)&&d.s("kind")!="rest")
                date else null
        }.toSet()
        val planned=plannedDates.size
        val plannedCompleted=complete.count{it.first in plannedDates}
        fun kindCount(vararg types:String)=complete.count{it.second.s("kind") in types}
        return MonarchAnalyticsSnapshot(
            periodDays=days,
            completed=complete.size,plannedCompleted=plannedCompleted,
            trainingDays=recorded.count{(_,l)->l.a("items").objects().any{x->
                x.a("actual").objects().any{a->a.b("done")}}},
            loggedSets=setTotal,loggedExercises=exercises,quickCompletions=confirmed.size,
            strength=kindCount("strength","mixed"),cardio=kindCount("cardio"),
            mobility=kindCount("recovery","mobility"),planned=planned,buckets=chart
        )
    }
}

@Composable
private fun MonarchSummaryTile(title:String,value:String,subtitle:String,modifier:Modifier=Modifier){
    val c=LocalHunter.current
    HPanel(modifier,padding=14,decor=false){
        Eyebrow(title.uppercase())
        Heading(value,size=31)
        HText(subtitle,size=12,color=c.muted)
    }
}

@Composable
private fun MonarchProgressTrend(data:MonarchAnalyticsSnapshot,showSets:Boolean){
    val c=LocalHunter.current
    val values=data.buckets.map{if(showSets)it.sets else it.sessions}
    val maxValue=max(1,values.maxOrNull()?:1)
    val total=values.sum()
    HPanel(padding=16){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically){
            Heading(if(showSets)"Series registradas" else "Sesiones completadas",size=22)
            HText(total.toString(),size=23,bold=true,color=c.accent)
        }
        if(data.loggedSets==0&&data.completed==0){
            HText("Todavía no hay datos en este período. El gráfico se llenará con tus registros.",
                size=14,color=c.muted)
        }
        Row(Modifier.fillMaxWidth().height(138.dp),
            verticalAlignment=Alignment.Bottom,
            horizontalArrangement=Arrangement.spacedBy(7.dp)){
            for((i,bucket)in data.buckets.withIndex()){
                val value=values[i]
                val target=(value.toFloat()/maxValue).coerceIn(0f,1f)
                val animated by animateFloatAsState(target,tween(600),label="progress-trend-$i")
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.Bottom){
                    HText(value.toString(),size=11,color=if(value>0)c.accent else c.muted)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(94.dp),contentAlignment=Alignment.BottomCenter){
                        Box(Modifier.fillMaxWidth(.72f)
                            .height((4f+animated*86f).dp)
                            .clip(RoundedCornerShape(topStart=7.dp,topEnd=7.dp))
                            .background(if(value>0)Brush.verticalGradient(
                                listOf(c.accent,c.secondary.copy(alpha=.55f)))
                                else Brush.verticalGradient(listOf(c.input,c.input)))
                            .semantics { contentDescription="${bucket.title}: $value ${if(showSets)"series" else "sesiones"}" })
                    }
                    Spacer(Modifier.height(6.dp))
                    HText(bucket.title,size=11,color=c.muted,maxLines=1)
                }
            }
        }
        HText(if(data.periodDays==7)"Cada barra representa un día."
            else "Cada barra agrupa un tramo consecutivo del período (B1–B7).",
            size=12,color=c.muted)
    }
}

@Composable
private fun MonarchKindDistribution(data:MonarchAnalyticsSnapshot){
    val c=LocalHunter.current
    val groups=listOf(
        Triple("Fuerza y mixto",data.strength,c.accent),
        Triple("Cardio",data.cardio,c.secondary),
        Triple("Movilidad y recuperación",data.mobility,c.muted)
    )
    val total=groups.sumOf{it.second}
    HPanel(padding=18){
        Heading("Sesiones por modalidad",size=22)
        if(total==0)HText("No hay sesiones completadas clasificadas en este período.",
            size=14,color=c.muted)
        for((label,n,color)in groups){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                HText(label,size=14,color=c.text)
                HText(n.toString(),size=14,bold=true,color=color)
            }
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(c.input)){
                val fraction=if(total==0)0f else n.toFloat()/total
                val animated by animateFloatAsState(fraction,tween(450),label="modality-$label")
                Box(Modifier.fillMaxWidth(animated.coerceIn(0f,1f)).fillMaxHeight()
                    .clip(CircleShape).background(color))
            }
        }
    }
}

data class MonarchMilestone(val name:String,val detail:String,val achieved:Boolean)

private fun monarchMilestones(stats:JSONObject, s:NativeStore):List<MonarchMilestone>{
    val completed=stats.i("days")
    val streak=stats.i("streak")
    val strength=s.logs.entries().count{(date,value)->
        date<=today()&&(value as? JSONObject)?.let{
            it.b("complete")&&it.s("kind") in setOf("strength","mixed")
        }==true
    }
    return listOf(
        MonarchMilestone("PRIMER DESPERTAR","Completa una sesión",completed>=1),
        MonarchMilestone("TRES DÍAS","Racha de 3 días",streak>=3),
        MonarchMilestone("DIEZ MISIONES","Completa 10 sesiones",completed>=10),
        MonarchMilestone("CIEN SERIES","Registra 100 series",stats.i("series")>=100),
        MonarchMilestone("FUERZA CONSTANTE","Completa 5 sesiones de fuerza",strength>=5),
        MonarchMilestone("CIEN EJERCICIOS","Registra 100 ejercicios",stats.i("exerciseCount")>=100)
    )
}

@Composable
private fun MonarchAchievementGrid(ui:NativeUi,stats:JSONObject){
    val c=LocalHunter.current
    val list=monarchMilestones(stats,ui.store)
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically){
            Heading("Hitos de evolución",size=25)
            HText("${list.count{it.achieved}} / ${list.size}",size=16,
                color=c.accent,bold=true)
        }
        for(row in list.chunked(2))Row(Modifier.fillMaxWidth(),
            horizontalArrangement=Arrangement.spacedBy(10.dp)){
            for(badge in row) HPanel(Modifier.weight(1f),padding=12,decor=false){
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(if(badge.achieved)c.accent.copy(alpha=.18f) else c.input)
                    .border(1.dp,if(badge.achieved)c.accent else c.line,
                        RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){
                    HText(if(badge.achieved)"✦" else "◇",size=23,
                        color=if(badge.achieved)c.accent else c.muted)
                }
                HText(badge.name,size=13,bold=true)
                HText(badge.detail,size=12,color=c.muted)
                HText(if(badge.achieved)"CONSEGUIDO" else "POR ALCANZAR",size=10,
                    color=if(badge.achieved)c.accent else c.muted)
            }
        }
        HText("Los hitos son reconocimientos visuales calculados con tus registros. No conceden XP adicional.",
            size=13,color=c.muted)
    }
}

@Composable
fun MonarchProgressScreen(ui:NativeUi){
    val s=ui.store
    s.revision.intValue
    var days by remember { mutableStateOf(7) }
    var graph by remember { mutableStateOf("sets") }
    val data=remember(days,s.revision.intValue){MonarchAnalytics.calculate(s,days)}
    val all=s.stats()
    val c=LocalHunter.current
    SceneColumn(ui){
        Eyebrow("CENTRO DE EVOLUCIÓN · MONARCA")
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically){
            Column {
                Heading("Tu evolución",size=34)
                HText("Datos reales de tus sesiones",size=14,color=c.muted)
            }
            HChip("RANGO ${rankKey(all.i("level",1))}",true){ui.open("evolution")}
        }
        HPanel(onClick={ui.open("evolution")},padding=16){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Eyebrow("EXPERIENCIA ACUMULADA")
                    Heading("NIVEL ${all.i("level",1)}",size=31)
                    HText("${all.i("xp")} XP · Rango ${rankKey(all.i("level",1))}",size=16,color=c.accent)
                }
                Box(Modifier.size(58.dp).clip(CircleShape)
                    .background(c.accent.copy(alpha=.13f))
                    .border(2.dp,c.accent,CircleShape),
                    contentAlignment=Alignment.Center){
                    HText(rankKey(all.i("level",1)),size=28,color=c.accent,bold=true)
                }
            }
            HProgress((all.i("xp")%300)/300f)
            HText("${all.i("xp")%300} / 300 XP al siguiente nivel · Ver rangos ↗",
                size=13,color=c.muted)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            for((num,label)in listOf(7 to "7 DÍAS",30 to "30 DÍAS",90 to "90 DÍAS")){
                HChip(label,days==num,Modifier.weight(1f)){days=num}
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){
            MonarchSummaryTile("Sesiones",data.completed.toString(),"Completadas",
                Modifier.weight(1f))
            MonarchSummaryTile("Series",data.loggedSets.toString(),"Registradas",
                Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){
            MonarchSummaryTile("Ejercicios",data.loggedExercises.toString(),
                "Con series realizadas",Modifier.weight(1f))
            MonarchSummaryTile("Racha actual",all.i("streak").toString(),
                "Días consecutivos",Modifier.weight(1f))
        }
        HPanel(padding=15,decor=false){
            Eyebrow("PLANIFICACIÓN DEL PERÍODO")
            if(data.planned>0){
                val count=data.plannedCompleted
                val fraction=count.toFloat()/data.planned
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Heading("${(fraction*100).roundToInt()} %",size=27)
                    HText("$count / ${data.planned} días",size=16,color=c.accent,bold=true)
                }
                HProgress(fraction)
                HText("Días programados completados / días programados guardados en el ciclo. "+
                    "No incluye días futuros.",size=12,color=c.muted)
            }else HText("Sin días de entrenamiento planificados guardados para este período.",
                size=14,color=c.muted)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){
            HChip("SERIES",graph=="sets",Modifier.weight(1f)){graph="sets"}
            HChip("SESIONES",graph=="sessions",Modifier.weight(1f)){graph="sessions"}
        }
        MonarchProgressTrend(data,graph=="sets")
        MonarchKindDistribution(data)
        if(data.quickCompletions>0){
            HNotice("${data.quickCompletions} sesiones se confirmaron sin registrar series "+
                "en este período. Cuentan como completadas, pero no inventamos repeticiones ni series.")
        }
        MonarchAchievementGrid(ui,all)
        HPanel(padding=16){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically){
                Heading("Diario de misiones",size=23)
                HText("${all.i("xp")} XP",size=16,color=c.accent,bold=true)
            }
            val records=s.logs.entries().mapNotNull{(key,value)->
                val date=runCatching{LocalDate.parse(key)}.getOrNull()
                val l=value as? JSONObject
                if(date==null||date.isAfter(LocalDate.parse(today()))||l==null)null
                else key to l
            }.sortedByDescending{it.first}
            if(records.isEmpty())HText("Tu historia comienza con tu primera misión registrada.",
                size=14,color=c.muted)
            for((date,log)in records.take(60)){
                Row(Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role=Role.Button){ui.open("history",obj("date" to date))}
                    .padding(vertical=7.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.SpaceBetween){
                    Column(Modifier.weight(1f)){
                        HText(date,size=13,color=c.muted)
                        HText(kindLabel(log.s("kind")),size=17,bold=true)
                    }
                    HText(if(log.b("complete"))"✓ COMPLETADA" else
                        if(log.b("pain"))"DETENIDA" else "EN CURSO",
                        size=12,color=if(log.b("complete"))c.accent else c.muted,bold=true)
                }
            }
        }
        HText("El tiempo registrado suma ejercicios medidos en segundos, no la duración de "+
            "una sesión. Los datos no enviados a los registros no se estiman.",
            size=13,color=c.muted)
    }
}

@Composable
fun MonarchEvolutionScreen(ui:NativeUi){
    val s=ui.store
    s.revision.intValue
    val stats=s.stats()
    val level=stats.i("level",1)
    val xp=stats.i("xp")
    val rank=rankKey(level)
    val c=LocalHunter.current
    Eyebrow("ARCHIVO DE RANGOS MONARCA")
    Heading("El despertar del cazador",size=32)
    HPanel(padding=18){
        Eyebrow("ESTADO ACTUAL")
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically){
            Heading("RANGO $rank",size=32)
            Heading("NV. $level",size=24)
        }
        HProgress((xp%300)/300f)
        HText("${xp%300} / 300 XP para subir de nivel · ${xp} XP total",
            size=14,color=c.muted)
        HText("El rango cambia cada cinco niveles. Solo se consideran misiones "+
            "registradas en el dispositivo.",size=13,color=c.muted)
    }
    val ranks=s.catalog.design.a("ranks").objects()
    val char=if(s.profile?.s("gender")=="female")"hunter-female" else "hunter-male"
    for((index,entry)in ranks.withIndex()){
        val key=entry.s("key",listOf("E","D","C","B","A","S").getOrElse(index){"S"})
        val accessible=listOf("E","D","C","B","A","S").indexOf(key) <=
            listOf("E","D","C","B","A","S").indexOf(rank)
        HPanel(padding=16,decor=accessible){
            Row(verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(14.dp)){
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(15.dp))
                    .background(if(accessible)c.accent.copy(alpha=.15f) else c.input)
                    .border(1.dp,if(accessible)c.accent else c.line,
                        RoundedCornerShape(15.dp)),contentAlignment=Alignment.Center){
                    HText(key,size=28,bold=true,
                        color=if(accessible)c.accent else c.muted)
                }
                Column(Modifier.weight(1f)){
                    Eyebrow(if(key==rank)"RANGO ACTUAL" else
                        if(accessible)"ALCANZADO" else "POR DESBLOQUEAR")
                    Heading(entry.s("name"),size=23)
                    HText("Desde el nivel ${entry.i("min",1)} · ${entry.i("xp")} XP",
                        size=14,color=c.muted)
                }
            }
            if(key==rank){
                AssetImage("ui33/$char-${key.lowercase()}.webp",
                    Modifier.fillMaxWidth().height(180.dp))
            }
            if(entry.s("detail").isNotBlank())HText(entry.s("detail"),
                size=14,color=c.muted)
        }
    }
    HNotice("Los rangos y emblemas representan progreso de entrenamiento registrado, "+
        "no una evaluación clínica ni un reconocimiento externo.")
}
