package com.georgernstgraf.aitranscribe.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.georgernstgraf.aitranscribe.data.local.TranscriptionDatabase
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.ui.components.CenteredToastHost
import com.georgernstgraf.aitranscribe.ui.screen.MainScreen
import com.georgernstgraf.aitranscribe.ui.screen.SearchScreen
import com.georgernstgraf.aitranscribe.ui.screen.SettingsScreen
import com.georgernstgraf.aitranscribe.ui.screen.SetupScreen
import com.georgernstgraf.aitranscribe.ui.screen.TranscriptionDetailScreen
import com.georgernstgraf.aitranscribe.ui.screen.auth.ConnectProviderScreen
import com.georgernstgraf.aitranscribe.ui.screen.auth.ProviderAuthScreen
import com.georgernstgraf.aitranscribe.ui.theme.AITranscribeTheme
import com.georgernstgraf.aitranscribe.util.ToastManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var toastManager: ToastManager
    
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainNavigation()
                        CenteredToastHost(toastManager = toastManager)
                    }
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
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val hasKeys = remember {
        runBlocking {
            val providers = TranscriptionDatabase.getDatabase(context).providerModelDao().getAllProviders()
            val hasGroq = providers.any { it.id == "groq" && !it.apiToken.isNullOrBlank() }
            val hasOpenRouter = providers.any { it.id == "openrouter" && !it.apiToken.isNullOrBlank() }
            hasGroq && hasOpenRouter
        }
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

        composable("connect_provider") {
            ConnectProviderScreen(
                navController = navController
            )
        }

        composable(
            route = "auth/{provider_id}",
            arguments = listOf(navArgument("provider_id") { type = NavType.StringType })
        ) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString("provider_id") ?: return@composable
            ProviderAuthScreen(
                providerId = providerId,
                navController = navController
            )
        }
    }
}
