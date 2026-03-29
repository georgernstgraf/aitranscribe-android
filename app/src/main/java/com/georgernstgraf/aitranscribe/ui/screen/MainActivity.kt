package com.georgernstgraf.aitranscribe.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.ui.theme.AITranscribeTheme
import com.georgernstgraf.aitranscribe.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.e("MainActivity", "=== onCreate: STARTED ===")
        
        setContent {
            AITranscribeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
        
        checkMicrophonePermission()
    }

    override fun onStart() {
        super.onStart()
        checkMicrophonePermission()
    }

    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val hasKeys = remember {
        val prefs = SecurePreferences(context)
        !prefs.peekGroqApiKey().isNullOrBlank() && !prefs.peekOpenRouterApiKey().isNullOrBlank()
    }
    val startDestination = if (hasKeys) "main" else "setup"
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("setup") {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate("main") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainScreen(
                navController = navController
            )
        }

        composable(
            route = "transcription/{transcription_id}/{view_filter}",
            arguments = listOf(
                navArgument("transcription_id") { type = NavType.LongType },
                navArgument("view_filter") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val transcriptionId = backStackEntry.arguments?.getLong("transcription_id") ?: return@composable
            val viewFilter = backStackEntry.arguments?.getString("view_filter") ?: return@composable
            TranscriptionDetailScreen(
                transcriptionId = transcriptionId,
                viewFilter = viewFilter?.let { ViewFilter.valueOf(it) } ?: ViewFilter.ALL,
                navController = navController
            )
        }

        composable("search") {
            SearchScreen(
                navController = navController
            )
        }

        composable("settings") {
            SettingsScreen(
                navController = navController
            )
        }
    }
}
