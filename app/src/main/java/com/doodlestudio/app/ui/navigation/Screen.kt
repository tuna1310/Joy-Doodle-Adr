package com.doodlestudio.app.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Gallery : Screen("gallery?category={category}") {
        fun createRoute(category: String? = null): String {
            return if (category != null) "gallery?category=$category" else "gallery"
        }
    }
    object Editor : Screen("editor?templateId={templateId}&artworkId={artworkId}") {
        fun createRoute(templateId: String? = null, artworkId: String? = null): String {
            val params = mutableListOf<String>()
            if (templateId != null) params.add("templateId=$templateId")
            if (artworkId != null) params.add("artworkId=$artworkId")
            return if (params.isNotEmpty()) "editor?${params.joinToString("&")}" else "editor"
        }
    }
    object Library : Screen("library")
    object Settings : Screen("settings")
    object Result : Screen("result?filePath={filePath}&isFallback={isFallback}") {
        fun createRoute(filePath: String?, isFallback: Boolean = false): String {
            val encodedPath = java.net.URLEncoder.encode(filePath ?: "", "UTF-8")
            return "result?filePath=$encodedPath&isFallback=$isFallback"
        }
    }
}
