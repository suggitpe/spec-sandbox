package com.recipemanager.domain.usecase

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.domain.service.RecipeCopyManager
import com.recipemanager.domain.service.ShareService
import com.recipemanager.domain.service.SharedRecipeMetadata
import kotlinx.datetime.Clock

/**
 * Use case for importing recipes from shared data.
 * Handles recipe deserialization, validation, and storage.
 */
class ImportRecipeUseCase(
    private val shareService: ShareService,
    private val recipeRepository: RecipeRepository,
    private val recipeCopyManager: RecipeCopyManager
) {
    /**
     * Imports a recipe from JSON data and saves it to the repository.
     * The imported recipe is assigned a new ID and timestamps to ensure independence.
     * 
     * @param jsonData The JSON string containing recipe data
     * @param sharedBy Optional identifier of who shared the recipe
     * @param shareNote Optional note about the sharing context
     * @return Result containing the imported recipe or an error
     */
    suspend fun importRecipe(
        jsonData: String,
        sharedBy: String? = null,
        shareNote: String? = null
    ): Result<Recipe> {
        // Import and validate recipe
        val importResult = shareService.importRecipe(jsonData)
        if (importResult.isFailure) {
            return Result.failure(importResult.exceptionOrNull()!!)
        }
        
        val importedRecipe = importResult.getOrThrow()
        
        // Create metadata for the shared recipe
        val metadata = SharedRecipeMetadata(
            originalRecipeId = importedRecipe.id,
            sharedAt = Clock.System.now(),
            sharedBy = sharedBy,
            shareNote = shareNote
        )
        
        // Create an independent copy using RecipeCopyManager
        val independentCopy = recipeCopyManager.createIndependentCopy(
            original = importedRecipe,
            metadata = metadata
        )
        
        // Save to repository
        return recipeRepository.createRecipe(independentCopy)
    }

    /**
     * Imports multiple recipes from JSON data and saves them to the repository.
     * Each imported recipe is assigned a new ID and timestamps to ensure independence.
     * 
     * @param jsonData The JSON string containing recipe data
     * @param sharedBy Optional identifier of who shared the recipes
     * @return Result containing the list of imported recipes or an error
     */
    suspend fun importRecipes(
        jsonData: String,
        sharedBy: String? = null
    ): Result<List<Recipe>> {
        // Import and validate recipes
        val importResult = shareService.importRecipes(jsonData)
        if (importResult.isFailure) {
            return Result.failure(importResult.exceptionOrNull()!!)
        }
        
        val importedRecipes = importResult.getOrThrow()
        val now = Clock.System.now()
        val savedRecipes = mutableListOf<Recipe>()
        
        // Create independent copies with new IDs and timestamps
        for (importedRecipe in importedRecipes) {
            val metadata = SharedRecipeMetadata(
                originalRecipeId = importedRecipe.id,
                sharedAt = now,
                sharedBy = sharedBy
            )
            
            val independentCopy = recipeCopyManager.createIndependentCopy(
                original = importedRecipe,
                metadata = metadata
            )
            
            val saveResult = recipeRepository.createRecipe(independentCopy)
            if (saveResult.isFailure) {
                return Result.failure(saveResult.exceptionOrNull()!!)
            }
            
            savedRecipes.add(saveResult.getOrThrow())
        }
        
        return Result.success(savedRecipes)
    }
}
