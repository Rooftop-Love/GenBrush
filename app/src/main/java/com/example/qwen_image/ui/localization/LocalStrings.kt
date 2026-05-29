package com.example.qwen_image.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

val LocalStrings = staticCompositionLocalOf { AppStrings.ZH }

fun resolveAppStrings(language: String): AppStrings = when (language) {
    "zh" -> AppStrings.ZH
    "en" -> AppStrings.EN
    else -> {
        val deviceLang = Locale.getDefault().language
        if (deviceLang == "zh") AppStrings.ZH else AppStrings.EN
    }
}
