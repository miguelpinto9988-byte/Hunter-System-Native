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
 val s=repository;if(s!=null){HunterTheme(s){HunterRoot(ui!!)}}else Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xff10171a)),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(15.dp)){Heading("HUNTER SYSTEM",size=38);HText(error?:"Preparando tu sistema…",color=androidx.compose.ui.graphics.Color(0xffbdd4df));if(error!=null)HButton("Reintentar"){recreate()}}}
 }}
 override fun onSaveInstanceState(outState:Bundle){ui?.let{outState.putString("tab",it.tab)};super.onSaveInstanceState(outState)}
}
@Composable fun HunterRoot(ui:NativeUi){val s=ui.store;s.revision.intValue;val c=LocalHunter.current;BackHandler(ui.stack.isNotEmpty()||ui.library.isNotBlank()||ui.tab !in listOf("home","profile")){ui.back()};Scaffold(containerColor=c.bg,bottomBar={if(s.profile!=null)BottomNav(ui)}){padding->Box(Modifier.fillMaxSize().padding(padding)){if(s.readError!=null)Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){Heading("Restaurar guardado");HNotice(s.readError!!);HButton("Elegir copia de seguridad",primary=true){ui.import()}} else when(ui.tab){"home"->HomeScreen(ui);"plan"->PlanScreen(ui);"objectives"->ObjectivesScreen(ui);"routines"->RoutinesScreen(ui);"book"->ExercisesScreen(ui);"stats"->ProgressScreen(ui);else->ProfileScreen(ui)}
 if(s.profile!=null&&ui.tab!="profile"&&ui.tab!="home")FloatingCompanion(ui,Modifier.align(Alignment.BottomEnd).padding(end=14.dp,bottom=12.dp))
 }};if(ui.stack.isNotEmpty())OverlayRoot(ui,ui.stack.last())}
@Composable fun BottomNav(ui:NativeUi){ V40BottomNav(ui) }
@Composable fun FloatingCompanion(ui:NativeUi,modifier:Modifier=Modifier){var dy=0f;if(ui.store.profile?.b("animate",true)==true){val transition=rememberInfiniteTransition(label="companion");val animated by transition.animateFloat(0f,-5f,infiniteRepeatable(tween(2200,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="float");dy=animated};Box(modifier.width(65.dp).height(86.dp).graphicsLayer{translationY=dy}.clickable{ui.open("coach")}){AssetImage("ui33/companion-${companionKey(ui.store.profile)}.webp",Modifier.fillMaxSize())}}
