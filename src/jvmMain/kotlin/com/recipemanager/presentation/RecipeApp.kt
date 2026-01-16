package com.recipemanager.presentation

import androidx.compose.runtime.*
import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.database.DatabaseManager
import com.recipemanager.data.repository.RecipeRepositoryImpl
import com.recipemanager.domain.validation.RecipeValidator
import com.recipemanager.presentation.screens.RecipeDetailScreen
import com.recipemanager.presentation.screens.RecipeFormScreen
import com.recipemanager.presentation.screens.RecipeListScreen
import com.recipemanager.presentation.viewmodel.RecipeDetailViewModel
import com.recipemanager.presentation.viewmodel.RecipeFormViewModel
import com.recipemanager.presentation.viewmodel.RecipeListViewModel

sealed class Screen {
    object RecipeList : Screen()
    data class RecipeDetail(val recipeId: String) : Screen()
    data class RecipeForm(val recipeId: String? = null) : Screen()
}

@Composable
fun RecipeApp() {
    // Initialize dependencies
    val databaseManager = remember {
        val driverFactory = DatabaseDriverFactory()
        val manager = DatabaseManager(driverFactory)
        manager.initialize()
        manager
    }
    
    val recipeRepository = remember {
        RecipeRepositoryImpl(databaseManager.getDatabase())
    }
    
    val recipeValidator = remember { RecipeValidator() }
    
    // Navigation state
    var currentScreen by remember { mutableStateOf<Screen>(Screen.RecipeList) }
    
    // ViewModels
    val recipeListViewModel = remember {
        RecipeListViewModel(recipeRepository)
    }
    
    val recipeDetailViewModel = remember {
        RecipeDetailViewModel(recipeRepository)
    }
    
    val recipeFormViewModel = remember {
        RecipeFormViewModel(recipeRepository, recipeValidator)
    }
    
    // Render current screen
    when (val screen = currentScreen) {
        is Screen.RecipeList -> {
            RecipeListScreen(
                viewModel = recipeListViewModel,
                onRecipeClick = { recipeId ->
                    currentScreen = Screen.RecipeDetail(recipeId)
                },
                onCreateRecipe = {
                    currentScreen = Screen.RecipeForm()
                }
            )
        }
        is Screen.RecipeDetail -> {
            RecipeDetailScreen(
                recipeId = screen.recipeId,
                viewModel = recipeDetailViewModel,
                onBack = {
                    currentScreen = Screen.RecipeList
                },
                onEdit = { recipeId ->
                    currentScreen = Screen.RecipeForm(recipeId)
                }
            )
        }
        is Screen.RecipeForm -> {
            RecipeFormScreen(
                recipeId = screen.recipeId,
                viewModel = recipeFormViewModel,
                onBack = {
                    currentScreen = Screen.RecipeList
                },
                onSaveSuccess = {
                    currentScreen = Screen.RecipeList
                    recipeListViewModel.loadRecipes()
                }
            )
        }
    }
}
