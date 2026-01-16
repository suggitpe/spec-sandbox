package com.recipemanager

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.recipemanager.presentation.RecipeApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Recipe Manager"
    ) {
        RecipeApp()
    }
}
