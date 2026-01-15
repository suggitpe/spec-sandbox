package com.recipemanager.domain.repository

import com.recipemanager.domain.model.RecipeVersion

/**
 * Repository interface for managing recipe version history.
 */
interface RecipeVersionRepository {
    /**
     * Creates a new version entry in the history.
     */
    suspend fun createVersion(version: RecipeVersion): Result<RecipeVersion>
    
    /**
     * Gets all version entries for a specific recipe.
     * @return List ordered by version descending (newest first)
     */
    suspend fun getVersionsByRecipeId(recipeId: String): Result<List<RecipeVersion>>
    
    /**
     * Gets a specific version entry by ID.
     */
    suspend fun getVersionById(id: String): Result<RecipeVersion?>
    
    /**
     * Gets a specific version entry by recipe ID and version number.
     */
    suspend fun getVersionByRecipeIdAndVersion(recipeId: String, version: Int): Result<RecipeVersion?>
    
    /**
     * Gets all version entries for a recipe family (parent and all descendants).
     */
    suspend fun getRecipeFamilyVersions(recipeId: String): Result<List<RecipeVersion>>
    
    /**
     * Deletes all version entries for a recipe.
     */
    suspend fun deleteVersionsByRecipeId(recipeId: String): Result<Unit>
}
