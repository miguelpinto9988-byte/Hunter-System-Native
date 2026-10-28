package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Wallpapers originales para las pantallas reales de Hunter System.
 *
 * Utiliza solo PNG rasterizados, totalmente locales; sin SVG ni Canvas.
 * Se sitúa DETRÁS de Compose: nunca modifica clicks, datos, ni desplazamiento.
 * Los paisajes son estáticos, respetando de forma natural movimiento reducido.
 */
internal fun hunterRealmArtRoute(route:String):String {
    if (!route.startsWith("overlay-")) return when(route){
        "home","plan","routines","book","stats","objectives","profile","recovery"->route
        else->"profile"
    }
    return when(route.removePrefix("overlay-")){
        "exercise","session","video","series","body"->"book"
        "mission","mission-calendar","confirm-finish","record","review"->"plan"
        "routine","assign","routine-editor","custom"->"routines"
        "evolution","history"->"stats"
        "goal-edit","delete-goal"->"objectives"
        "coach","creator","academy","creator-refresh","reminders"->"profile"
        "pain"->"recovery"
        else->"home"
    }
}

@Composable
fun HunterRealmWallpaper(route:String,modifier:Modifier=Modifier){
    val scene=hunterRealmArtRoute(route)
    Box(modifier.background(Color(0xFF050916))){
        AssetImage("hunter-png/wallpapers/$scene.png",Modifier.fillMaxSize(),scale=androidx.compose.ui.layout.ContentScale.Crop,description=null)
        // Protege contraste en todas las resoluciones sin editar la ilustración.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                0f to Color(0x52040919),
                .24f to Color(0x8305091A),
                .49f to Color(0xBB050919),
                .72f to Color(0xE2050917),
                1f to Color(0xFB050916)
        )))
    }
}
