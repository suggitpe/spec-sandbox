package com.recipemanager.domain.repository

import com.recipemanager.domain.model.RecipeSnapshot

/**
 * Repository interface for managing recipe snapshots.
 * Snapshots store complete recipe data at each version for rollback functionality.
 */
interface RecipeSnapshotRepository {
    /**
     * Creates a new recipe snapshot.
     */
    suspend fun createSnapshot(snapshot: RecipeSnapshot): Result<RecipeSnapshot>
    
    /**
     * Gets a snapshot by version ID.
     */
    suspend fun getSnapshotByVersionId(versionId: String): Result<RecipeSnapshot?>
    
    /**
     * Deletes a snapshot by version ID.
     */
    suspend fun deleteSnapshotByVersionId(versionId: String): Result<Unit>
    
    /**
     * Deletes all snapshots for a recipe (by recipe ID).
     */
    suspend fun deleteSnapshotsByRecipeId(recipeId: String): Result<Unit>
}
