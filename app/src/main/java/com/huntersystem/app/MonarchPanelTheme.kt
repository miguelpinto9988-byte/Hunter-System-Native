package com.huntersystem.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import org.json.JSONObject

/** Rediseño acotado a los cuatro paneles de navegación. El perfil, ventanas,
 * persistencia, contenidos, rutas y motor vectorial no se modifican. */
internal val LocalMonarchPanelRefresh = staticCompositionLocalOf { false }

private fun contrast(a: Color, b: Color): Float {
    val x = a.luminance()
    val y = b.luminance()
    return (maxOf(x,y)+.05f)/(minOf(x,y)+.05f)
}

internal fun panelForeground(background: Color): Color =
    if(contrast(Color.White,background) >= contrast(Color(0xFF111318),background)) Color.White else Color(0xFF111318)

private fun accessibleAccent(accent: Color, background: Color, input: Color, neutral: Color): Color {
    var adjusted = accent
    repeat(24) {
        if(contrast(adjusted,background)>=4.5f && contrast(adjusted,input)>=4.5f) return adjusted
        adjusted = lerp(adjusted,neutral,.13f)
    }
    return neutral
}

private fun hex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

internal fun monarchPanelTokens(original: HunterColors): JSONObject {
    val light = original.isLight
    val bg = Color(if(light)0xFFFFFFFF else 0xFF101115)
    val panel = Color(if(light)0xFFFFFFFF else 0xFF191B21)
    val surface = Color(if(light)0xFFF9FAFC else 0xFF202229)
    val input = Color(if(light)0xFFF0F2F5 else 0xFF272A32)
    val text = Color(if(light)0xFF171B24 else 0xFFF1F2F5)
    val accent = accessibleAccent(original.accent,bg,input,text)
    return JSONObject(original.data.toString()).apply {
        put("--bg",hex(bg))
        put("--panel",hex(panel))
        put("--surface",hex(surface))
        put("--input",hex(input))
        put("--line",if(light)"#D7DCE4" else "#3B404C")
        put("--text",hex(text))
        put("--muted",if(light)"#505B6B" else "#B9C0CB")
        put("--accent",hex(accent))
        put("--outlineAccent",hex(accent))
        put("--secondary33",hex(lerp(accent,text,.12f)))
        put("--selected33",hex(lerp(input,accent,if(light).10f else .17f)))
    }
}

@Composable
internal fun MonarchPanelTheme(content: @Composable () -> Unit) {
    val original = LocalHunter.current
    val palette = remember(original.data.toString()) { HunterColors(monarchPanelTokens(original)) }
    val scheme = MaterialTheme.colorScheme.copy(
        primary=palette.accent,onPrimary=panelForeground(palette.accent),
        surface=palette.panel,onSurface=palette.text,background=palette.bg,
        onBackground=palette.text,outline=palette.line
    )
    CompositionLocalProvider(LocalHunter provides palette, LocalMonarchPanelRefresh provides true) {
        MaterialTheme(colorScheme=scheme,content=content)
    }
}
