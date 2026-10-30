package com.huntersystem.app

import org.junit.Assert.assertEquals
import org.junit.Test

class GuidedSessionLayoutV43Test {
    @Test fun clockFormatsSafeValues(){
        assertEquals("0:00",guidedSessionClock(-1))
        assertEquals("0:45",guidedSessionClock(45))
        assertEquals("1:15",guidedSessionClock(75))
    }
    @Test fun intervalProgressIsBounded(){
        assertEquals(0f,guidedSessionProgress(45,45),0.0001f)
        assertEquals(0.5f,guidedSessionProgress(40,20),0.0001f)
        assertEquals(1f,guidedSessionProgress(40,0),0.0001f)
        assertEquals(0f,guidedSessionProgress(0,0),0.0001f)
        assertEquals(0f,guidedSessionProgress(40,80),0.0001f)
    }
}
