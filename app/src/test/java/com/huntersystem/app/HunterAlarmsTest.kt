package com.huntersystem.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Casos puros: horario local, repetición, migración y persistencia JSON. */
class HunterAlarmsTest {
 private val lima=ZoneId.of("America/Lima")
 @Test fun multipleDaysAndNextWeek() {
  val alarm=HunterAlarm("test-1","Misiones",LocalTime.of(18,30),setOf(1,3))
  val monday=ZonedDateTime.of(2026,10,26,8,0,0,0,lima)
  assertEquals("2026-10-26T18:30",HunterAlarms.next(alarm,monday)!!.toLocalDateTime().toString())
  assertEquals("2026-10-28T18:30",HunterAlarms.next(alarm,monday.plusHours(12))!!.toLocalDateTime().toString())
  assertEquals("2026-11-02T18:30",HunterAlarms.next(alarm,monday.plusDays(6))!!.toLocalDateTime().toString())
 }
 @Test fun disabledAlarmHasNoNextOccurrence() {
  val alarm=HunterAlarm("test-2","Pausa",LocalTime.NOON,(1..7).toSet(),false)
  assertNull(HunterAlarms.next(alarm,ZonedDateTime.of(2026,10,26,9,0,0,0,lima)))
 }
 @Test fun migrateOldDailyAndIntervalWithoutLosingDayChoices() {
  val daily=obj("enabled" to true,"mode" to "daily","time" to "17:45",
   "days" to arr(listOf(2,4)))
  val first=HunterAlarms.legacy(daily)
  assertEquals(1,first.size)
  assertEquals("17:45",first.first().time.toString())
  assertEquals(setOf(2,4),first.first().days)
  val interval=obj("enabled" to true,"mode" to "interval","start" to "09:00",
   "end" to "17:00","interval" to 4,"days" to arr(listOf(1,3,5)))
  assertEquals(listOf("09:00","13:00","17:00"),
   HunterAlarms.legacy(interval).map{it.time.toString()})
  assertTrue(HunterAlarms.legacy(obj("enabled" to false)).isEmpty())
 }
 @Test fun jsonRoundTripAndInvalidDataIgnored(){
  val alarm=HunterAlarm("test-3","Descansar",LocalTime.of(15,5),setOf(1,5),
   true,"soft","Toma una pausa")
  val back=HunterAlarms.fromJson(JSONObject(alarm.toJson().toString()))
  assertEquals(alarm,back)
  assertNull(HunterAlarms.fromJson(obj("id" to "bad/identifier","title" to "X",
   "time" to "25:66","days" to arr(listOf(9)))))
 }
 @Test fun supportsDaylightSavingTransition(){
  val zone=ZoneId.of("America/New_York")
  val alarm=HunterAlarm("dst","Pausa",LocalTime.of(2,30),setOf(7))
  val now=ZonedDateTime.of(2027,3,14,1,0,0,0,zone)
  val next=HunterAlarms.next(alarm,now)!!
  assertTrue(next.isAfter(now))
  assertEquals(3,next.hour)
 }
}
