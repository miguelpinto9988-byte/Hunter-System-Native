package com.huntersystem.app

import android.graphics.Color
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileOnDeviceTest {
    @Test
    fun freshInstallActivatesNativeProfileAndPersistsAfterRecreation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            fun awaitText(label: String) = checkNotNull(device.wait(Until.findObject(By.text(label)), 25_000)) {
                "Missing Compose control: $label"
            }
            fun capture(name: String) {
                val image = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
                try {
                    var bright = 0
                    for (y in image.height / 6 until image.height * 5 / 6 step 5) {
                        for (x in image.width / 10 until image.width * 9 / 10 step 5) {
                            val color = image.getPixel(x, y)
                            if (maxOf(Color.red(color), Color.green(color), Color.blue(color)) > 125) bright++
                        }
                    }
                    assertTrue("Application area appears blank", bright > 120)
                    instrumentation.targetContext.openFileOutput("profile-$name.png", 0).use {
                        image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                    }
                } finally {
                    image.recycle()
                }
            }
            awaitText("PASO 1 DE 4")
            capture("1-claro")
            val fields = device.findObjects(By.clazz("android.widget.EditText"))
            assertTrue("Missing identity fields", fields.size >= 2)
            fields[0].text = "Auditoría Android"
            fields[1].text = "14"
            awaitText("SIGUIENTE ›").click()
            awaitText("PASO 2 DE 4")
            capture("2-claro")
            awaitText("SIGUIENTE ›").click()
            awaitText("PASO 3 DE 4")
            capture("3-claro")
            val theme = checkNotNull(device.wait(Until.findObject(By.textContains("Tema del sistema")), 10_000))
            theme.click()
            awaitText("Negro").click()
            capture("3-oscuro")
            awaitText("SIGUIENTE ›").click()
            awaitText("PASO 4 DE 4")
            capture("4-oscuro")
            assertTrue("Consent must be explicit", !awaitText("ACTIVAR PERFIL").isEnabled)
            awaitText("He revisado mi información y deseo activar mi perfil de cazador.").click()
            awaitText("ACTIVAR PERFIL").click()
            awaitText("SISTEMA ACTIVADO")
            awaitText("ENTRAR AL SISTEMA").click()
            awaitText("Ejercicios")
            scenario.recreate()
            awaitText("Ejercicios")
        }
    }
}
