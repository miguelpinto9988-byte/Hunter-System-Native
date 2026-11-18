package com.huntersystem.app

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import kotlinx.coroutines.isActive
import org.json.JSONObject
import kotlin.math.min

/**
 * Movimiento estructural legado. Lee únicamente demos30/<id>.json y nunca
 * carga ni ejecuta SVG animados. La estética vive por separado en media2d/.
 */
internal class LegacyRigMotion(val frames:List<List<Offset>>){
    fun sample(progress:Float):List<Offset>{
        if(frames.size==1)return frames.first()
        val position=progress.coerceIn(0f,1f)*frames.size
        val first=position.toInt().coerceIn(0,frames.lastIndex)
        val second=(first+1)%frames.size
        val alpha=(position-first).coerceIn(0f,1f)
        return frames[first].zip(frames[second]){a,b->
            Offset(a.x+(b.x-a.x)*alpha,a.y+(b.y-a.y)*alpha)
        }
    }
}

internal object LegacyRigRepository{
    private val validId=Regex("[A-Za-z0-9_-]{1,100}")
    fun load(context:Context,illustration:String):LegacyRigMotion?{
        if(!illustration.startsWith("media2d/")||!illustration.endsWith(".svg"))return null
        val id=illustration.removePrefix("media2d/").removeSuffix(".svg")
        if(!validId.matches(id))return null
        return runCatching {
            val raw=context.assets.open("demos30/$id.json")
                .bufferedReader().use{it.readText()}
            val frames=JSONObject(raw).getJSONArray("frames")
            require(frames.length() in 1..240)
            val poses=List(frames.length()){i->
                val points=frames.getJSONObject(i).getJSONArray("p")
                require(points.length()==11)
                List(11){j->
                    val point=points.getJSONArray(j)
                    require(point.length()>=2)
                    val x=point.getDouble(0).toFloat()
                    val y=point.getDouble(1).toFloat()
                    require(x.isFinite()&&y.isFinite())
                    Offset(x,y)
                }
            }
            LegacyRigMotion(poses)
        }.getOrNull()
    }
}

private val rigBones=listOf(
    0 to 1,1 to 2,1 to 3,3 to 4,1 to 5,5 to 6,
    2 to 7,7 to 8,2 to 9,9 to 10
)

/** Dibuja un esqueleto en movimiento, desacoplado del diseño estético. */
@Composable
internal fun LegacyExerciseRig(
    data:LegacyRigMotion,
    modifier:Modifier=Modifier,
    play:Boolean=true,
    speed:Float=1f,
    mirror:Boolean=false,
    scrub:Float=0f,
    restartToken:Int=0
){
    val colors=LocalHunter.current
    var phase by remember(data){mutableFloatStateOf(0f)}
    LaunchedEffect(data,restartToken){phase=0f}
    // Una edición del deslizador actualiza la pose. Una pausa conserva la última.
    LaunchedEffect(scrub){if(!play)phase=scrub.coerceIn(0f,1f)}
    LaunchedEffect(data,play,speed,restartToken){
        if(play){
            var previous=withFrameNanos{it}
            while(isActive){
                val current=withFrameNanos{it}
                val ms=(current-previous)/1_000_000f
                previous=current
                if(ms>=0f&&ms<250f){
                    phase=(phase+ms*speed.coerceIn(.25f,2f)/4800f)%1f
                }
            }
        }
    }
    val points=data.sample(phase)
    Canvas(modifier){
        // Las poses históricas están en coordenadas 720x800 (no en el
        // viewBox 480x480 de los SVG posteriores).
        val scale=min(size.width/720f,size.height/800f)
        val left=(size.width-720f*scale)/2f
        val top=(size.height-800f*scale)/2f
        fun project(point:Offset):Offset{
            val x=if(mirror)720f-point.x else point.x
            return Offset(left+x*scale,top+point.y*scale)
        }
        val joints=points.map(::project)
        rigBones.forEachIndexed{index,(from,to)->
            drawLine(
                color=if(index<2)colors.text else colors.accent,
                start=joints[from],end=joints[to],
                strokeWidth=10f*scale,cap=StrokeCap.Round
            )
        }
        joints.forEachIndexed{i,point->
            drawCircle(
                color=if(i==0)colors.text else colors.accent,
                radius=if(i==0)21f*scale else 8f*scale,center=point
            )
        }
    }
}
