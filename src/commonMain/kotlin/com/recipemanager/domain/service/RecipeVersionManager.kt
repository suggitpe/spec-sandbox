package com.recipemanager.domain.service

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.model.RecipeSnapshot
import com.recipemanager.domain.model.RecipeVersion
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.domain.repository.RecipeSnapshotRepository
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
 * - Store complete recipe snapshots for each version
 * - Revert recipes to previous versions with complete data rollback
 * - Query version history
 */
class RecipeVersionManager(
    private val recipeRepository: RecipeRepository,
    private val versionRepository: RecipeVersionRepository,
    private val snapshotRepository: RecipeSnapshotRepository
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
     * - Store a complete snapshot for rollback capability
     * - Generate new IDs for all nested objects (ingredients, steps, photos)
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
            
            // Apply modifications to the parent recipe
            val modifiedRecipe = modifications(parentRecipe)
            
            // Create the upgraded recipe with new IDs for all nested objects
            val upgradedRecipe = modifiedRecipe.copy(
                id = generateId(),
                parentRecipeId = parentRecipe.id,
                version = newVersion,
                createdAt = now,
                updatedAt = now,
                // Generate new IDs for ingredients to avoid conflicts
                ingredients = modifiedRecipe.ingredients.map { ingredient ->
                    ingredient.copy(
                        id = generateId(),
                        photos = ingredient.photos.map { photo ->
                            photo.copy(id = generateId())
                        }
                    )
                },
                // Generate new IDs for steps to avoid conflicts
                steps = modifiedRecipe.steps.map { step ->
                    step.copy(
                        id = generateId(),
                        photos = step.photos.map { photo ->
                            photo.copy(id = generateId())
                        }
                    )
                }
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
            
            // Store complete recipe snapshot for rollback
            val snapshot = RecipeSnapshot(
                id = generateId(),
                versionId = versionEntry.id,
                recipe = upgradedRecipe,
                createdAt = now
            )
            
            val snapshotResult = snapshotRepository.createSnapshot(snapshot)
            if (snapshotResult.isFailure) {
                // Rollback: delete version and recipe
                versionRepository.deleteVersionsByRecipeId(upgradedRecipe.id)
                recipeRepository.deleteRecipe(upgradedRecipe.id)
                return@withContext Result.failure(
                    Exception("Failed to store recipe snapshot", snapshotResult.exceptionOrNull())
                )
            }
            
            Result.success(upgradedRecipe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Creates an initial version entry and snapshot for a newly created recipe.
     * This should be called after a recipe is first created to enable version tracking.
     * 
     * @param recipe The newly created recipe
     * @return Result indicating success or failure
     */
    suspend fun createInitialVersion(recipe: Recipe): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val now = Clock.System.now()
            
            // Create version entry for the initial recipe
            val versionEntry = RecipeVersion(
                id = generateId(),
                recipeId = recipe.id,
                version = recipe.version,
                parentRecipeId = recipe.parentRecipeId,
                upgradeNotes = "Initial version",
                createdAt = now
            )
            
            val versionResult = versionRepository.createVersion(versionEntry)
            if (versionResult.isFailure) {
                return@withContext Result.failure(
                    Exception("Failed to create initial version entry", versionResult.exceptionOrNull())
                )
            }
            
            // Create snapshot for the initial recipe
            val snapshot = RecipeSnapshot(
                id = generateId(),
                versionId = versionEntry.id,
                recipe = recipe,
                createdAt = now
            )
            
            val snapshotResult = snapshotRepository.createSnapshot(snapshot)
            if (snapshotResult.isFailure) {
                // Rollback: delete the version entry
                versionRepository.deleteVersionsByRecipeId(recipe.id)
                return@withContext Result.failure(
                    Exception("Failed to create initial snapshot", snapshotResult.exceptionOrNull())
                )
            }
            
            Result.success(Unit)
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
     * Reverts a recipe to a previous version by restoring the complete
     * recipe data from the snapshot of that version.
     * 
     * This creates a new recipe (with new ID) that contains the exact data
     * from the target version snapshot, maintaining the version chain.
     * 
     * @param currentRecipeId The current recipe ID
     * @param targetVersion The version number to revert to
     * @return Result containing the reverted recipe (as a new recipe with restored data)
     */
    suspend fun revertToVersion(
        currentRecipeId: String,
        targetVersion: Int
    ): Result<Recipe> = withContext(Dispatchers.Default) {
        try {
            // Get the current recipe to validate and get version info
            val currentRecipeResult = recipeRepository.getRecipe(currentRecipeId)
            if (currentRecipeResult.isFailure) {
                return@withContext Result.failure(
                    Exception("Failed to get current recipe", currentRecipeResult.exceptionOrNull())
                )
            }
            
            val currentRecipe = currentRecipeResult.getOrNull()
                ?: return@withContext Result.failure(Exception("Recipe not found: $currentRecipeId"))
            
            // Validate that we're not trying to revert to the current version
            if (currentRecipe.version == targetVersion) {
                return@withContext Result.failure(
                    Exception("Cannot revert to current version $targetVersion")
                )
            }
            
            // Get the family history to find the target version across the entire chain
            val familyHistoryResult = getRecipeFamilyHistory(currentRecipeId)
            if (familyHistoryResult.isFailure) {
                return@withContext Result.failure(
                    Exception("Failed to get recipe family history", familyHistoryResult.exceptionOrNull())
                )
            }
            
            val targetVersionEntry = familyHistoryResult.getOrNull()
                ?.find { it.version == targetVersion }
                ?: return@withContext Result.failure(
                    Exception("Version $targetVersion not found in recipe family")
                )
            
            // Get the complete recipe snapshot from the target version
            val snapshotResult = snapshotRepository.getSnapshotByVersionId(targetVersionEntry.id)
            if (snapshotResult.isFailure) {
                return@withContext Result.failure(
                    Exception("Failed to get snapshot for version $targetVersion", snapshotResult.exceptionOrNull())
                )
            }
            
            val snapshot = snapshotResult.getOrNull()
                ?: return@withContext Result.failure(
                    Exception("No snapshot found for version $targetVersion. Cannot perform complete rollback.")
                )
            
            // Create a new recipe with the snapshot data but new metadata and new IDs for nested objects
            val now = Clock.System.now()
            val revertedRecipe = snapshot.recipe.copy(
                id = generateId(),
                parentRecipeId = currentRecipeId,
                version = currentRecipe.version + 1,
                createdAt = now,
                updatedAt = now,
                // Generate new IDs for ingredients to avoid conflicts
                ingredients = snapshot.recipe.ingredients.map { ingredient ->
                    ingredient.copy(
                        id = generateId(),
                        photos = ingredient.photos.map { photo ->
                            photo.copy(id = generateId())
                        }
                    )
                },
                // Generate new IDs for steps to avoid conflicts
                steps = snapshot.recipe.steps.map { step ->
                    step.copy(
                        id = generateId(),
                        photos = step.photos.map { photo ->
                            photo.copy(id = generateId())
                        }
                    )
                }
            )
            
            // Save the reverted recipe
            val createResult = recipeRepository.createRecipe(revertedRecipe)
            if (createResult.isFailure) {
                return@withContext createResult
            }
            
            // Record version history for the reversion
            val versionEntry = RecipeVersion(
                id = generateId(),
                recipeId = revertedRecipe.id,
                version = revertedRecipe.version,
                parentRecipeId = currentRecipeId,
                upgradeNotes = "Reverted to version $targetVersion",
                createdAt = now
            )
            
            val versionResult = versionRepository.createVersion(versionEntry)
            if (versionResult.isFailure) {
                // Rollback: delete the created recipe
                recipeRepository.deleteRecipe(revertedRecipe.id)
                return@withContext Result.failure(
                    Exception("Failed to record version history", versionResult.exceptionOrNull())
                )
            }
            
            // Store snapshot of the reverted recipe
            val revertedSnapshot = RecipeSnapshot(
                id = generateId(),
                versionId = versionEntry.id,
                recipe = revertedRecipe,
                createdAt = now
            )
            
            val snapshotCreateResult = snapshotRepository.createSnapshot(revertedSnapshot)
            if (snapshotCreateResult.isFailure) {
                // Rollback: delete version and recipe
                versionRepository.deleteVersionsByRecipeId(revertedRecipe.id)
                recipeRepository.deleteRecipe(revertedRecipe.id)
                return@withContext Result.failure(
                    Exception("Failed to store snapshot", snapshotCreateResult.exceptionOrNull())
                )
            }
            
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
