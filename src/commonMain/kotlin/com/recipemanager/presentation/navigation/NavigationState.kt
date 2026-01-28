package com.recipemanager.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Represents the navigation state that can be persisted and restored.
 */
@Serializable
data class NavigationState(
    val currentRoute: String = Routes.RECIPE_LIST,
    val backStack: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Manages navigation state persistence and restoration.
 * Implements Requirements 7.1 (navigation structure) and 7.5 (state preservation).
 */
class NavigationStateManager {
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private var _navigationState by mutableStateOf(NavigationState())
    val navigationState: NavigationState get() = _navigationState
    
    private var navController: NavHostController? = null
    
    /**
     * Initialize the navigation state manager with a NavHostController.
     */
    fun initialize(controller: NavHostController) {
        navController = controller
        
        // Listen to navigation changes and update state
        controller.addOnDestinationChangedListener { _, destination, _ ->
            destination.route?.let { route ->
                updateNavigationState(route)
            }
        }
    }
    
    /**
     * Update the current navigation state.
     */
    private fun updateNavigationState(currentRoute: String) {
        // For now, we'll track the current route only
        // In a full implementation, you'd track the back stack properly
        _navigationState = NavigationState(
            currentRoute = currentRoute,
            backStack = emptyList(), // Simplified for now
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Serialize the current navigation state to JSON string.
     * Used for persistence across app sessions.
     */
    fun serializeState(): String {
        return json.encodeToString(_navigationState)
    }
    
    /**
     * Restore navigation state from JSON string.
     * Used when app is restored from background or restarted.
     */
    fun restoreState(serializedState: String): NavigationState? {
        return try {
            val state = json.decodeFromString<NavigationState>(serializedState)
            _navigationState = state
            state
        } catch (e: Exception) {
            // If deserialization fails, return null to use default state
            null
        }
    }
    
    /**
     * Navigate to a route and update state.
     */
    fun navigateTo(route: String) {
        navController?.navigate(route)
    }
    
    /**
     * Navigate back and update state.
     */
    fun navigateBack(): Boolean {
        return navController?.popBackStack() ?: false
    }
    
    /**
     * Clear the back stack and navigate to a new route.
     */
    fun navigateAndClearBackStack(route: String) {
        val controller = navController
        if (controller != null) {
            controller.navigate(route) {
                popUpTo(Routes.RECIPE_LIST) { inclusive = true }
            }
        }
    }
    
    /**
     * Handle deep link navigation.
     * Supports shared recipe deep links as per Requirements 7.1.
     */
    fun handleDeepLink(deepLink: String): Boolean {
        return when {
            deepLink.startsWith("recipemanager://recipe/") -> {
                val recipeId = deepLink.substringAfterLast("/")
                navigateTo(Routes.recipeDetail(recipeId))
                true
            }
            deepLink.startsWith("recipemanager://collection/") -> {
                val collectionId = deepLink.substringAfterLast("/")
                navigateTo(Routes.collectionDetail(collectionId))
                true
            }
            else -> false
        }
    }
    
    /**
     * Get the current route for state preservation.
     */
    fun getCurrentRoute(): String = _navigationState.currentRoute
    
    /**
     * Check if we can navigate back.
     */
    fun canNavigateBack(): Boolean {
        return navController?.previousBackStackEntry != null
    }
}