package com.example.appcrud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.appcrud.ui.theme.AppCRUDTheme

import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppCRUDTheme {
                MainScreen()
            }
        }
    }
}

// Placeholder hasta que se integre el NavHost real (login, catálogo, solicitudes, etc.).
@Composable
fun MainScreen() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Urbify")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    AppCRUDTheme {
        MainScreen()
    }
}
