package com.huntersystem.app

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Fase 10: una sola política de movimiento para elementos de la interfaz.
 * Preferencias independientes del perfil para no regenerar misiones.
 * Las opciones se almacenan solo en el dispositivo.
 */
data class MonarchMotionSettings(
    val reduced:Boolean,
    val userReduced:Boolean,
    val systemReduced:Boolean,
    val setReduced:(Boolean)->Unit
) {
    fun duration(normalMs:Int):Int=if(reduced)0 else normalMs.coerceIn(0,650)
    fun spec(normalMs:Int):FiniteAnimationSpec<Float> =
        if(reduced) snap() else tween(duration(normalMs))
}

val LocalMonarchMotion=staticCompositionLocalOf {
    MonarchMotionSettings(reduced=false,userReduced=false,systemReduced=false,setReduced={})
}

@Composable
fun MonarchMotionProvider(content:@Composable ()->Unit){
    val ctx=LocalContext.current
    val preferences=remember(ctx){
        ctx.getSharedPreferences("hunter_motion_prefs_v42",Context.MODE_PRIVATE)
    }
    var reduceUser by remember(preferences){
        mutableStateOf(preferences.getBoolean("reduce_motion",false))
    }
    // Cuando Android deshabilita las animaciones, prevalece sobre la preferencia local.
    val reduceSystem=remember(ctx){
        runCatching{
            Settings.Global.getFloat(ctx.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f
        }.getOrDefault(false)
    }
    val motion=MonarchMotionSettings(
        reduced=reduceUser||reduceSystem,
        userReduced=reduceUser,
        systemReduced=reduceSystem,
        setReduced={newValue->
            preferences.edit().putBoolean("reduce_motion",newValue).apply()
            reduceUser=newValue
        }
    )
    CompositionLocalProvider(LocalMonarchMotion provides motion){content()}
}

@Composable
fun MonarchMotionOptions(){
    val motion=LocalMonarchMotion.current
    HPanel(padding=17,decor=false){
        Eyebrow("ACCESIBILIDAD Y RENDIMIENTO")
        Heading("Fluidez y movimiento",size=26)
        HCheck("Reducir animaciones y transiciones",motion.userReduced){
            motion.setReduced()
        }
        HText("La opción se aplica de inmediato, sin modificar el perfil, " +
            "las rutinas ni las estadísticas. Los botones conservan " +
            "una respuesta visual sin animaciones prolongadas.",
            size=14,color=LocalHunter.current.muted)
        if(motion.systemReduced){
            HText("Android tiene las animaciones desactivadas; esta " +
                "preferencia del sistema tiene prioridad.",
                size=13,color=LocalHunter.current.accent)
        }
    }
}
