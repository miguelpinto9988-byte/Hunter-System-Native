package com.huntersystem.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** Los identificadores del perfil y de la navegación se mantienen intactos. */
private val iconToPng = mapOf(
    "panel" to "home",
    "missions" to "quest",
    "routines" to "calendar",
    "exercises" to "dumbbell",
    "progress" to "chart",
    "profile" to "profile",
    "menu" to "sliders",
    "reminders" to "bell",
    "back" to "back",
    "settings" to "gear",
    "strength" to "dumbbell",
    "academy" to "book",
    "trophy" to "trophy",
    "cardio" to "heart",
    "start" to "play",
    "pause" to "pause",
    "success" to "check",
    "available" to "check",
    "locked" to "lock",
    "warning" to "warning",
    "timer" to "clock",
    "retry" to "refresh",
    "progress_bar" to "chart",
    "milestone" to "star",
    "streak" to "fire",
    "level" to "trophy",
    "xp" to "lightning",
    "goals" to "target",
    "crystal" to "crystal",
    "rune" to "crystal",
    "monarch_seal" to "crown",
    "portal" to "portal",
    "chest" to "chest",
    "insignia" to "shield",
    "special" to "swords",
    "next" to "next",
    "close" to "close",
    "add" to "add",
    "remove" to "remove",
    "edit" to "edit",
    "book" to "book",
    "training" to "dumbbell",
    "dumbbell" to "dumbbell",
    "focus" to "target",
    "energy" to "lightning",
    "lightning" to "lightning",
    "shield" to "shield",
    "achievement" to "trophy",
    "goal_strength" to "dumbbell",
    "goal_habit" to "checklist",
    "goal_read" to "book",
    "calendar" to "calendar",
    "history" to "clock",
    "rest" to "rest",
    "coach" to "chat",
    "search" to "search",
    "export" to "upload",
    "import" to "download",
    "notifications" to "bell",
    "play" to "play",
    "complete" to "check",
    "delete" to "trash",
    "back_arrow" to "back",
    "goal_finish" to "star",
    "rank" to "crown",
    "stats" to "chart",
    "library" to "book",
    "monarch" to "crown",
    "programs" to "checklist",
    "mobility" to "leaf",
    "week_upper" to "week_upper",
    "week_core" to "week_core",
    "week_lower" to "week_lower",
    "week_mobility" to "week_mobility",
    "week_rest" to "week_rest",
    "week_cardio" to "week_cardio"
)

/** Iconografía Pixel Art original, PNG RGBA transparente empaquetado sin red. */
internal val hunterPixelPngResources:Map<String,Int> = mapOf(
    "add" to R.drawable.hunter_pixel_add,
    "back" to R.drawable.hunter_pixel_back,
    "bell" to R.drawable.hunter_pixel_bell,
    "book" to R.drawable.hunter_pixel_book,
    "calendar" to R.drawable.hunter_pixel_calendar,
    "chart" to R.drawable.hunter_pixel_chart,
    "chat" to R.drawable.hunter_pixel_chat,
    "check" to R.drawable.hunter_pixel_check,
    "checklist" to R.drawable.hunter_pixel_checklist,
    "chest" to R.drawable.hunter_pixel_chest,
    "clock" to R.drawable.hunter_pixel_clock,
    "close" to R.drawable.hunter_pixel_close,
    "crown" to R.drawable.hunter_pixel_crown,
    "crystal" to R.drawable.hunter_pixel_crystal,
    "download" to R.drawable.hunter_pixel_download,
    "dumbbell" to R.drawable.hunter_pixel_dumbbell,
    "edit" to R.drawable.hunter_pixel_edit,
    "fire" to R.drawable.hunter_pixel_fire,
    "gear" to R.drawable.hunter_pixel_gear,
    "heart" to R.drawable.hunter_pixel_heart,
    "home" to R.drawable.hunter_pixel_home,
    "leaf" to R.drawable.hunter_pixel_leaf,
    "lightning" to R.drawable.hunter_pixel_lightning,
    "lock" to R.drawable.hunter_pixel_lock,
    "next" to R.drawable.hunter_pixel_next,
    "pause" to R.drawable.hunter_pixel_pause,
    "play" to R.drawable.hunter_pixel_play,
    "portal" to R.drawable.hunter_pixel_portal,
    "profile" to R.drawable.hunter_pixel_profile,
    "quest" to R.drawable.hunter_pixel_quest,
    "refresh" to R.drawable.hunter_pixel_refresh,
    "remove" to R.drawable.hunter_pixel_remove,
    "rest" to R.drawable.hunter_pixel_rest,
    "search" to R.drawable.hunter_pixel_search,
    "shield" to R.drawable.hunter_pixel_shield,
    "sliders" to R.drawable.hunter_pixel_sliders,
    "star" to R.drawable.hunter_pixel_star,
    "swords" to R.drawable.hunter_pixel_swords,
    "target" to R.drawable.hunter_pixel_target,
    "trash" to R.drawable.hunter_pixel_trash,
    "trophy" to R.drawable.hunter_pixel_trophy,
    "upload" to R.drawable.hunter_pixel_upload,
    "warning" to R.drawable.hunter_pixel_warning,
    "week_cardio" to R.drawable.hunter_pixel_week_cardio,
    "week_core" to R.drawable.hunter_pixel_week_core,
    "week_lower" to R.drawable.hunter_pixel_week_lower,
    "week_mobility" to R.drawable.hunter_pixel_week_mobility,
    "week_rest" to R.drawable.hunter_pixel_week_rest,
    "week_upper" to R.drawable.hunter_pixel_week_upper
)

internal fun hunterIconDrawableId(key:String):Int {
    val graphic=iconToPng[key] ?: "lightning"
    return hunterPixelPngResources[graphic] ?: R.drawable.hunter_pixel_lightning
}

/** Un ImageView real, como en Agenda Clara, resuelve cada PNG con R.drawable.
 * No se crea dibujo mediante Canvas, XML vectorial ni tintes que alteren la imagen.
 * Desactivar el filtro evita difuminar los píxeles al mostrar tamaños pequeños.
 */
internal fun updateHunterNativeIcon(view: ImageView, drawableId: Int, description: String?) {
    if (view.tag != drawableId) {
        view.setImageResource(drawableId)
        view.tag = drawableId
    }
    view.imageTintList = null
    (view.drawable as? BitmapDrawable)?.setFilterBitmap(false)
    view.scaleType = ImageView.ScaleType.FIT_CENTER
    view.contentDescription = description
    view.importantForAccessibility = if (description == null)
        View.IMPORTANT_FOR_ACCESSIBILITY_NO else View.IMPORTANT_FOR_ACCESSIBILITY_YES
}

internal fun nativeHunterIconView(
    context: Context,
    drawableId: Int,
    description: String?
): ImageView = ImageView(context).apply {
    setBackgroundColor(Color.TRANSPARENT)
    updateHunterNativeIcon(this, drawableId, description)
}

@Composable
private fun HunterNativePngImage(
    drawableId: Int,
    modifier: Modifier,
    contentDescription: String?
) {
    AndroidView(
        factory = { context -> nativeHunterIconView(context, drawableId, contentDescription) },
        modifier = modifier,
        update = { view -> updateHunterNativeIcon(view, drawableId, contentDescription) }
    )
}

/** API histórica: las pantallas y claves semánticas permanecen intactas. */
@Composable
fun HunterPngIcon(
    key: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    HunterNativePngImage(hunterIconDrawableId(key), modifier, contentDescription)
}

/** Variantes de selección extraídas de la lámina Pixel Art aprobada.
 * La navegación dibuja exclusivamente bitmaps PNG (sin Canvas ni VectorDrawable).
 */
private val hunterSelectedNavResources: Map<String, Int> = mapOf(
    "panel" to R.drawable.hunter_nav_home,
    "missions" to R.drawable.hunter_nav_quest,
    "routines" to R.drawable.hunter_nav_calendar,
    "exercises" to R.drawable.hunter_nav_dumbbell,
    "progress" to R.drawable.hunter_nav_chart,
    "profile" to R.drawable.hunter_nav_profile
)

internal fun hunterNavigationDrawableId(key: String, selected: Boolean): Int =
    if (selected) hunterSelectedNavResources[key] ?: hunterIconDrawableId(key)
    else hunterIconDrawableId(key)

@Composable
fun HunterNavigationPngIcon(
    key: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    HunterNativePngImage(
        hunterNavigationDrawableId(key, selected),
        modifier,
        contentDescription
    )
}

/** Clave adaptativa basada en el contenido REAL de la rutina y sus categorías. */
fun hunterRoutinePngKey(title:String,category:String=""):String{
    val text=(title+" "+category).lowercase()
    return when{
        listOf("cardio","resistencia","correr","intervalo","aeróbic").any{it in text}->"cardio"
        listOf("movilidad","flexibilidad","estira","yoga","recupera").any{it in text}->"mobility"
        listOf("hábito","constancia","disciplina").any{it in text}->"goal_habit"
        listOf("brazo","pecho","fuerza","masa","muscular","peso","gimnasio","espalda").any{it in text}->"strength"
        listOf("descans","sueño").any{it in text}->"rest"
        else->"training"
    }
}

/** Clave de imagen que depende del título real de la meta, no de un estado inventado. */
fun hunterGoalPngKey(title:String):String{
    val x=title.lowercase()
    return when{
        listOf("leer","estudiar","libro","aprender","curso").any{it in x}->"goal_read"
        listOf("entren","muscul","fuerza","gimnas","peso").any{it in x}->"goal_strength"
        listOf("caminar","correr","cardio","resistencia").any{it in x}->"cardio"
        listOf("dormir","descans","meditar","hábito","agua","día","diario").any{it in x}->"goal_habit"
        else->"goals"
    }
}
