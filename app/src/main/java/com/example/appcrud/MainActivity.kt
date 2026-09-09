package com.example.appcrud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.appcrud.data.session.ThemeManager
import com.example.appcrud.ui.navigation.AppNavGraph
import com.example.appcrud.ui.theme.UrbifyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme by ThemeManager.flow(context, systemDark).collectAsState(
                initial = ThemeManager.getCached(systemDark)
            )
            val scope = rememberCoroutineScope()

            UrbifyTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AppNavGraph(
                        navController = navController,
                        onToggleDarkTheme = {
                            scope.launch { ThemeManager.saveDarkTheme(context, !isDarkTheme) }
                        },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}
