package com.example.genbrush.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.genbrush.ui.localization.AppStrings

sealed class Screen(val route: String, private val iconVector: ImageVector, private val titleKey: (AppStrings) -> String) {
    val icon: ImageVector get() = iconVector
    fun title(strings: AppStrings) = titleKey(strings)

    data object TextToImage : Screen("text_to_image", Icons.Default.AutoAwesome, { it.navGenerate })
    data object ImageEdit : Screen("image_edit", Icons.Default.Edit, { it.navEdit })
    data object Gallery : Screen("gallery", Icons.Default.PhotoLibrary, { it.navGallery })
    data object Settings : Screen("settings", Icons.Default.Settings, { it.navSettings })
}
