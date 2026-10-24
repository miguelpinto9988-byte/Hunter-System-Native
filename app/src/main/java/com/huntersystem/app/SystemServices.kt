package com.huntersystem.app

import android.Manifest
import android.app.*
import android.app.job.*
import android.content.*
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.*
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import org.json.JSONArray
import java.time.*
import java.net.URL
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object TimerSound{fun play(context:Context){runCatching{MediaPlayer.create(context,R.raw.timer_finished)?.apply{setOnCompletionListener{it.release()};setOnErrorListener{mp,_,_->mp.release();true};start()}}}}
object ReminderScheduler{
 private const val ID=2194
 fun next(r:JSONObject,now:ZonedDateTime=ZonedDateTime.now()):ZonedDateTime?{if(!r.b("enabled"))return null;val days=r.a("days").ints().ifEmpty{(1..7).toList()}.toSet();val candidates=mutableListOf<ZonedDateTime>();fun time(s:String,fallback:String)=runCatching{LocalTime.parse(s)}.getOrElse{LocalTime.parse(fallback)}
  for(i in 0L..7L){val d=now.toLocalDate().plusDays(i);if(d.dayOfWeek.value !in days)continue;if(r.s("mode","daily")=="interval"){var t=d.atTime(time(r.s("start"),"09:00")).atZone(now.zone);val end=d.atTime(time(r.s("end"),"20:00")).atZone(now.zone);val hours=r.i("interval",3).coerceIn(1,12);while(!t.isAfter(end)){if(t.isAfter(now))candidates+=t;t=t.plusHours(hours.toLong())}}else{val t=d.atTime(time(r.s("time"),"18:00")).atZone(now.zone);if(t.isAfter(now))candidates+=t}}
  return candidates.minOrNull()
 }
 fun schedule(context:Context,r:JSONObject){val am=context.getSystemService(AlarmManager::class.java);val pi=PendingIntent.getBroadcast(context,ID,Intent(context,ReminderReceiver::class.java).setAction("com.huntersystem.app.REMINDER"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);am.cancel(pi);next(r)?.let{am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,it.toInstant().toEpochMilli(),pi)}}
 fun notify(context:Context){if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;val nm=context.getSystemService(NotificationManager::class.java);nm.createNotificationChannel(NotificationChannel("hunter-reminders","Recordatorios del sistema",NotificationManager.IMPORTANCE_DEFAULT));val pi=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT);val n=Notification.Builder(context,"hunter-reminders").setSmallIcon(R.drawable.notification_icon).setContentTitle("Hunter System · Tu misión te espera").setContentText("Abre tu misión y revisa cómo te sientes antes de entrenar.").setContentIntent(pi).setAutoCancel(true).build();nm.notify(ID,n)}
}
class ReminderReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){val pending=goAsync();Thread{try{val s=HunterRepository.get(context);if(intent.action!="android.intent.action.BOOT_COMPLETED"&&s.state.o("reminders").b("enabled"))ReminderScheduler.notify(context);ReminderScheduler.schedule(context,s.state.o("reminders"))}finally{pending.finish()}}.start()}}
object CreatorFeedNative{
 fun refresh(store:NativeStore):JSONObject{val old=store.state.o("creatorData");val out=mutableListOf<JSONObject>();for(c in store.catalog.editorial.a("channels").objects()){val prior=old.a("channels").objects().firstOrNull{it.s("channelId")==c.s("channelId")};val channel=c.copyJson();val videos=mutableListOf<JSONObject>();var ok=false
  try{require(Regex("[A-Za-z0-9_-]{15,60}").matches(c.s("channelId")));val con=URL("https://www.youtube.com/feeds/videos.xml?channel_id="+c.s("channelId")).openConnection();con.connectTimeout=12000;con.readTimeout=12000;val bytes=con.getInputStream().use{readBounded(it,2*1024*1024)};val xml=bytes.toString(Charsets.UTF_8);require(!Regex("<!\\s*(DOCTYPE|ENTITY)",RegexOption.IGNORE_CASE).containsMatchIn(xml));val f=DocumentBuilderFactory.newInstance();f.isNamespaceAware=true;f.isExpandEntityReferences=false;runCatching{f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true)};runCatching{f.setFeature("http://xml.org/sax/features/external-general-entities",false)};val doc=f.newDocumentBuilder().parse(org.xml.sax.InputSource(java.io.StringReader(xml)));val entries=doc.getElementsByTagName("entry");for(i in 0 until entries.length){val e=entries.item(i) as org.w3c.dom.Element;fun text(tag:String)=e.getElementsByTagName(tag).item(0)?.textContent?:""
val id=text("yt:videoId");if(Regex("[A-Za-z0-9_-]{11}").matches(id))videos+=obj("id" to id,"channelId" to c.s("channelId"),"title" to text("title").take(160),"published" to text("published"),"chapters" to JSONArray(),"url" to "https://www.youtube.com/watch?v=$id")};ok=true}catch(_:Exception){}
  videos+=prior?.a("videos")?.objects()?:emptyList();channel.put("videos",arr(videos.distinctBy{it.s("id")}.sortedByDescending{it.s("published")}.take(100)));channel.put("status",if(ok)"ok" else "unavailable");channel.put("lastSuccess",if(ok)System.currentTimeMillis() else prior?.n("lastSuccess")?:0);out+=channel
 };return obj("updated" to System.currentTimeMillis(),"channels" to arr(out))}
 fun schedule(context:Context,enabled:Boolean){val scheduler=context.getSystemService(JobScheduler::class.java);scheduler.cancel(2195);if(enabled)scheduler.schedule(JobInfo.Builder(2195,ComponentName(context,CreatorSyncJob::class.java)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(24*60*60*1000L).setPersisted(true).build())}
}
class CreatorSyncJob:JobService(){@Volatile private var stopped=false;override fun onStartJob(params:JobParameters):Boolean{stopped=false;Thread{val s=HunterRepository.get(this);val result=CreatorFeedNative.refresh(s);Handler(Looper.getMainLooper()).post{if(!stopped){if(s.readError==null)runCatching{s.state.put("creatorData",result);s.save()};jobFinished(params,false)}}}.start();return true};override fun onStopJob(params:JobParameters):Boolean{stopped=true;return true}}
@Composable fun NativeVideo(path:String,modifier:Modifier=Modifier){val context=androidx.compose.ui.platform.LocalContext.current;var file by remember(path){mutableStateOf<File?>(null)};LaunchedEffect(path){file=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){require(Regex("[A-Za-z0-9_./-]+\\.mp4").matches(path)&&".." !in path);val target=File(context.cacheDir,"video-"+path.substringAfterLast('/'));if(!target.exists()){val bytes=context.assets.open(path).use{readBounded(it,40*1024*1024)};target.writeBytes(bytes)};target}};val local=file;if(local==null)HText("Cargando demostración…") else AndroidView(factory={VideoView(it).apply{setMediaController(MediaController(it));setVideoPath(local.absolutePath);setOnPreparedListener{mp->mp.isLooping=true;start()}}},modifier=modifier,onRelease={it.stopPlayback()})}
