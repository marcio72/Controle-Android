package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
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
            // Read standard system preference, and allow instant dynamic manual visual toggle in emulator window
            val systemDark = isSystemInDarkTheme()
            var darkThemeManual by remember { mutableStateOf<Boolean?>(null) }
            val resolvedDarkTheme = darkThemeManual ?: systemDark

            var currentScreen by remember { mutableStateOf("splash") }
            val viewModel: AppViewModel = viewModel()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()

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

@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}
