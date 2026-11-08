package com.doodlestudio.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.doodlestudio.app.ui.screens.editor.EditorScreen
import com.doodlestudio.app.ui.screens.gallery.GalleryScreen
import com.doodlestudio.app.ui.screens.home.HomeScreen
import com.doodlestudio.app.ui.screens.splash.SplashScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToEditor = { artworkId ->
                    navController.navigate(Screen.Editor.createRoute(artworkId = artworkId))
                },
                onNavigateToGallery = {
                    navController.navigate(Screen.Gallery.createRoute())
                }
            )
        }

        composable(
            route = Screen.Gallery.route,
            arguments = listOf(
                navArgument("category") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            GalleryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { artworkId ->
                    navController.navigate(Screen.Editor.createRoute(artworkId = artworkId))
                }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("templateId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("artworkId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId")
            val artworkId = backStackEntry.arguments?.getString("artworkId")
            EditorScreen(
                templateId = templateId,
                artworkId = artworkId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGallery = {
                    navController.navigate(Screen.Gallery.createRoute())
                }
            )
        }
    }
}
