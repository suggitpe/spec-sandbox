package com.recipemanager.presentation.navigation

import androidx.navigation.NavHostController

/**
 * Handles deep link navigation for shared recipes and collections.
 * Implements Requirements 7.1 (deep linking support for shared recipes).
 */
class DeepLinkHandler(
    private val navController: NavHostController,
    private val navigationStateManager: NavigationStateManager
) {
    
    /**
     * Process a deep link and navigate to the appropriate screen.
     * Returns true if the deep link was handled, false otherwise.
     */
    fun handleDeepLink(deepLink: String): Boolean {
        return when {
            // Handle shared recipe deep links
            deepLink.matches(Regex("recipemanager://recipe/[a-zA-Z0-9-]+")) -> {
                val recipeId = extractRecipeId(deepLink)
                if (recipeId != null) {
                    navigateToRecipe(recipeId)
                    true
                } else {
                    false
                }
            }
            
            // Handle shared collection deep links
            deepLink.matches(Regex("recipemanager://collection/[a-zA-Z0-9-]+")) -> {
                val collectionId = extractCollectionId(deepLink)
                if (collectionId != null) {
                    navigateToCollection(collectionId)
                    true
                } else {
                    false
                }
            }
            
            // Handle HTTP/HTTPS deep links (for web sharing)
            deepLink.startsWith("https://recipemanager.app/recipe/") -> {
                val recipeId = deepLink.substringAfterLast("/")
                if (recipeId.isNotBlank()) {
                    navigateToRecipe(recipeId)
                    true
                } else {
                    false
                }
            }
            
            deepLink.startsWith("https://recipemanager.app/collection/") -> {
                val collectionId = deepLink.substringAfterLast("/")
                if (collectionId.isNotBlank()) {
                    navigateToCollection(collectionId)
                    true
                } else {
                    false
                }
            }
            
            else -> false
        }
    }
    
    /**
     * Extract recipe ID from a deep link.
     */
    private fun extractRecipeId(deepLink: String): String? {
        return try {
            when {
                deepLink.startsWith("recipemanager://recipe/") -> {
                    deepLink.substringAfter("recipemanager://recipe/")
                }
                deepLink.startsWith("https://recipemanager.app/recipe/") -> {
                    deepLink.substringAfter("https://recipemanager.app/recipe/")
                }
                else -> null
            }?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract collection ID from a deep link.
     */
    private fun extractCollectionId(deepLink: String): String? {
        return try {
            when {
                deepLink.startsWith("recipemanager://collection/") -> {
                    deepLink.substringAfter("recipemanager://collection/")
                }
                deepLink.startsWith("https://recipemanager.app/collection/") -> {
                    deepLink.substringAfter("https://recipemanager.app/collection/")
                }
                else -> null
            }?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Navigate to a recipe detail screen.
     */
    private fun navigateToRecipe(recipeId: String) {
        navController.navigate(Routes.recipeDetail(recipeId)) {
            // Clear back stack to prevent navigation issues
            popUpTo(Routes.RECIPE_LIST) {
                inclusive = false
            }
        }
    }
    
    /**
     * Navigate to a collection detail screen.
     */
    private fun navigateToCollection(collectionId: String) {
        navController.navigate(Routes.collectionDetail(collectionId)) {
            // Clear back stack to prevent navigation issues
            popUpTo(Routes.RECIPE_LIST) {
                inclusive = false
            }
        }
    }
    
    /**
     * Generate a shareable deep link for a recipe.
     */
    fun generateRecipeDeepLink(recipeId: String): String {
        return "recipemanager://recipe/$recipeId"
    }
    
    /**
     * Generate a shareable deep link for a collection.
     */
    fun generateCollectionDeepLink(collectionId: String): String {
        return "recipemanager://collection/$collectionId"
    }
    
    /**
     * Generate a web-shareable link for a recipe.
     */
    fun generateRecipeWebLink(recipeId: String): String {
        return "https://recipemanager.app/recipe/$recipeId"
    }
    
    /**
     * Generate a web-shareable link for a collection.
     */
    fun generateCollectionWebLink(collectionId: String): String {
        return "https://recipemanager.app/collection/$collectionId"
    }
}