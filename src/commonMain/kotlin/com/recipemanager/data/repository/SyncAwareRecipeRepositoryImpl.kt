package com.recipemanager.data.repository

import com.recipemanager.data.cloud.CloudSyncManager
import com.recipemanager.data.cloud.SyncEntityType
import com.recipemanager.data.cloud.SyncOperation
import com.recipemanager.data.cloud.SyncOperationType
import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Recipe repository implementation with cloud synchronization support
 */
class SyncAwareRecipeRepositoryImpl(
    private val database: RecipeDatabase,
    private val syncManager: CloudSyncManager
) : RecipeRepository {
    
    override suspend fun createRecipe(recipe: Recipe): Result<Recipe> {
        return try {
            // Save to local database first
            database.recipeQueries.insertRecipe(
                id = recipe.id,
                title = recipe.title,
                description = recipe.description,
                preparationTime = recipe.preparationTime.toLong(),
                cookingTime = recipe.cookingTime.toLong(),
                servings = recipe.servings.toLong(),
                tags = Json.encodeToString(recipe.tags),
                createdAt = recipe.createdAt.epochSeconds,
                updatedAt = recipe.updatedAt.epochSeconds,
                version = recipe.version.toLong(),
                parentRecipeId = recipe.parentRecipeId
            )
            
            // Queue for cloud sync
            val syncOperation = SyncOperation(
                type = SyncOperationType.CREATE,
                entityType = SyncEntityType.RECIPE,
                entityId = recipe.id,
                data = Json.encodeToString(recipe)
            )
            syncManager.queueOperation(syncOperation)
            
            Result.success(recipe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateRecipe(recipe: Recipe): Result<Recipe> {
        return try {
            val updatedRecipe = recipe.copy(
                updatedAt = Clock.System.now(),
                version = recipe.version + 1
            )
            
            // Update local database
            database.recipeQueries.updateRecipe(
                title = updatedRecipe.title,
                description = updatedRecipe.description,
                preparationTime = updatedRecipe.preparationTime.toLong(),
                cookingTime = updatedRecipe.cookingTime.toLong(),
                servings = updatedRecipe.servings.toLong(),
                tags = Json.encodeToString(updatedRecipe.tags),
                updatedAt = updatedRecipe.updatedAt.epochSeconds,
                version = updatedRecipe.version.toLong(),
                parentRecipeId = updatedRecipe.parentRecipeId,
                id = updatedRecipe.id
            )
            
            // Queue for cloud sync
            val syncOperation = SyncOperation(
                type = SyncOperationType.UPDATE,
                entityType = SyncEntityType.RECIPE,
                entityId = updatedRecipe.id,
                data = Json.encodeToString(updatedRecipe)
            )
            syncManager.queueOperation(syncOperation)
            
            Result.success(updatedRecipe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteRecipe(id: String): Result<Unit> {
        return try {
            // Delete from local database
            database.recipeQueries.deleteRecipe(id)
            
            // Queue for cloud sync
            val syncOperation = SyncOperation(
                type = SyncOperationType.DELETE,
                entityType = SyncEntityType.RECIPE,
                entityId = id,
                data = "" // No data needed for delete
            )
            syncManager.queueOperation(syncOperation)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getRecipe(id: String): Result<Recipe?> {
        return try {
            val recipeRow = database.recipeQueries.selectRecipeById(id).executeAsOneOrNull()
            val recipe = recipeRow?.let { row ->
                Recipe(
                    id = row.id,
                    title = row.title,
                    description = row.description,
                    ingredients = emptyList(), // Would need to fetch from ingredients table
                    steps = emptyList(), // Would need to fetch from steps table
                    preparationTime = row.preparationTime.toInt(),
                    cookingTime = row.cookingTime.toInt(),
                    servings = row.servings.toInt(),
                    tags = Json.decodeFromString(row.tags),
                    createdAt = kotlinx.datetime.Instant.fromEpochSeconds(row.createdAt),
                    updatedAt = kotlinx.datetime.Instant.fromEpochSeconds(row.updatedAt),
                    version = row.version.toInt(),
                    parentRecipeId = row.parentRecipeId
                )
            }
            Result.success(recipe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAllRecipes(): Result<List<Recipe>> {
        return try {
            val recipes = database.recipeQueries.selectAllRecipes().executeAsList().map { row ->
                Recipe(
                    id = row.id,
                    title = row.title,
                    description = row.description,
                    ingredients = emptyList(), // Would need to fetch from ingredients table
                    steps = emptyList(), // Would need to fetch from steps table
                    preparationTime = row.preparationTime.toInt(),
                    cookingTime = row.cookingTime.toInt(),
                    servings = row.servings.toInt(),
                    tags = Json.decodeFromString(row.tags),
                    createdAt = kotlinx.datetime.Instant.fromEpochSeconds(row.createdAt),
                    updatedAt = kotlinx.datetime.Instant.fromEpochSeconds(row.updatedAt),
                    version = row.version.toInt(),
                    parentRecipeId = row.parentRecipeId
                )
            }
            Result.success(recipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun observeRecipes(): Flow<List<Recipe>> {
        return flow {
            try {
                val recipes = database.recipeQueries.selectAllRecipes().executeAsList().map { row ->
                    Recipe(
                        id = row.id,
                        title = row.title,
                        description = row.description,
                        ingredients = emptyList(), // Would need to fetch from ingredients table
                        steps = emptyList(), // Would need to fetch from steps table
                        preparationTime = row.preparationTime.toInt(),
                        cookingTime = row.cookingTime.toInt(),
                        servings = row.servings.toInt(),
                        tags = Json.decodeFromString(row.tags),
                        createdAt = kotlinx.datetime.Instant.fromEpochSeconds(row.createdAt),
                        updatedAt = kotlinx.datetime.Instant.fromEpochSeconds(row.updatedAt),
                        version = row.version.toInt(),
                        parentRecipeId = row.parentRecipeId
                    )
                }
                emit(recipes)
            } catch (e: Exception) {
                emit(emptyList())
            }
        }
    }
    
    override suspend fun searchRecipes(query: String): Result<List<Recipe>> {
        return try {
            val recipes = database.recipeQueries.searchRecipes(query, query, query).executeAsList().map { row ->
                Recipe(
                    id = row.id,
                    title = row.title,
                    description = row.description,
                    ingredients = emptyList(), // Would need to fetch from ingredients table
                    steps = emptyList(), // Would need to fetch from steps table
                    preparationTime = row.preparationTime.toInt(),
                    cookingTime = row.cookingTime.toInt(),
                    servings = row.servings.toInt(),
                    tags = Json.decodeFromString(row.tags),
                    createdAt = kotlinx.datetime.Instant.fromEpochSeconds(row.createdAt),
                    updatedAt = kotlinx.datetime.Instant.fromEpochSeconds(row.updatedAt),
                    version = row.version.toInt(),
                    parentRecipeId = row.parentRecipeId
                )
            }
            Result.success(recipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}