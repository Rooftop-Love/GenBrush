package com.example.qwen_image.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.qwen_image.data.local.PreferencesManager
import com.example.qwen_image.data.repository.GenerationRepository
import com.example.qwen_image.ui.gallery.FullImageViewerScreen
import com.example.qwen_image.ui.gallery.GalleryScreen
import com.example.qwen_image.ui.gallery.GalleryViewModel
import com.example.qwen_image.ui.imageedit.ImageEditScreen
import com.example.qwen_image.ui.imageedit.ImageEditViewModel
import com.example.qwen_image.ui.localization.LocalStrings
import com.example.qwen_image.ui.localization.resolveAppStrings
import com.example.qwen_image.ui.settings.SettingsScreen
import com.example.qwen_image.ui.settings.SettingsViewModel
import com.example.qwen_image.ui.texttoimage.TextToImageScreen
import com.example.qwen_image.ui.texttoimage.TextToImageViewModel

@Composable
fun AppNavigation(
    prefs: PreferencesManager,
    repository: GenerationRepository,
    language: String,
    onLanguageChange: (String) -> Unit
) {
    val strings = resolveAppStrings(language)

    CompositionLocalProvider(LocalStrings provides strings) {
        val navController = rememberNavController()
        val bottomBarScreens = listOf(
            Screen.TextToImage,
            Screen.ImageEdit,
            Screen.Gallery,
            Screen.Settings
        )
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        Scaffold(
            bottomBar = {
                if (currentRoute in bottomBarScreens.map { it.route }) {
                    NavigationBar {
                        bottomBarScreens.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.title(strings)) },
                                label = { Text(screen.title(strings)) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                    }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.TextToImage.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.TextToImage.route) {
                    val vm: TextToImageViewModel = viewModel(
                        factory = TextToImageViewModel.factory(repository, prefs)
                    )
                    TextToImageScreen(vm)
                }

                composable(Screen.ImageEdit.route) {
                    val vm: ImageEditViewModel = viewModel(
                        factory = ImageEditViewModel.factory(repository, prefs)
                    )
                    ImageEditScreen(vm)
                }

                composable(Screen.Gallery.route) {
                    val vm: GalleryViewModel = viewModel(
                        factory = GalleryViewModel.factory(repository)
                    )
                    GalleryScreen(
                        viewModel = vm,
                        onImageClick = { entryId ->
                            navController.navigate("gallery/image/$entryId")
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    val vm: SettingsViewModel = viewModel(
                        factory = SettingsViewModel.factory(prefs)
                    )
                    SettingsScreen(viewModel = vm, onLanguageChange = onLanguageChange)
                }

                composable(
                    "gallery/image/{imageId}",
                    arguments = listOf(navArgument("imageId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val imageId = backStackEntry.arguments?.getString("imageId") ?: return@composable
                    FullImageViewerScreen(
                        imageId = imageId,
                        repository = repository,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
