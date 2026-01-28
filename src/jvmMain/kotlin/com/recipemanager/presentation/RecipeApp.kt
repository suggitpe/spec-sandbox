package com.recipemanager.presentation

import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.database.DatabaseManager
import com.recipemanager.data.repository.RecipeRepositoryImpl
import com.recipemanager.domain.validation.RecipeValidator
import com.recipemanager.presentation.navigation.*
import kotlinx.coroutines.launch

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
    
    // Navigation and state management
    val navController = rememberNavController()
    val navigationStateManager = remember { NavigationStateManager() }
    val statePersistenceManager = remember { 
        AppStatePersistenceManager(JvmStatePersistence()) 
    }
    val deepLinkHandler = remember(navController) {
        DeepLinkHandler(navController, navigationStateManager)
    }
    val coroutineScope = rememberCoroutineScope()
    
    // State restoration
    var startDestination by remember { mutableStateOf(Routes.RECIPE_LIST) }
    var isStateRestored by remember { mutableStateOf(false) }
    
    // Restore navigation state on app start
    LaunchedEffect(Unit) {
        val savedState = statePersistenceManager.loadNavigationState()
        if (savedState != null) {
            startDestination = savedState.currentRoute
            // The navigation state manager will handle the restoration
        }
        isStateRestored = true
    }
    
    // Save navigation state when it changes
    LaunchedEffect(navigationStateManager.navigationState) {
        if (isStateRestored) {
            statePersistenceManager.saveNavigationState(navigationStateManager.navigationState)
        }
    }
    
    // Handle deep links
    LaunchedEffect(navController) {
        // This would be called when the app receives a deep link
        // For now, it's a placeholder for future deep link handling
    }
    
    // Only render navigation when state is restored
    if (isStateRestored) {
        RecipeNavHost(
            navController = navController,
            navigationStateManager = navigationStateManager,
            recipeRepository = recipeRepository,
            recipeValidator = recipeValidator,
            startDestination = startDestination
        )
    }
}
