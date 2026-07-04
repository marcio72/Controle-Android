package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var darkThemeManual by remember { mutableStateOf<Boolean?>(null) }
            val resolvedDarkTheme = darkThemeManual ?: systemDark

            var currentScreen by rememberSaveable { mutableStateOf("splash") }
            val viewModel: AppViewModel = viewModel()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()

            // Pedido de permissão GPS em runtime
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                // Resultado ignorado aqui — o LocationHelper verifica na hora do uso
            }

            // Pede a permissão assim que a tela principal for exibida
            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn) {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }

            MyApplicationTheme(darkTheme = resolvedDarkTheme) {
                if (currentScreen == "splash") {
                    SplashScreen(
                        onSplashFinished = {
                            currentScreen = if (isLoggedIn) "main" else "login"
                        }
                    )
                } else if (!isLoggedIn) {
                    LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = {
                            currentScreen = "main"
                        }
                    )
                } else {
                    DashboardScreen(
                        viewModel = viewModel,
                        darkTheme = resolvedDarkTheme,
                        onToggleDarkTheme = {
                            darkThemeManual = !resolvedDarkTheme
                        }
                    )
                }
            }
        }
    }
}
