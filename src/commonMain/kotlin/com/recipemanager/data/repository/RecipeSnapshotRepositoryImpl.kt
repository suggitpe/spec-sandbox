package com.recipemanager.data.repository

import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.model.RecipeSnapshot
import com.recipemanager.domain.repository.RecipeSnapshotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RecipeSnapshotRepositoryImpl(
    private val database: RecipeDatabase
) : RecipeSnapshotRepository {
    
    private val json = Json { 
        prettyPrint = false
        ignoreUnknownKeys = true
    }
    
    override suspend fun createSnapshot(snapshot: RecipeSnapshot): Result<RecipeSnapshot> = withContext(Dispatchers.Default) {
        try {
            val recipeJson = json.encodeToString(snapshot.recipe)
            
            database.recipeSnapshotQueries.insertSnapshot(
                id = snapshot.id,
                versionId = snapshot.versionId,
                recipeData = recipeJson,
                createdAt = snapshot.createdAt.epochSeconds
            )
            
            Result.success(snapshot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getSnapshotByVersionId(versionId: String): Result<RecipeSnapshot?> = withContext(Dispatchers.Default) {
        try {
            val row = database.recipeSnapshotQueries.selectSnapshotByVersionId(versionId)
                .executeAsOneOrNull()
            
            val snapshot = row?.let {
                val recipe = json.decodeFromString<Recipe>(it.recipeData)
                RecipeSnapshot(
                    id = it.id,
                    versionId = it.versionId,
                    recipe = recipe,
                    createdAt = Instant.fromEpochSeconds(it.createdAt)
                )
            }
            
            Result.success(snapshot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteSnapshotByVersionId(versionId: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.recipeSnapshotQueries.deleteSnapshotByVersionId(versionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteSnapshotsByRecipeId(recipeId: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.recipeSnapshotQueries.deleteSnapshotsByRecipeVersionIds(recipeId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
