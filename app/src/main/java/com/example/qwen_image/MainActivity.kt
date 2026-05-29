package com.example.qwen_image

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.qwen_image.ui.navigation.AppNavigation
import com.example.qwen_image.ui.theme.Qwen_ImageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as QwenImageApp
        setContent {
            Qwen_ImageTheme {
                val language by app.language.collectAsState()
                AppNavigation(
                    prefs = app.prefs,
                    repository = app.repository,
                    language = language,
                    onLanguageChange = app::setLanguage
                )
            }
        }
    }
}
