package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/**
 * Hunter Design System V42 fase 02.
 * Tokens semánticos persistentes; ninguna dependencia de red.
 * La selección del tema afecta solo a la apariencia.
 */
data class HunterThemeSpec(
    val id: String,
    val label: String,
    val bg: String,
    val panel: String,
    val surface: String,
    val border: String,
    val text: String,
    val muted: String,
    val accent: String,
    val secondary: String,
    val selected: String
)

object HunterDesignSystem {
    val themes = listOf(
        HunterThemeSpec("eclipse","Eclipse","#080D19","#111C31","#18263F","#36506C","#E8F1FF","#B4C6DB","#25C8FF","#7F5CFF","#183352"),
        HunterThemeSpec("umbral","Umbral","#080B15","#151C35","#21274A","#46527E","#F3F2FF","#B7BAD8","#9B7CFF","#54C9FF","#302950"),
        HunterThemeSpec("abismo","Abismo","#05121B","#0C2230","#133343","#355668","#EBFAFF","#A9C9D3","#47DAEE","#4FA1FA","#15434B"),
        HunterThemeSpec("runa","Runa","#0A1818","#102A2A","#1A3838","#426B62","#E7FFF5","#B2D3C8","#78E6B2","#41BFC7","#215247"),
        HunterThemeSpec("hielo","Hielo","#0A1623","#17293C","#1D3B52","#4A718A","#F2FBFF","#C2D6E9","#82DBFA","#B7EFFF","#214662"),
        HunterThemeSpec("lavanda","Lavanda","#181225","#251A38","#39274F","#6F5985","#F8EEFF","#D6C1E9","#CEA4FF","#F2A8D5","#503767"),
        HunterThemeSpec("aurora","Aurora","#140919","#27112F","#3E1B4D","#795386","#FFF1FF","#C7A9D3","#F63AB8","#FB87DF","#58235E"),
        HunterThemeSpec("ceniza","Ceniza","#0D1017","#1A202B","#303A49","#617085","#EDF1FA","#B9C7DE","#C9D2E6","#8298B3","#33445C"),
        HunterThemeSpec("amanecer","Amanecer","#1F1510","#35261C","#4C3525","#8B6B45","#FFF2DE","#D8BB99","#F6B84F","#FFD988","#654629")
    )
    val ids = themes.map { it.id }.toSet()
    fun theme(id: String?): HunterThemeSpec = themes.firstOrNull { it.id == id } ?: themes.first()

    fun tokens(id: String, source: JSONObject): JSONObject {
        val t = theme(id)
        return JSONObject(source.toString()).apply {
            put("--bg",t.bg);put("--panel",t.panel);put("--surface",t.surface)
            put("--input",t.surface);put("--line",t.border)
            put("--text",t.text);put("--muted",t.muted)
            put("--accent",t.accent);put("--secondary33",t.secondary)
            put("--selected33",t.selected)
        }
    }

    object Dimensions {
        val cornerCard = 22.dp
        val cornerButton = 14.dp
        val cornerField = 13.dp
        val cornerDialog = 26.dp
        val spacing = 12.dp
        val touchTarget = 48.dp
        const val animationMs = 220
    }
}

/** Vista previa antes de guardar. No altera perfil ni regenera misiones al tocar muestras. */
@Composable
fun MonarchThemePicker(current: String, onChange: (String) -> Unit) {
    val c = LocalHunter.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Eyebrow("PERSONALIZACIÓN VISUAL")
        Heading("Escoge tu atmósfera", size=26)
        HText("El tema cambia los colores, no tu progreso ni los ejercicios.",
            size=15, color=c.muted)
        for (row in HunterDesignSystem.themes.chunked(3)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                for (t in row) {
                    val active = current == t.id
                    val accent = parseColor(t.accent)
                    val shape = RoundedCornerShape(14.dp)
                    Column(
                        Modifier.weight(1f)
                            .clip(shape)
                            .background(c.input)
                            .border(
                                1.dp,
                                if (active) c.accent else c.line,
                                shape
                            )
                            .selectable(
                                selected=active,
                                role=Role.RadioButton,
                                onClick={ onChange(t.id) }
                            )
                            .padding(8.dp),
                        horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            Modifier.fillMaxWidth().height(42.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(parseColor(t.bg), accent, parseColor(t.secondary))
                                    )
                                ),
                            contentAlignment=Alignment.Center
                        ) {
                            if (active) HText("✓", size=20, color=Color.White, bold=true)
                        }
                        HText(t.label,size=13,bold=active,maxLines=1,
                            color=if(active)c.accent else c.text)
                    }
                }
            }
        }
        val preview=HunterDesignSystem.theme(current)
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape)
                .background(parseColor(preview.accent))
                .border(1.dp,c.line,CircleShape))
            Column(Modifier.weight(1f)) {
                HText("Vista previa: " + preview.label,size=16,bold=true)
                HText("Pulsa «Guardar perfil» para aplicar el tema.",
                    size=13,color=c.muted)
            }
        }
    }
}

@Composable
fun MonarchSectionHeading(kicker: String, title: String) {
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Eyebrow(kicker)
        Heading(title,size=28)
        HorizontalDivider(color=LocalHunter.current.line.copy(alpha=.70f))
    }
}

@Composable
fun MonarchStatTile(number: String, caption: String, modifier: Modifier=Modifier) {
    HPanel(modifier,padding=12) {
        HText(number,size=29,bold=true,color=LocalHunter.current.accent,maxLines=1)
        HText(caption,size=12,color=LocalHunter.current.muted,maxLines=2)
    }
}
