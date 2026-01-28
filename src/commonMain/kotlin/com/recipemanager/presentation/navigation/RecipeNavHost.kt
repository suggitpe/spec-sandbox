package com.recipemanager.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.recipemanager.data.repository.RecipeRepositoryImpl
import com.recipemanager.domain.validation.RecipeValidator
import com.recipemanager.presentation.screens.*
import com.recipemanager.presentation.viewmodel.*

/**
 * Main navigation host for the Recipe Manager application.
 * Implements Requirements 7.1 (navigation structure) and 7.5 (state preservation).
 */
@Composable
fun RecipeNavHost(
    navController: NavHostController = rememberNavController(),
    navigationStateManager: NavigationStateManager,
    recipeRepository: RecipeRepositoryImpl,
    recipeValidator: RecipeValidator,
    startDestination: String = Routes.RECIPE_LIST
) {
    // Initialize navigation state manager
    LaunchedEffect(navController) {
        navigationStateManager.initialize(navController)
    }
    
    // Create ViewModels - these will be recreated on each recomposition
    // In a production app, you'd want to use a DI framework or ViewModel factory
    val recipeListViewModel = remember {
        RecipeListViewModel(recipeRepository)
    }
    
    val recipeDetailViewModel = remember {
        RecipeDetailViewModel(recipeRepository)
    }
    
    val recipeFormViewModel = remember {
        RecipeFormViewModel(recipeRepository, recipeValidator)
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Recipe List Screen
        composable(Routes.RECIPE_LIST) {
            RecipeListScreen(
                viewModel = recipeListViewModel,
                onRecipeClick = { recipeId ->
                    navController.navigate(Routes.recipeDetail(recipeId))
                },
                onCreateRecipe = {
                    navController.navigate(Routes.recipeForm())
                }
            )
        }
        
        // Recipe Detail Screen
        composable(Routes.RECIPE_DETAIL) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
            
            RecipeDetailScreen(
                recipeId = recipeId,
                viewModel = recipeDetailViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onEdit = { editRecipeId ->
                    navController.navigate(Routes.recipeForm(editRecipeId))
                }
            )
        }
        
        // Recipe Form Screen (Create/Edit)
        composable(Routes.RECIPE_FORM) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId")
            
            RecipeFormScreen(
                recipeId = recipeId,
                viewModel = recipeFormViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                    // Refresh the recipe list
                    recipeListViewModel.loadRecipes()
                }
            )
        }
    }
}