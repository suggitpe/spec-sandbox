package com.recipemanager.domain.usecase

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.service.PlatformShareService
import com.recipemanager.domain.service.ShareChannel
import com.recipemanager.domain.service.ShareService

/**
 * Use case for sharing recipes through various channels.
 * Combines recipe serialization with platform-specific sharing capabilities.
 */
class ShareRecipeUseCase(
    private val shareService: ShareService,
    private val platformShareService: PlatformShareService
) {
    /**
     * Shares a recipe through the specified channel.
     * 
     * @param recipe The recipe to share
     * @param channel The sharing channel to use
     * @return Result indicating success or failure
     */
    suspend fun shareRecipe(recipe: Recipe, channel: ShareChannel): Result<Unit> {
        // Export recipe to JSON
        val exportResult = shareService.exportRecipe(recipe)
        if (exportResult.isFailure) {
            return Result.failure(exportResult.exceptionOrNull()!!)
        }
        
        val jsonData = exportResult.getOrThrow()
        
        // Share through platform-specific service
        return platformShareService.shareRecipe(
            data = jsonData,
            channel = channel,
            title = recipe.title
        )
    }

    /**
     * Shares multiple recipes through the specified channel.
     * 
     * @param recipes The list of recipes to share
     * @param channel The sharing channel to use
     * @param title Optional title for the share action
     * @return Result indicating success or failure
     */
    suspend fun shareRecipes(
        recipes: List<Recipe>,
        channel: ShareChannel,
        title: String? = null
    ): Result<Unit> {
        // Export recipes to JSON
        val exportResult = shareService.exportRecipes(recipes)
        if (exportResult.isFailure) {
            return Result.failure(exportResult.exceptionOrNull()!!)
        }
        
        val jsonData = exportResult.getOrThrow()
        
        // Share through platform-specific service
        return platformShareService.shareRecipe(
            data = jsonData,
            channel = channel,
            title = title ?: "recipes"
        )
    }

    /**
     * Gets the list of available sharing channels on the current platform.
     * 
     * @return List of available sharing channels
     */
    fun getAvailableChannels(): List<ShareChannel> {
        return platformShareService.getAvailableChannels()
    }
}
