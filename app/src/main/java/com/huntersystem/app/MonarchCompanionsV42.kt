package com.huntersystem.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/**
 * Fase 08: personalización local del acompañante.
 * Los personajes se eligen únicamente desde Perfil; la pantalla de inicio
 * muestra esa misma selección sin duplicar controles.
 */
data class MonarchCompanionSpec(
    val id: String, val asset: String, val title: String,
    val subtitle: String, val motto: String
)

object MonarchCompanions {
    val all = listOf(
        MonarchCompanionSpec("knight","igris","IGRIS","Caballero de la disciplina","Un paso firme. Una misión a la vez."),
        MonarchCompanionSpec("warrior","iron","IRON","Guardián de la fuerza","La constancia también es poder."),
        MonarchCompanionSpec("bear","tank","TANK","Protector del equilibrio","La fuerza crece con paciencia."),
        MonarchCompanionSpec("insect","beru","BERU","Centinela del progreso","Cada serie cuenta."),
        MonarchCompanionSpec("scientist","dokja","DOKJA KIM","Estratega de la evolución","Avanza con intención.")
    )
    fun forId(id: String?): MonarchCompanionSpec =
        all.firstOrNull { it.id == id } ?: all.first()

    fun motion(id: String?): String =
        id?.takeIf { it in setOf("off","calm","normal","fast") } ?: "normal"
    fun size(id: String?): String =
        id?.takeIf { it in setOf("compact","regular","large") } ?: "regular"
    fun tone(id: String?): String =
        id?.takeIf { it in setOf("balanced","strategic","warm") } ?: "balanced"
}

@Composable
fun MonarchCompanionPicker(selected:String, onSelected:(String)->Unit) {
    val c = LocalHunter.current
    val chosen = MonarchCompanions.forId(selected)
    Column(verticalArrangement=Arrangement.spacedBy(13.dp)) {
        Eyebrow("PERSONALIZACIÓN DEL ACOMPAÑANTE")
        Heading("Elige quién te acompaña",size=26)
        HText("Tu selección se muestra en el inicio y durante tus misiones. Solo se cambia desde Perfil.",
            size=14,color=c.muted)
        val hero = RoundedCornerShape(21.dp)
        Row(
            Modifier.fillMaxWidth().clip(hero)
                .background(Brush.horizontalGradient(listOf(c.panel,c.surface)))
                .border(1.dp,c.accent.copy(alpha=.45f),hero)
                .padding(13.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(14.dp)
        ) {
            AssetImage("ui33/companion-${chosen.asset}.webp",
                Modifier.width(118.dp).height(151.dp)
                    .semantics { contentDescription = "Vista previa de ${chosen.title}" })
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)){
                Eyebrow("ACOMPAÑANTE SELECCIONADO")
                Heading(chosen.title,size=25)
                HText(chosen.subtitle,size=14,color=c.muted)
                HorizontalDivider(color=c.line)
                HText(chosen.motto,size=14,color=c.accent)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(9.dp)){
            for(item in MonarchCompanions.all){
                val active=item.id==selected
                val shape=RoundedCornerShape(16.dp)
                Column(
                    Modifier.width(108.dp).clip(shape)
                        .background(if(active)c.selected else c.input)
                        .border(if(active)2.dp else 1.dp,
                            if(active)c.accent else c.line,shape)
                        .selectable(selected=active,role=Role.RadioButton,
                            onClick={onSelected(item.id)})
                        .padding(8.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(5.dp)){
                    AssetImage("ui33/companion-${item.asset}.webp",
                        Modifier.fillMaxWidth().height(104.dp))
                    HText(item.title,size=12,bold=active,maxLines=1,
                        color=if(active)c.accent else c.text)
                    HText(if(active)"✓ SELECCIONADO" else "ELEGIR",size=10,
                        color=if(active)c.accent else c.muted,maxLines=1)
                }
            }
        }
    }
}

@Composable
fun MonarchCompanionOptions(
    values:Map<String,String>, onChange:(String,String)->Unit
){
    val c=LocalHunter.current
    Column(verticalArrangement=Arrangement.spacedBy(13.dp)) {
        Heading("Presencia y movimiento",size=23)
        HText("Personaliza su presencia sin modificar tu entrenamiento.",
            color=c.muted,size=14)
        HCheck("Mostrar acompañante flotante",
            values["companionVisible"]!="false"){
            onChange("companionVisible",
                (values["companionVisible"]=="false").toString())
        }
        Eyebrow("MOVIMIENTO")
        Chips(
            listOf("off" to "Quieto","calm" to "Lento",
                "normal" to "Normal","fast" to "Rápido"),
            MonarchCompanions.motion(values["companionMotion"])
        ){onChange("companionMotion",it)}
        Eyebrow("TAMAÑO EN PANTALLA")
        Chips(
            listOf("compact" to "Compacto","regular" to "Normal",
                "large" to "Grande"),
            MonarchCompanions.size(values["companionSize"])
        ){onChange("companionSize",it)}
        Eyebrow("ESTILO DE MENSAJE")
        Chips(
            listOf("balanced" to "Equilibrado","strategic" to "Estratégico",
                "warm" to "Cercano"),
            MonarchCompanions.tone(values["companionTone"])
        ){onChange("companionTone",it)}
        HNotice("Los mensajes del acompañante son orientaciones locales; no son una IA remota ni sustituyen recomendaciones profesionales.")
    }
}

/** Globo visual de estado. La respuesta a preguntas sigue siendo local y basada en reglas. */
@Composable
fun MonarchCompanionMood(p:JSONObject?,modifier:Modifier=Modifier) {
    val c=LocalHunter.current
    val spec=MonarchCompanions.forId(p?.s("companionCharacter"))
    val phrase=when(MonarchCompanions.tone(p?.s("companionTone"))){
        "strategic" -> "Planifica, ejecuta y registra cada serie."
        "warm" -> "Vas a tu ritmo; tus avances cuentan."
        else -> spec.motto
    }
    Row(modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(12.dp)){
        AssetImage("ui33/companion-${spec.asset}.webp",
            Modifier.width(66.dp).height(82.dp))
        Column(Modifier.weight(1f)){
            Eyebrow("ACOMPAÑANTE ACTIVO")
            HText(spec.title,size=19,bold=true,color=c.accent)
            HText(phrase,size=13,color=c.muted)
        }
    }
}

/** Movimiento suave y opcional; sin temporizador, WebView ni red. */
@Composable
fun MonarchFloatingCompanion(ui:NativeUi, modifier:Modifier=Modifier){
    val p=ui.store.profile
    if(p?.b("companionVisible",true)==false) return
    val character=MonarchCompanions.forId(p?.s("companionCharacter"))
    val size=MonarchCompanions.size(p?.s("companionSize"))
    val width=when(size){"compact"->51;"large"->82;else->65}
    val height=when(size){"compact"->69;"large"->108;else->86}
    val motion=if(p?.b("animate",true)==false)"off"
        else MonarchCompanions.motion(p?.s("companionMotion"))
    val cycleMs=when(motion){"calm"->3100;"fast"->1200;else->2200}
    val amplitude=if(motion=="off")0f else if(motion=="fast")7f else 5f
    val dy=if(LocalMonarchMotion.current.reduced || amplitude==0f) 0f else {
        val transition=rememberInfiniteTransition(label="monarch-companion")
        val floatOffset by transition.animateFloat(
            0f,-amplitude,infiniteRepeatable(
                tween(cycleMs,easing=FastOutSlowInEasing),RepeatMode.Reverse),
            label="float-${character.asset}"
        )
        floatOffset
    }
    AssetImage("ui33/companion-${character.asset}.webp",
        modifier.width(width.dp).height(height.dp)
            .graphicsLayer { translationY=dy }
            .clip(RoundedCornerShape(12.dp))
            .clickable { ui.open("coach") }
            .semantics { contentDescription="Abrir guía de ${character.title}" }
    )
}
