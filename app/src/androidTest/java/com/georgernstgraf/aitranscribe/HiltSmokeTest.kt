package com.georgernstgraf.aitranscribe

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HiltSmokeTest {

    @Test
    fun applicationIsHiltInjected() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        assertTrue(
            app is AITranscribeApp,
            "App must be AITranscribeApp with valid Hilt _GeneratedInjector. " +
                "If this fails: run ./gradlew clean assembleDebug"
        )
        val hiltApp = app as AITranscribeApp
        assertNotNull(hiltApp.workerFactory, "HiltWorkerFactory must be injected")
    }

    @Test
    fun hiltGeneratedInjectorClassExists() {
        val clazz = Class.forName(
            "com.georgernstgraf.aitranscribe.AITranscribeApp_GeneratedInjector"
        )
        assertNotNull(
            clazz,
            "Hilt _GeneratedInjector must exist. If missing: run ./gradlew clean assembleDebug"
        )
    }
}
