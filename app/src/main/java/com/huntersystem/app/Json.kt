package com.huntersystem.app

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.io.InputStream
import java.io.ByteArrayOutputStream

fun JSONObject.s(k:String,d:String="")=if(isNull(k))d else optString(k,d)
fun JSONObject.n(k:String,d:Double=0.0)=optDouble(k,d).let{if(it.isFinite())it else d}
fun JSONObject.i(k:String,d:Int=0)=n(k,d.toDouble()).toInt()
fun JSONObject.b(k:String,d:Boolean=false)=optBoolean(k,d)
fun JSONObject.o(k:String)=optJSONObject(k)?:JSONObject()
fun JSONObject.a(k:String)=optJSONArray(k)?:JSONArray()
fun JSONObject.copyJson()=JSONObject(toString())
fun JSONObject.entries():List<Pair<String,Any?>> = keys().asSequence().map{it to opt(it)}.toList()
fun JSONArray.objects()=(0 until length()).mapNotNull{optJSONObject(it)}
fun JSONArray.strings()=(0 until length()).map{optString(it)}
fun JSONArray.ints()=(0 until length()).map{optInt(it)}
fun JSONArray.copyJson()=JSONArray(toString())
fun obj(vararg pairs:Pair<String,Any?>)=JSONObject().apply{pairs.forEach{put(it.first,it.second?:JSONObject.NULL)}}
fun arr(xs:Iterable<Any?>)=JSONArray().apply{xs.forEach{put(it?:JSONObject.NULL)}}
fun today()=LocalDate.now().toString()
fun plus(date:String,days:Long)=LocalDate.parse(date).plusDays(days).toString()
fun distance(a:String,b:String)=kotlin.math.abs(ChronoUnit.DAYS.between(LocalDate.parse(a),LocalDate.parse(b)))
fun weekday(date:String)=LocalDate.parse(date).dayOfWeek.value%7
fun level(p:JSONObject)=listOf("basic","intermediate","advanced").indexOf(p.s("difficulty")).coerceAtLeast(0)
val groupLabels=linkedMapOf("upper" to "Tren superior","lower" to "Tren inferior","core" to "Tren central","cardio" to "Cardio","mobility" to "Movilidad y activación")
fun kindLabel(kind:String)=mapOf("strength" to "Fuerza","cardio" to "Cardio","recovery" to "Movilidad","rest" to "Descanso")[kind]?:kind
fun readBounded(stream:InputStream,limit:Int):ByteArray{val out=ByteArrayOutputStream();val buf=ByteArray(8192);var total=0;while(true){val n=stream.read(buf);if(n<0)break;total+=n;require(total<=limit){"El archivo supera el tamaño permitido"};out.write(buf,0,n)};return out.toByteArray()}
fun hash(s:String):Long{var h=2166136261L;for(c in s)h=((h xor c.code.toLong())*16777619L)and 0xffffffffL;return h}
fun normalize(s:String)=java.text.Normalizer.normalize(s.lowercase(),java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"")
