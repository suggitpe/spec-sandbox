package com.recipemanager.domain.service

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.model.RecipeVersion
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.domain.repository.RecipeVersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Manages recipe versioning, upgrades, and version history.
 * 
 * Responsibilities:
 * - Create upgraded versions of recipes with parent linking
 * - Track version history with upgrade notes
 * - Revert recipes to previous versions
 * - Query version history
 */
class RecipeVersionManager(
    private val recipeRepository: RecipeRepository,
    private val versionRepository: RecipeVersionRepository
) {
    
    /**
     * Generates a simple UUID-like string for IDs.
     */
    private fun generateId(): String {
        return "${Random.nextLong()}-${Clock.System.now().toEpochMilliseconds()}"
    }
    
    /**
     * Creates an upgraded version of a recipe.
     * 
     * The new recipe will:
     * - Have a new unique ID
     * - Link to the parent recipe via parentRecipeId
     * - Increment the version number
     * - Record upgrade notes in version history
     * 
     * @param parentRecipe The recipe to upgrade
     * @param upgradeNotes Notes explaining the changes made
     * @param modifications Lambda to apply modifications to the recipe
     * @return Result containing the new upgraded recipe
     */
    suspend fun upgradeRecipe(
        parentRecipe: Recipe,
        upgradeNotes: String?,
        modifications: (Recipe) -> Recipe
    ): Result<Recipe> = withContext(Dispatchers.Default) {
        try {
            val now = Clock.System.now()
            val newVersion = parentRecipe.version + 1
            
            // Create the upgraded recipe with modifications
            val upgradedRecipe = modifications(parentRecipe).copy(
                id = generateId(),
                parentRecipeId = parentRecipe.id,
                version = newVersion,
                createdAt = now,
                updatedAt = now
            )
            
            // Save the upgraded recipe
            val createResult = recipeRepository.createRecipe(upgradedRecipe)
            if (createResult.isFailure) {
                return@withContext createResult
            }
            
            // Record version history
            val versionEntry = RecipeVersion(
                id = generateId(),
                recipeId = upgradedRecipe.id,
                version = newVersion,
                parentRecipeId = parentRecipe.id,
                upgradeNotes = upgradeNotes,
                createdAt = now
            )
            
            val versionResult = versionRepository.createVersion(versionEntry)
            if (versionResult.isFailure) {
                // Rollback: delete the created recipe
                recipeRepository.deleteRecipe(upgradedRecipe.id)
                return@withContext Result.failure(
                    Exception("Failed to record version history", versionResult.exceptionOrNull())
                )
            }
            
            Result.success(upgradedRecipe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets the complete version history for a recipe.
     * 
     * @param recipeId The recipe ID
     * @return Result containing list of version entries, ordered by version descending
     */
    suspend fun getVersionHistory(recipeId: String): Result<List<RecipeVersion>> {
        return versionRepository.getVersionsByRecipeId(recipeId)
    }
    
    /**
     * Gets all versions in a recipe family (parent and all descendants).
     * 
     * @param recipeId Any recipe ID in the family
     * @return Result containing list of all version entries in the family
     */
    suspend fun getRecipeFamilyHistory(recipeId: String): Result<List<RecipeVersion>> {
        return versionRepository.getRecipeFamilyVersions(recipeId)
    }
    
    /**
     * Reverts a recipe to a previous version by creating a new recipe
     * with the data from the specified version.
     * 
     * This creates a new recipe (with new ID) that is a copy of the target version,
     * maintaining the version chain.
     * 
     * @param currentRecipeId The current recipe ID
     * @param targetVersion The version number to revert to
     * @return Result containing the reverted recipe (as a new recipe)
     */
    suspend fun revertToVersion(
        currentRecipeId: String,
        targetVersion: Int
    ): Result<Recipe> = withContext(Dispatchers.Default) {
        try {
            // Get the current recipe to find the version chain
            val currentRecipeResult = recipeRepository.getRecipe(currentRecipeId)
            if (currentRecipeResult.isFailure) {
                return@withContext Result.failure(
                    Exception("Failed to get current recipe", currentRecipeResult.exceptionOrNull())
                )
            }
            
            val currentRecipe = currentRecipeResult.getOrNull()
                ?: return@withContext Result.failure(Exception("Recipe not found: $currentRecipeId"))
            
            // Find the target version in history
            val versionHistoryResult = getVersionHistory(currentRecipeId)
            if (versionHistoryResult.isFailure) {
                return@withContext Result.failure(
                    Exception("Failed to get version history", versionHistoryResult.exceptionOrNull())
                )
            }
            
            val targetVersionEntry = versionHistoryResult.getOrNull()
                ?.find { it.version == targetVersion }
                ?: return@withContext Result.failure(
                    Exception("Version $targetVersion not found for recipe $currentRecipeId")
                )
            
            // Get the recipe data from the target version
            // Note: In a full implementation, we would store complete recipe snapshots
            // For now, we'll traverse the parent chain to reconstruct the recipe
            val targetRecipeResult = if (targetVersionEntry.parentRecipeId != null) {
                recipeRepository.getRecipe(targetVersionEntry.parentRecipeId)
            } else {
                recipeRepository.getRecipe(currentRecipeId)
            }
            
            if (targetRecipeResult.isFailure) {
                return@withContext Result.failure(
                    Exception("Failed to get target version recipe", targetRecipeResult.exceptionOrNull())
                )
            }
            
            val targetRecipe = targetRecipeResult.getOrNull()
                ?: return@withContext Result.failure(
                    Exception("Target version recipe not found")
                )
            
            // Create a new recipe as a reversion
            val now = Clock.System.now()
            val revertedRecipe = targetRecipe.copy(
                id = generateId(),
                parentRecipeId = currentRecipeId,
                version = currentRecipe.version + 1,
                createdAt = now,
                updatedAt = now
            )
            
            // Save the reverted recipe
            val createResult = recipeRepository.createRecipe(revertedRecipe)
            if (createResult.isFailure) {
                return@withContext createResult
            }
            
            // Record version history
            val versionEntry = RecipeVersion(
                id = generateId(),
                recipeId = revertedRecipe.id,
                version = revertedRecipe.version,
                parentRecipeId = currentRecipeId,
                upgradeNotes = "Reverted to version $targetVersion",
                createdAt = now
            )
            
            versionRepository.createVersion(versionEntry)
            
            Result.success(revertedRecipe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets the parent recipe of a given recipe.
     * 
     * @param recipe The recipe to get the parent for
     * @return Result containing the parent recipe, or null if no parent exists
     */
    suspend fun getParentRecipe(recipe: Recipe): Result<Recipe?> {
        if (recipe.parentRecipeId == null) {
            return Result.success(null)
        }
        return recipeRepository.getRecipe(recipe.parentRecipeId)
    }
    
    /**
     * Gets all child recipes (upgrades) of a given recipe.
     * 
     * @param recipeId The parent recipe ID
     * @return Result containing list of child recipes
     */
    suspend fun getChildRecipes(recipeId: String): Result<List<Recipe>> = withContext(Dispatchers.Default) {
        try {
            // Get all recipes and filter for children
            val allRecipesResult = recipeRepository.getAllRecipes()
            if (allRecipesResult.isFailure) {
                return@withContext Result.failure(
                    Exception("Failed to get recipes", allRecipesResult.exceptionOrNull())
                )
            }
            
            val children = allRecipesResult.getOrNull()
                ?.filter { it.parentRecipeId == recipeId }
                ?: emptyList()
            
            Result.success(children)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
