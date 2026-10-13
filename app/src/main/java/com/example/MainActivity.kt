package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.VoiceStudioScreen
import com.example.ui.VoiceViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: VoiceViewModel = viewModel()
            val themePalette by viewModel.themePalette.collectAsState()

            MyApplicationTheme(themePalette = themePalette) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Render the comprehensive main view with padded edges
                    VoiceStudioScreen(viewModel = viewModel)
                }
            }
        }
    }
}
