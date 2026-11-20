package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.huntersystem.app.planning.CategoryMapper
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.time.Duration
import java.time.LocalDate

private val bossGold=Color(0xFFFFD99B)
private val bossBlue=Color(0xFFA4DCFF)

@Composable
private fun monarchClock():Long {
    val now by produceState(System.currentTimeMillis()) {
        while(true){
            delay(30_000)
            value=System.currentTimeMillis()
        }
    }
    return now
}

@Composable
fun MonarchSpecialMissionsPanel(ui:NativeUi) {
    val store=ui.store
    val revision=store.revision.intValue
    val run=remember(revision) { MonarchBossChallenges(store).current() }
    val active=run?.status(monarchClock())==BossStatus.ACTIVE
    val boss=run?.offer?.boss ?: MonarchBoss.IGRIS
    MonarchBossBanner(boss,if(active) "Desafío activo" else "Desafío voluntario",compact=true)
    HButton(if(active) "Continuar desafío Monarca" else "Ver Misiones Monarca",
        Modifier.fillMaxWidth(),primary=true){ui.open("monarch-boss")}
    LegacyMonarchMissionPanel(ui)
}

@Composable
internal fun MonarchBossScreen(ui:NativeUi,windowState:JSONObject) {
    val store=ui.store
    val revision=store.revision.intValue
    val repository=remember(store,revision) { MonarchBossChallenges(store) }
    val run=repository.current()
    val now=monarchClock()
    val status=run?.status(now)
    var next by remember(run?.startedAt) { mutableStateOf(windowState.b("previewNext")) }
    var confirmAbandon by remember(run?.startedAt) { mutableStateOf(false) }
    val showRun=run!=null&&!next
    val date=LocalDate.now()
    var selectedBoss by remember(run?.startedAt) {
        mutableStateOf(MonarchBoss.entries.firstOrNull { it.name==windowState.s("selectedBoss") } ?: MonarchBoss.IGRIS)
    }
    val preview=remember(repository,date,showRun,selectedBoss) { if(showRun)null else repository.offer(date,selectedBoss) }
    val offer=if(showRun) run?.offer else preview
    val boss=offer?.boss ?: selectedBoss

    MonarchBossBanner(boss,if(showRun) when(status){
        BossStatus.ACTIVE->"Desafío activo"
        BossStatus.COMPLETED->"Desafío completado"
        BossStatus.ABANDONED->"Desafío abandonado"
        else->"Tiempo finalizado"
    } else if(preview==null) "Sin propuesta compatible" else "Desafío disponible",compact=showRun)

    if(showRun){
        val done=run.offer.goals.sumOf { it.count }
        val total=run.offer.goals.sumOf { it.target }
        HPanel(decor=false,padding=12){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Eyebrow("PROGRESO ACUMULADO")
                if(status==BossStatus.ACTIVE) HText("Día ${run.day(now)} de 5",size=14,bold=true)
            }
            HText("$done / $total repeticiones",size=26,bold=true)
            HProgress(done.toFloat()/total)
            if(status==BossStatus.ACTIVE) HText(remainingTime(run.deadline-now),size=14)
        }
    }else{
        HSelect("Elige un jefe",selectedBoss.name,MonarchBoss.entries.map {
            it.name to "${it.title} · ${it.difficulty} · ${it.rangeLabel} reps"
        }) {
            windowState.put("selectedBoss",it)
            selectedBoss=MonarchBoss.valueOf(it)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
            listOf("5 días" to "Desde que aceptas","5 ejercicios" to "Según tu perfil",boss.rangeLabel to "Reps por ejercicio").forEach { (title,description) ->
                HPanel(Modifier.weight(1f),decor=false,padding=7){
                    HText(title,size=17,bold=true)
                    HText(description,size=12,color=LocalHunter.current.muted)
                }
            }
        }
    }

    if(offer==null){
        HNotice("No hay cinco ejercicios distintos por repeticiones y con demostración compatibles con tu perfil.")
    }else{
        Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
            Heading("Objetivos del desafío",size=26)
            HText(if(showRun&&status==BossStatus.ACTIVE) "Toca un ejercicio para ver su demostración y registrar tu avance."
                else "Las metas son acumuladas durante los cinco días.",size=14,color=LocalHunter.current.muted)
            for(goal in offer.goals) {
                val exercise=store.catalog.byId[goal.id]
                HPanel(onClick={ui.open("monarch-boss-exercise",obj("id" to goal.id,"target" to goal.target,
                    "start" to (run?.startedAt?.takeIf { showRun }?:0L)))},decor=false,padding=9){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        MonarchExerciseBadge(remember(exercise) { monarchExerciseIcon(exercise) })
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
                            HText(exercise?.name?:goal.id,bold=true,size=16)
                            exercise?.let { e ->
                                val category=CategoryMapper.normalize(e.type)?.code?.replace('_',' ')
                                HText(category?.replaceFirstChar { it.uppercase() }?:e.group,
                                    size=12,color=LocalHunter.current.muted)
                            }
                            if(showRun)HProgress(goal.count.toFloat()/goal.target)
                        }
                        HText(if(showRun) "${goal.count} / ${goal.target} reps" else "${goal.target} reps",
                            size=14,bold=true)
                        HText(if(showRun&&status==BossStatus.ACTIVE) "+" else "›",size=19,color=LocalHunter.current.accent)
                    }
                }
            }
        }
    }

    if(showRun){
        if(status==BossStatus.ACTIVE){
            if(confirmAbandon){
                HNotice("Tu avance quedará guardado.")
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    HButton("Confirmar abandono",Modifier.weight(1f)){
                        ui.attempt { repository.abandon(run.startedAt);confirmAbandon=false }
                    }
                    HButton("Seguir en el desafío",Modifier.weight(1f)){confirmAbandon=false}
                }
            }else HButton("Abandonar desafío",Modifier.fillMaxWidth()){confirmAbandon=true}
        }else HButton("Ver nuevo desafío",Modifier.fillMaxWidth(),primary=true){
            windowState.put("previewNext",true)
            next=true
        }
    }else{
        HText("Metas acumuladas durante cinco días. El desafío es opcional e independiente de tus misiones y rutinas.",size=14,
            color=LocalHunter.current.muted)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            HButton("Aceptar desafío",Modifier.weight(1.5f),primary=true,enabled=preview!=null){
                preview?.let {
                    ui.attempt {
                        repository.start(it,date)
                        windowState.remove("previewNext")
                        windowState.remove("selectedBoss")
                        next=false
                    }
                }
            }
            HButton("Ahora no",Modifier.weight(1f)){ui.back()}
        }
    }
}

@Composable
private fun MonarchBossBanner(boss:MonarchBoss,status:String,compact:Boolean) {
    val frame=RoundedCornerShape(15.dp)
    Box(Modifier.fillMaxWidth().height(if(compact) 145.dp else 205.dp).clip(frame).monarchOutline(frame)){
        val isIgris=boss==MonarchBoss.IGRIS
        key(boss) {
            AssetImage(boss.artwork,Modifier.matchParentSize().graphicsLayer {
                if(isIgris)scaleX=-1f
            },ContentScale.Crop,description="Ilustración de ${boss.title}",alignment=Alignment.TopCenter)
        }
        val scrim=if(isIgris) Brush.horizontalGradient(
            listOf(Color(0xF0081324),Color(0xB0081324),Color.Transparent))
        else Brush.horizontalGradient(
            0f to Color(0xDC081324),.35f to Color(0xA8081324),.68f to Color.Transparent)
        Box(Modifier.matchParentSize().background(scrim))
        val bottomScrim=if(isIgris) Brush.verticalGradient(listOf(Color.Transparent,Color(0x98091221)))
        else Brush.verticalGradient(.56f to Color.Transparent,1f to Color(0x69091221))
        Box(Modifier.matchParentSize().background(bottomScrim))
        Column(Modifier.align(Alignment.BottomStart).padding(15.dp)){
            HText("JEFE MONARCA",size=13,bold=true,color=bossBlue)
            val titleSize=if(boss==MonarchBoss.TUSK) 30 else if(compact) 36 else 48
            HText(boss.title,size=titleSize,bold=true,color=bossGold)
            HText(boss.encounter,size=17,color=Color(0xFFF3F6FD))
            HText(status,size=14,bold=true,color=bossBlue)
        }
    }
}

@Composable
internal fun MonarchBossExercise(ui:NativeUi,data:JSONObject) {
    val store=ui.store
    val exercise=store.catalog.byId[data.s("id")]
    if(exercise==null){HNotice("La ficha de este ejercicio no está disponible.");return}
    val revision=store.revision.intValue
    val repository=remember(store,revision) { MonarchBossChallenges(store) }
    val start=data.optLong("start")
    val now=monarchClock()
    val run=repository.current()?.takeIf { start>0&&it.startedAt==start }
    val goal=run?.offer?.goals?.firstOrNull { it.id==exercise.id }
    val remaining=(goal?.target?:0)-(goal?.count?:0)
    val compatible=remember(repository,exercise.id,start) { start==0L||repository.compatible(exercise.id) }
    val canRecord=run?.status(now)==BossStatus.ACTIVE&&remaining>0&&compatible
    var amount by remember(start,exercise.id,goal?.count) { mutableStateOf("") }
    var play by remember(exercise.id) { mutableStateOf(false) }
    var mirror by remember(exercise.id) { mutableStateOf(false) }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle){
        val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_STOP) play=false }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val reps=amount.toIntOrNull()?:0

    Eyebrow("MISIÓN MONARCA · ${run?.offer?.boss?.title ?: "VISTA PREVIA"}")
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        MonarchExerciseBadge(remember(exercise) { monarchExerciseIcon(exercise) })
        Heading(exercise.name,Modifier.weight(1f),size=34)
    }
    MonarchExerciseStage(exercise,mirror,height=265,play=play)
    Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
        HButton(if(play) "Pausar demostración" else "Reproducir demostración"){play=!play}
        HButton(if(mirror) "Espejo ✓" else "Espejo"){mirror=!mirror}
    }
    HPanel(decor=false,padding=13){
        Heading("Registrar repeticiones",size=25)
        HText(if(goal==null) "Meta del desafío: ${data.i("target")} repeticiones"
            else "${goal.count} / ${goal.target} repeticiones acumuladas",size=17,bold=true)
        if(exercise.sides)HText("Registra el total realizado entre ambos lados.",size=13)
        when {
            canRecord && goal!=null -> {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    HButton("−",enabled=reps>0){amount=(reps-1).toString()}
                    HInput(amount,{amount=it.filter(Char::isDigit).take(3)},"Repeticiones realizadas",
                        Modifier.weight(1f),number=true)
                    HButton("+",enabled=reps<remaining){amount=(reps+1).toString()}
                }
                if(reps<=remaining)HText("Total al guardar: ${goal.count+reps} / ${goal.target} reps",size=14)
                else HNotice("Quedan $remaining repeticiones para esta meta.")
                HButton("Guardar repeticiones",Modifier.fillMaxWidth(),primary=true,enabled=reps in 1..remaining){
                    ui.attempt { repository.record(start,exercise.id,reps,goal.count) }
                }
            }
            start==0L -> HText("El registro se habilita al aceptar el desafío.",size=14)
            goal!=null&&remaining==0 -> HText("Objetivo completado.",size=14)
            run?.status(now)!=BossStatus.ACTIVE -> HText("El desafío ya no está activo.",size=14)
            else -> HNotice("Revisa el equipo y las restricciones de tu perfil para registrar este ejercicio.")
        }
    }
}

private fun remainingTime(millis:Long):String {
    val time=Duration.ofMillis(millis.coerceAtLeast(0))
    val days=time.toDays()
    val hours=time.toHours()%24
    return when {
        days>0->"Tiempo restante: $days d y $hours h"
        hours>0->"Tiempo restante: $hours h y ${time.toMinutes()%60} min"
        else->"Tiempo restante: ${time.toMinutes()} min"
    }
}
