package com.huntersystem.app

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class MainActivity:ComponentActivity(){
 private var ui:NativeUi?=null
 private val createBackup=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->if(uri!=null)lifecycleScope.launch{runCatching{withContext(Dispatchers.IO){contentResolver.openOutputStream(uri,"wt")!!.use{it.write(ui!!.store.export().toByteArray())}}}.onSuccess{toast("Copia guardada")}.onFailure{toast("No se pudo exportar: ${it.message}")}}}
 private val openBackup=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)lifecycleScope.launch{runCatching{withContext(Dispatchers.IO){contentResolver.openInputStream(uri)!!.use{JSONObject(readBounded(it,40*1024*1024).toString(Charsets.UTF_8))}.let{ui!!.store.validateBackup(it)}}}.onSuccess{ui?.open("confirm-import",it)}.onFailure{toast("No se pudo leer la copia: ${it.message}")}}}
 private val pickAvatar=registerForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)lifecycleScope.launch{runCatching{withContext(Dispatchers.IO){val bytes=contentResolver.openInputStream(uri)!!.use{readBounded(it,20*1024*1024)};val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds);require(bounds.outWidth>0&&bounds.outHeight>0);val sample=BitmapFactory.Options().apply{inSampleSize=(maxOf(bounds.outWidth,bounds.outHeight)/1024).coerceAtLeast(1)};val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,sample)?:error("Imagen inválida");val scale=512f/maxOf(bitmap.width,bitmap.height);val result=if(scale<1)Bitmap.createScaledBitmap(bitmap,(bitmap.width*scale).toInt(),(bitmap.height*scale).toInt(),true) else bitmap;val out=ByteArrayOutputStream();result.compress(Bitmap.CompressFormat.PNG,100,out);if(result!==bitmap)result.recycle();bitmap.recycle();"data:image/png;base64,"+Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP)}}.onSuccess{ui?.pendingAvatar=it}.onFailure{toast("No se pudo abrir la imagen")}}}
 private val notifyPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(!granted)toast("Los recordatorios necesitan permiso de notificaciones")}
 private fun toast(message:String){Toast.makeText(this,message,Toast.LENGTH_LONG).show()}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{var repository by remember{mutableStateOf<NativeStore?>(null)};var error by remember{mutableStateOf<String?>(null)};LaunchedEffect(Unit){try{val s=withContext(Dispatchers.IO){HunterRepository.get(this@MainActivity).also{if(it.readError==null)it.refreshWeek()}};val controller=NativeUi(s);controller.message={toast(it)};controller.export={createBackup.launch("Hunter-System-Backup-${today()}.json")};controller.import={openBackup.launch(arrayOf("application/json","text/plain","application/octet-stream"))};controller.chooseAvatar={pickAvatar.launch("image/*")};controller.requestNotifications={if(android.os.Build.VERSION.SDK_INT>=33)notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)};controller.openUrl={url->runCatching{val uri=Uri.parse(url);require(uri.scheme=="https");startActivity(Intent(Intent.ACTION_VIEW,uri))}.onFailure{toast("No se pudo abrir el enlace")}};savedInstanceState?.getString("tab")?.let{controller.tab=it};ui=controller;repository=s;if(s.readError==null)ReminderScheduler.schedule(this@MainActivity,s.state.o("reminders"));if(s.readError==null)CreatorFeedNative.schedule(this@MainActivity,s.state.b("creatorAuto",true))}catch(e:Exception){error=e.message?:"No se pudo abrir Hunter System"}}
 val s=repository;if(s!=null){HunterTheme(s){HunterRoot(ui!!)}}else EclipseLoading(error){recreate()}
 }}
 override fun onSaveInstanceState(outState:Bundle){ui?.let{outState.putString("tab",it.tab)};super.onSaveInstanceState(outState)}
}
@Composable fun HunterRoot(ui:NativeUi){
 val s=ui.store;s.revision.intValue;val c=LocalHunter.current
 BackHandler(ui.stack.isNotEmpty()||ui.library.isNotBlank()||(ui.tab!="home"&&s.profile!=null)){ui.back()}
 val entrance=remember{Animatable(1f)};val motion=LocalMotionEnabled.current
 LaunchedEffect(ui.tab){if(motion){entrance.snapTo(0f);entrance.animateTo(1f,tween(230,easing=FastOutSlowInEasing))}else entrance.snapTo(1f)}
 BoxWithConstraints(Modifier.fillMaxSize().background(c.bg)){
  val wide=maxWidth>=840.dp
  val companionDock=s.profile!=null&&ui.tab!="profile"&&LocalVisualTier.current.floatingCompanion
  Scaffold(containerColor=c.bg,contentWindowInsets=WindowInsets.safeDrawing,bottomBar={if(s.profile!=null&&!wide)BottomNav(ui)}){padding->
   Row(Modifier.fillMaxSize().padding(padding)){
    if(s.profile!=null&&wide)NavigationRail(ui)
    Box(Modifier.weight(1f).fillMaxHeight(),contentAlignment=Alignment.TopCenter){
     Box(Modifier.widthIn(max=900.dp).fillMaxSize().graphicsLayer{alpha=entrance.value;translationY=(1-entrance.value)*10.dp.toPx()}){
      Box(Modifier.fillMaxSize().padding(bottom=if(companionDock)68.dp else 0.dp)){
      if(s.readError!=null)Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){Heading("Restaurar guardado");HNotice(s.readError!!);HButton("Elegir copia de seguridad",primary=true){ui.import()}}
      else when(ui.tab){"home"->HomeScreen(ui);"plan"->PlanScreen(ui);"objectives"->ObjectivesScreen(ui);"routines"->RoutinesScreen(ui);"book"->ExercisesScreen(ui);"stats"->ProgressScreen(ui);else->ProfileScreen(ui)}
      }
      if(companionDock)FloatingCompanion(ui,Modifier.align(Alignment.BottomEnd).padding(end=18.dp,bottom=12.dp))
     }
    }
   }
  }
 }
 if(ui.stack.isNotEmpty())OverlayRoot(ui,ui.stack.last())
}
private val navigationItems=listOf(Triple("home","Inicio",Sigil.Home),Triple("plan","Misiones",Sigil.Mission),Triple("routines","Rutinas",Sigil.Routine),Triple("book","Ejercicios",Sigil.Exercise))
@Composable fun BottomNav(ui:NativeUi){
 val c=LocalHunter.current
 Row(Modifier.fillMaxWidth().background(c.panel).navigationBarsPadding().padding(horizontal=7.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
  for((id,title,icon)in navigationItems){
   val selected=ui.tab==id||(id=="home"&&ui.tab in listOf("stats","objectives"))
   Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if(selected)c.selected else Color.Transparent)
    .selectable(selected,role=Role.Tab){ui.go(id)}.padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){
    HunterIcon(icon,Modifier.size(23.dp),if(selected)c.accent else c.muted)
    HText(title,size=11,color=if(selected)c.accent else c.muted,bold=selected,maxLines=1)
   }
  }
 }
}
@Composable fun NavigationRail(ui:NativeUi){
 val c=LocalHunter.current
 Column(Modifier.width(108.dp).fillMaxHeight().background(c.panel).padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)){
  HunterIcon(Sigil.Crown,Modifier.padding(vertical=22.dp).size(32.dp))
  for((id,title,icon)in navigationItems){val selected=ui.tab==id||(id=="home"&&ui.tab in listOf("stats","objectives"))
   Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if(selected)c.selected else Color.Transparent).selectable(selected,role=Role.Tab){ui.go(id)}.padding(vertical=15.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
    HunterIcon(icon,color=if(selected)c.accent else c.muted);HText(title,size=12,color=if(selected)c.accent else c.muted)
   }
  }
 }
}
@Composable fun FloatingCompanion(ui:NativeUi,modifier:Modifier=Modifier){
 val c=LocalHunter.current
 Row(modifier.clip(RoundedCornerShape(22.dp)).background(c.panel).border(1.dp,c.accent.copy(alpha=.5f),RoundedCornerShape(22.dp))
  .clickable(role=Role.Button){ui.open("coach")}.semantics{contentDescription="Abrir acompañante ${companionName(ui.store.profile)}"}.padding(horizontal=10.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
  AssetImage("ui33/companion-${companionKey(ui.store.profile)}.webp",Modifier.size(32.dp,42.dp))
  HText(companionName(ui.store.profile).substringBefore(" "),size=11,color=c.accent,bold=true)
 }
}
