package com.huntersystem.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import app.rive.Rive
import app.rive.RiveFileSource
import app.rive.Result
import app.rive.rememberRiveFile
import app.rive.rememberRiveWorkerOrNull

/** Carga exclusivamente los .riv generados para cada ejercicio; no hay fallback de medios. */
@Composable
fun HunterRiveExercise(
    path:String,
    modifier:Modifier=Modifier,
    play:Boolean=true,
    mirror:Boolean=false,
    restartToken:Int=0
){
    val context=LocalContext.current
    val id=path.substringAfterLast('/').removeSuffix(".riv")
    if(!Regex("[a-zA-Z0-9_-]{1,100}").matches(id)||path!="demos-rive/$id.riv"){
        Box(modifier,contentAlignment=Alignment.Center){Text("ID Rive inválido")}
        return
    }
    key(path,restartToken){
        val worker=rememberRiveWorkerOrNull()
        if(worker==null){
            Box(modifier,contentAlignment=Alignment.Center){Text("Motor Rive no disponible")}
        }else{
            // Each artboard is released with the corresponding RiveFile by rememberRiveFile().
            val data=remember(path){
                runCatching{context.assets.open(path).use{it.readBytes()}}.getOrNull()
            }
            if(data==null){
                Box(modifier,contentAlignment=Alignment.Center){Text("Demostración Rive ausente: $id")}
            }else{
                when(val loaded=rememberRiveFile(RiveFileSource.Bytes(data),worker)){
                    Result.Loading -> Box(modifier,contentAlignment=Alignment.Center){
                        CircularProgressIndicator()
                    }
                    is Result.Error -> Box(modifier,contentAlignment=Alignment.Center){
                        Text("No se pudo abrir el archivo Rive: $id")
                    }
                    is Result.Success -> Rive(
                        file=loaded.value,
                        modifier=modifier.graphicsLayer(scaleX=if(mirror)-1f else 1f),
                        playing=play
                    )
                }
            }
        }
    }
}
