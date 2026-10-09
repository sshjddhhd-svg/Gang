package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.AuthManager
import com.example.data.repository.GangRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.GangTheme
import com.example.ui.viewmodel.GangViewModel
import com.example.ui.viewmodel.GangViewModelFactory
import com.example.util.GangNotificationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        GangNotificationHelper.initChannel(this)

        setContent {
            // Notification runtime permission launcher
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* Granted or denied handled gracefully */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val authManager = remember { AuthManager(this@MainActivity) }
            val repository = remember { GangRepository(this@MainActivity) }
            val viewModel: GangViewModel = viewModel(
                factory = GangViewModelFactory(repository, authManager)
            )

            val themeMode by viewModel.themeMode.collectAsState()
            val currentUser by viewModel.currentUser.collectAsState()

            GangTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (currentUser == null) {
                        AuthScreen(
                            authManager = authManager,
                            onAuthSuccess = { /* Auth state flow automatically updates */ }
                        )
                    } else {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
