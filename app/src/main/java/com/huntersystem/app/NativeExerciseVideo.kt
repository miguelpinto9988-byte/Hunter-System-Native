package com.huntersystem.app

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Sólo los MP4 aprobados y realmente incluidos pueden sustituir a cada SVG.
 * No se asume aprobación técnica de los 800 ejercicios Hunter2000.
 */
object ExerciseVideoAssets {
 private val cache=ConcurrentHashMap<String,Boolean>()
 fun approvedVideo(context:Context,svgPath:String):String?{
  val id=svgPath.substringAfterLast('/').removeSuffix(".svg")
  if(!Regex("[a-zA-Z0-9_-]{1,100}").matches(id))return null
  val path="demos120/$id.mp4"
  val approved=cache.getOrPut(path){
   try{
    val allowed=context.assets.open("demos120/approved.txt").bufferedReader().use{
     it.lineSequence().any{line->line.trim()==id}
    }
    allowed&&context.assets.openFd(path).use{it.length>1000L}
   }catch(_:Exception){false}
  }
  return if(approved)path else null
 }
}

/** Reproductor MP4 local, sin conexión, usando Android MediaPlayer. */
class OfflineExerciseVideoView(context:Context):TextureView(context),TextureView.SurfaceTextureListener{
 private var player:MediaPlayer?=null
 private var path=""
 private var desiredPlay=false
 private var desiredSpeed=1f
 private var desiredProgress=0f
 init{surfaceTextureListener=this}
 fun configure(asset:String,play:Boolean,speed:Float,mirror:Boolean,progress:Float){
  val changed=path!=asset
  path=asset;desiredPlay=play;desiredSpeed=speed.coerceIn(.5f,2f)
  desiredProgress=progress.coerceIn(0f,1f)
  scaleX=if(mirror)-1f else 1f
  if(changed)open()else updatePlayback()
 }
 private fun open(){
  releasePlayer()
  val texture=surfaceTexture?:return
  if(path.isEmpty())return
  try{
   val mp=MediaPlayer()
   context.assets.openFd(path).use{afd->
    mp.setDataSource(afd.fileDescriptor,afd.startOffset,afd.length)
   }
   val surface=Surface(texture)
   mp.setSurface(surface);surface.release()
   mp.isLooping=true;mp.setVolume(0f,0f)
   mp.setOnPreparedListener{updatePlayback()}
   mp.setOnErrorListener{_,_,_->releasePlayer();true}
   player=mp;mp.prepareAsync()
  }catch(_:Exception){releasePlayer()}
 }
 private fun updatePlayback(){
  val mp=player?:return
  try{
   if(mp.duration<=0)return
   if(desiredPlay){
    mp.playbackParams=mp.playbackParams.setSpeed(desiredSpeed)
    if(!mp.isPlaying)mp.start()
   }else{
    if(mp.isPlaying)mp.pause()
    val pos=(mp.duration*desiredProgress).toInt().coerceIn(0,mp.duration-1)
    if(abs(mp.currentPosition-pos)>75)mp.seekTo(pos)
   }
  }catch(_:IllegalStateException){/* Android MediaPlayer todavía se prepara */}
 }
 override fun onSurfaceTextureAvailable(s:SurfaceTexture,w:Int,h:Int){open()}
 override fun onSurfaceTextureSizeChanged(s:SurfaceTexture,w:Int,h:Int){}
 override fun onSurfaceTextureUpdated(s:SurfaceTexture){}
 override fun onSurfaceTextureDestroyed(s:SurfaceTexture):Boolean{releasePlayer();return true}
 private fun releasePlayer(){val old=player;player=null;runCatching{old?.reset()};runCatching{old?.release()}}
 fun release(){releasePlayer()}
}
@Composable
fun OfflineExerciseVideo(asset:String,modifier:Modifier,play:Boolean,speed:Float,mirror:Boolean,progress:Float){
 AndroidView(factory={OfflineExerciseVideoView(it)},modifier=modifier,
  update={it.configure(asset,play,speed,mirror,progress)},onRelease={it.release()})
}
