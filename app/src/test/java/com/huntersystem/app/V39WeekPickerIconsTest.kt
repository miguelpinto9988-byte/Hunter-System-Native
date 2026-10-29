package com.huntersystem.app

import androidx.compose.runtime.mutableStateListOf
import org.junit.Assert.assertEquals
import org.junit.Test

/** El icono semanal sigue el plan REAL; no depende del nombre del día. */
class V39WeekPickerIconsTest {
    @Test fun everyExistingPlanAndFocusSelectsTheExpectedPixelIcon() {
        val cases=listOf(
            Triple("train","upper","week_upper"),
            Triple("train","lower","week_lower"),
            Triple("train","core","week_core"),
            Triple("cardio","upper","week_cardio"),
            Triple("mobility","lower","week_mobility"),
            Triple("rest","core","week_rest")
        )
        for((plan,focus,expected) in cases)
            assertEquals(expected,V39Training.of(plan,focus).iconKey)
    }

    @Test fun selectingAnActivityUpdatesWeekAndIconWithoutChangingOtherDays() {
        val week=mutableStateListOf<String>().apply { addAll(List(7){"rest"}) }
        val focus=mutableStateListOf<String>().apply { addAll(List(7){"rest"}) }
        val day=3
        val order=listOf(
            V39Training.UPPER,V39Training.LOWER,V39Training.CARDIO,
            V39Training.MOBILITY,V39Training.CORE,V39Training.REST
        )
        for(activity in order) {
            activity.update(day,week,focus)
            assertEquals(activity,V39Training.of(week[day],focus[day]))
            assertEquals(activity.iconKey,V39Training.of(week[day],focus[day]).iconKey)
            assertEquals("rest",week[2])
            assertEquals("rest",week[4])
        }
    }
}
