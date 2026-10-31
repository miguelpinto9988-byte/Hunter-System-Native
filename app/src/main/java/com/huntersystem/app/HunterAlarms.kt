package com.huntersystem.app

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Alarmas locales e independientes: no dependen de Internet ni de ejecutar Compose. */
data class HunterAlarm(
 val id:String, val title:String, val time:LocalTime, val days:Set<Int>,
 val enabled:Boolean=true, val tone:String="energetic", val detail:String=""
) {
 fun toJson():JSONObject=obj("id" to id,"title" to title,"time" to time.toString(),
  "days" to arr(days.sorted()),"enabled" to enabled,"tone" to tone,"detail" to detail)
}

object HunterAlarms {
 const val KEY="hunterAlarms43"
 const val FIRE="com.huntersystem.app.ALARM_FIRE"
 const val SNOOZE="com.huntersystem.app.ALARM_SNOOZE"
 const val DISMISS="com.huntersystem.app.ALARM_DISMISS"
 const val OPEN="com.huntersystem.app.OPEN_ALARMS"
 private const val MAX_ALARMS=12
 private const val LEGACY_ID=2194
 private const val CH_HIGH="hunter-alarms-43"
 private const val CH_SOFT="hunter-alarms-soft-43"
 private const val PI_FLAGS=PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
 private val formatter=DateTimeFormatter.ofPattern("HH:mm")

 fun fromJson(json:JSONObject):HunterAlarm?=runCatching{
  val id=json.s("id");require(id.matches(Regex("[A-Za-z0-9_-]{1,64}")))
  val title=json.s("title").trim().take(70);require(title.isNotBlank())
  val time=LocalTime.parse(json.s("time"))
  val days=json.a("days").ints().filter{it in 1..7}.toSet();require(days.isNotEmpty())
  HunterAlarm(id,title,time,days,json.b("enabled",true),
   if(json.s("tone")=="soft")"soft" else "energetic",json.s("detail").take(140))
 }.getOrNull()
 fun list(store:NativeStore):List<HunterAlarm> =
  store.state.a(KEY).objects().take(MAX_ALARMS).mapNotNull(::fromJson).distinctBy{it.id}

 /** Migra la configuración antigua sin modificar el plan, el perfil o los registros. */
 fun legacy(reminders:JSONObject):List<HunterAlarm>{
  if(!reminders.b("enabled"))return emptyList()
  val days=reminders.a("days").ints().filter{it in 1..7}.toSet().ifEmpty{(1..7).toSet()}
  val hours=if(reminders.s("mode","daily")=="interval"){
   val start=runCatching{LocalTime.parse(reminders.s("start","09:00"))}.getOrDefault(LocalTime.of(9,0))
   val end=runCatching{LocalTime.parse(reminders.s("end","20:00"))}.getOrDefault(LocalTime.of(20,0))
   val interval=reminders.i("interval",3).coerceIn(1,12)
   generateSequence(start){it.plusHours(interval.toLong())}
    .takeWhile{!it.isAfter(end)}.take(MAX_ALARMS).toList()
  }else listOf(runCatching{LocalTime.parse(reminders.s("time","18:00"))}.getOrDefault(LocalTime.of(18,0)))
  return hours.mapIndexed{index,t->
   HunterAlarm("legacy-$index","Mi misión diaria",t,days,true,"soft")
  }
 }
 fun migrate(store:NativeStore){
  if(store.state.has(KEY)||store.readError!=null)return
  store.state.put(KEY,arr(legacy(store.state.o("reminders")).map{it.toJson()}))
  store.save()
 }
 fun next(item:HunterAlarm,now:ZonedDateTime=ZonedDateTime.now()):ZonedDateTime?{
  if(!item.enabled)return null
  for(offset in 0L..7L){
   val date=now.toLocalDate().plusDays(offset)
   if(date.dayOfWeek.value !in item.days)continue
   // ZonedDateTime resuelve correctamente cambios de horario de verano.
   val candidate=date.atTime(item.time).atZone(now.zone)
   if(candidate.isAfter(now))return candidate
  }
  return null
 }
 fun newAlarm(title:String,time:LocalTime,days:Set<Int>,tone:String,detail:String)=
  HunterAlarm(UUID.randomUUID().toString(),title.trim().take(70),time,days,
   true,if(tone=="soft")"soft" else "energetic",detail.trim().take(140))
 fun persist(store:NativeStore,alarms:List<HunterAlarm>){
  require(alarms.size<=MAX_ALARMS){"Máximo 12 alarmas"}
  require(alarms.map{it.id}.distinct().size==alarms.size){"Identificador de alarma repetido"}
  require(alarms.all{it.title.isNotBlank()&&it.days.isNotEmpty()&&it.days.all{d->d in 1..7}})
  val old=list(store).map{it.id}.toSet()
  store.state.put(KEY,arr(alarms.map{it.toJson()}))
  store.save()
  for(id in old-alarms.map{it.id}.toSet())cancel(store.context,id)
  sync(store)
 }
 private fun request(context:Context,action:String,id:String,extra:Boolean=false):PendingIntent{
  val data=Uri.parse("hunteralarm://"+action.substringAfterLast('.')+"/"+Uri.encode(id)+if(extra)"/snooze" else "")
  val intent=Intent(context,HunterAlarmReceiver::class.java).setAction(action).setData(data)
    .putExtra("id",id).putExtra("snoozed",extra)
  return PendingIntent.getBroadcast(context,0,intent,PI_FLAGS)
 }
 private fun cancelLegacy(context:Context,manager:AlarmManager){
  val i=Intent().setClassName(context,context.packageName+".ReminderReceiver")
    .setAction("com.huntersystem.app.REMINDER")
  val old=PendingIntent.getBroadcast(context,LEGACY_ID,i,PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
  if(old!=null){manager.cancel(old);old.cancel()}
 }
 private fun plan(context:Context,id:String,whenMillis:Long,snoozed:Boolean=false){
  val manager=context.getSystemService(AlarmManager::class.java)
  val pending=request(context,FIRE,id,snoozed)
  try{
   if(Build.VERSION.SDK_INT<31||manager.canScheduleExactAlarms())
    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,whenMillis,pending)
   else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,whenMillis,pending)
  }catch(_:SecurityException){
   manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,whenMillis,pending)
  }
 }
 fun exactAllowed(context:Context):Boolean{
  val manager=context.getSystemService(AlarmManager::class.java)
  return Build.VERSION.SDK_INT<31||manager.canScheduleExactAlarms()
 }
 fun cancel(context:Context,id:String){
  val manager=context.getSystemService(AlarmManager::class.java)
  for(snoozed in listOf(false,true)){
   val pending=request(context,FIRE,id,snoozed)
   manager.cancel(pending);pending.cancel()
  }
  context.getSystemService(NotificationManager::class.java).cancel(notificationId(id))
 }
 fun sync(store:NativeStore){
  if(store.readError!=null)return
  migrate(store)
  val manager=store.context.getSystemService(AlarmManager::class.java)
  cancelLegacy(store.context,manager)
  for(item in list(store)){
   // Sólo se cancela la recurrencia; un aviso pospuesto explícitamente se mantiene.
   val pi=request(store.context,FIRE,item.id)
   manager.cancel(pi)
   if(item.enabled)next(item)?.let{plan(store.context,item.id,it.toInstant().toEpochMilli())}
   else {
    manager.cancel(request(store.context,FIRE,item.id,true))
    store.context.getSystemService(NotificationManager::class.java).cancel(notificationId(item.id))
   }
  }
 }
 fun snooze(context:Context,id:String){
  plan(context,id,System.currentTimeMillis()+10*60*1000L,true)
 }
 private fun notificationId(id:String)=31000+(id.hashCode() and 0x3fff)
 private fun channels(context:Context){
  val manager=context.getSystemService(NotificationManager::class.java)
  val loud=NotificationChannel(CH_HIGH,"Alarmas del cazador",NotificationManager.IMPORTANCE_HIGH)
  loud.description="Avisos importantes de misiones y hábitos";loud.enableVibration(true)
  loud.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
  val soft=NotificationChannel(CH_SOFT,"Avisos suaves del cazador",NotificationManager.IMPORTANCE_DEFAULT)
  soft.description="Recordatorios tranquilos";soft.enableVibration(false)
  manager.createNotificationChannels(listOf(loud,soft))
 }
 fun notify(context:Context,item:HunterAlarm){
  if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(
      context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
  channels(context)
  val open=Intent(context,MainActivity::class.java).setAction(OPEN)
    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
  val content=PendingIntent.getActivity(context,0,open,PI_FLAGS)
  val snoozePi=request(context,SNOOZE,item.id)
  val dismissPi=request(context,DISMISS,item.id)
  val message=item.detail.ifBlank{"Revisa tus misiones y elige cómo continuar."}
  val notification=Notification.Builder(context,if(item.tone=="soft")CH_SOFT else CH_HIGH)
   .setSmallIcon(R.drawable.notification_icon_pixel)
   .setContentTitle("Hunter System · "+item.title)
   .setContentText(message)
   .setStyle(Notification.BigTextStyle().bigText(message))
   .setCategory(if(item.tone=="soft")Notification.CATEGORY_REMINDER else Notification.CATEGORY_ALARM)
   .setPriority(if(item.tone=="soft")Notification.PRIORITY_DEFAULT else Notification.PRIORITY_HIGH)
   .setVisibility(Notification.VISIBILITY_PRIVATE)
   .setContentIntent(content)
   .setWhen(System.currentTimeMillis())
   .setAutoCancel(true)
   .addAction(Notification.Action.Builder(R.drawable.notification_icon_pixel,"Posponer 10 min",snoozePi).build())
   .addAction(Notification.Action.Builder(R.drawable.notification_icon_pixel,"Descartar",dismissPi).build())
   .build()
  context.getSystemService(NotificationManager::class.java).notify(notificationId(item.id),notification)
 }
 fun clearNotification(context:Context,id:String){
  context.getSystemService(NotificationManager::class.java).cancel(notificationId(id))
 }
}

/** Recibe alarmas incluso con la aplicación cerrada; reprograma tras reinicio y cambio horario. */
class HunterAlarmReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  val pending=goAsync()
  Thread({
   try{
    val store=HunterRepository.get(context.applicationContext)
    if(store.readError!=null)return@Thread
    HunterAlarms.migrate(store)
    when(intent.action){
     HunterAlarms.FIRE -> {
      val id=intent.getStringExtra("id")?:return@Thread
      val alarm=HunterAlarms.list(store).firstOrNull{it.id==id&&it.enabled}?:return@Thread
      HunterAlarms.notify(context,alarm)
      if(!intent.getBooleanExtra("snoozed",false))HunterAlarms.sync(store)
     }
     HunterAlarms.SNOOZE -> {
      val id=intent.getStringExtra("id")?:return@Thread
      HunterAlarms.clearNotification(context,id)
      if(HunterAlarms.list(store).any{it.id==id&&it.enabled})HunterAlarms.snooze(context,id)
     }
     HunterAlarms.DISMISS -> intent.getStringExtra("id")?.let{HunterAlarms.clearNotification(context,it)}
     else -> HunterAlarms.sync(store)
    }
   }finally{pending.finish()}
  },"hunter-alarm-dispatch").start()
 }
}
