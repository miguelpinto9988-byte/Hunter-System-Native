package com.huntersystem.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Iconos originales 2D pixel art: archivos SVG locales, sin fondos ni red. */
private val iconToSvg = mapOf(
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
    "mobility" to "leaf"
)

/** Cache de bitmaps de 128px compartido entre pantallas: sin redibujar SVG en cada recomposición. */
private val hunterPixelCache = object : LruCache<String, ImageBitmap>(100) {}

/** API histórica mantenida para no romper las llamadas existentes de Monarca. */
@Composable
fun HunterPngIcon(key:String, modifier:Modifier=Modifier, contentDescription:String?=null) {
    val graphic=iconToSvg[key] ?: "lightning"
    val path="hunter-icons/$graphic.svg"
    val ctx=LocalContext.current
    val bitmap by produceState<ImageBitmap?>(hunterPixelCache.get(path),path) {
        value=withContext(Dispatchers.IO) {
            synchronized(hunterPixelCache){hunterPixelCache.get(path)} ?: runCatching {
                ctx.assets.open(path).use { input ->
                    val picture=SVG.getFromInputStream(input).renderToPicture(128,128)
                    val bitmap=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888)
                    Canvas(bitmap).drawPicture(picture)
                    bitmap.asImageBitmap().also { converted ->
                        synchronized(hunterPixelCache){hunterPixelCache.put(path,converted)}
                    }
                }
            }.getOrNull()
        }
    }
    if(bitmap!=null) Image(
        bitmap=bitmap!!,contentDescription=contentDescription,modifier=modifier,
        contentScale=ContentScale.Fit,filterQuality=FilterQuality.None
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
