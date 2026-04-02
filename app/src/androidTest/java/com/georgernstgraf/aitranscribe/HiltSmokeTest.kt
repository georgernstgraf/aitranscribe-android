package com.georgernstgraf.aitranscribe

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HiltSmokeTest {

    @Test
    fun applicationIsHiltInjected() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        assertTrue(
            "App must be AITranscribeApp with valid Hilt _GeneratedInjector. " +
                "If this fails: run ./gradlew clean assembleDebug",
            app is AITranscribeApp
        )
        val hiltApp = app as AITranscribeApp
        assertNotNull("HiltWorkerFactory must be injected", hiltApp.workerFactory)
    }

    @Test
    fun hiltGeneratedInjectorClassExists() {
        val clazz = Class.forName(
            "com.georgernstgraf.aitranscribe.AITranscribeApp_GeneratedInjector"
        )
        assertNotNull(
            "Hilt _GeneratedInjector must exist. If missing: run ./gradlew clean assembleDebug",
            clazz
        )
    }
}
