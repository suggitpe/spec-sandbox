package com.recipemanager.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.model.CookingStep
import com.recipemanager.domain.model.Ingredient
import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.model.RecipeCollection
import com.recipemanager.domain.repository.CollectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CollectionRepositoryImpl(
    private val database: RecipeDatabase
) : CollectionRepository {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun createCollection(collection: RecipeCollection): Result<RecipeCollection> = withContext(Dispatchers.Default) {
        try {
            database.transaction {
                // Insert collection
                database.collectionQueries.insertCollection(
                    id = collection.id,
                    name = collection.name,
                    description = collection.description,
                    createdAt = collection.createdAt.epochSeconds,
                    updatedAt = collection.updatedAt.epochSeconds
                )
                
                // Insert recipe associations
                collection.recipeIds.forEach { recipeId ->
                    database.recipeCollectionQueries.insertRecipeCollection(
                        recipeId = recipeId,
                        collectionId = collection.id,
                        addedAt = Clock.System.now().epochSeconds
                    )
                }
            }
            Result.success(collection)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCollection(id: String): Result<RecipeCollection?> = withContext(Dispatchers.Default) {
        try {
            val collectionRow = database.collectionQueries.selectCollectionById(id).executeAsOneOrNull()
            
            if (collectionRow == null) {
                return@withContext Result.success(null)
            }
            
            // Get associated recipe IDs
            val recipeIds = database.recipeCollectionQueries.selectRecipesByCollectionId(id)
                .executeAsList()
                .map { it.id }
            
            val collection = RecipeCollection(
                id = collectionRow.id,
                name = collectionRow.name,
                description = collectionRow.description,
                recipeIds = recipeIds,
                createdAt = Instant.fromEpochSeconds(collectionRow.createdAt),
                updatedAt = Instant.fromEpochSeconds(collectionRow.updatedAt)
            )
            
            Result.success(collection)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateCollection(collection: RecipeCollection): Result<RecipeCollection> = withContext(Dispatchers.Default) {
        try {
            database.transaction {
                // Update collection metadata
                database.collectionQueries.updateCollection(
                    name = collection.name,
                    description = collection.description,
                    updatedAt = collection.updatedAt.epochSeconds,
                    id = collection.id
                )
                
                // Update recipe associations
                // Delete existing associations
                database.recipeCollectionQueries.deleteRecipeCollectionsByCollectionId(collection.id)
                
                // Insert updated associations
                collection.recipeIds.forEach { recipeId ->
                    database.recipeCollectionQueries.insertRecipeCollection(
                        recipeId = recipeId,
                        collectionId = collection.id,
                        addedAt = Clock.System.now().epochSeconds
                    )
                }
            }
            Result.success(collection)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteCollection(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.transaction {
                // Delete recipe associations (cascade will handle this, but explicit is clearer)
                database.recipeCollectionQueries.deleteRecipeCollectionsByCollectionId(id)
                
                // Delete collection
                database.collectionQueries.deleteCollection(id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAllCollections(): Result<List<RecipeCollection>> = withContext(Dispatchers.Default) {
        try {
            val collectionRows = database.collectionQueries.selectAllCollections().executeAsList()
            
            val collections = collectionRows.map { collectionRow ->
                val recipeIds = database.recipeCollectionQueries.selectRecipesByCollectionId(collectionRow.id)
                    .executeAsList()
                    .map { it.id }
                
                RecipeCollection(
                    id = collectionRow.id,
                    name = collectionRow.name,
                    description = collectionRow.description,
                    recipeIds = recipeIds,
                    createdAt = Instant.fromEpochSeconds(collectionRow.createdAt),
                    updatedAt = Instant.fromEpochSeconds(collectionRow.updatedAt)
                )
            }
            
            Result.success(collections)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun observeCollections(): Flow<List<RecipeCollection>> {
        return database.collectionQueries.selectAllCollections()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { collectionRows ->
                collectionRows.map { collectionRow ->
                    val recipeIds = database.recipeCollectionQueries.selectRecipesByCollectionId(collectionRow.id)
                        .executeAsList()
                        .map { it.id }
                    
                    RecipeCollection(
                        id = collectionRow.id,
                        name = collectionRow.name,
                        description = collectionRow.description,
                        recipeIds = recipeIds,
                        createdAt = Instant.fromEpochSeconds(collectionRow.createdAt),
                        updatedAt = Instant.fromEpochSeconds(collectionRow.updatedAt)
                    )
                }
            }
    }
    
    override suspend fun addRecipeToCollection(recipeId: String, collectionId: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.recipeCollectionQueries.insertRecipeCollection(
                recipeId = recipeId,
                collectionId = collectionId,
                addedAt = Clock.System.now().epochSeconds
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun removeRecipeFromCollection(recipeId: String, collectionId: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.recipeCollectionQueries.deleteRecipeCollection(
                recipeId = recipeId,
                collectionId = collectionId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getRecipesInCollection(collectionId: String): Result<List<Recipe>> = withContext(Dispatchers.Default) {
        try {
            val recipeRows = database.recipeCollectionQueries.selectRecipesByCollectionId(collectionId).executeAsList()
            
            val recipes = recipeRows.map { recipeRow ->
                val ingredients = database.ingredientQueries.selectIngredientsByRecipeId(recipeRow.id)
                    .executeAsList()
                    .map { ingredientRow ->
                        Ingredient(
                            id = ingredientRow.id,
                            name = ingredientRow.name,
                            quantity = ingredientRow.quantity,
                            unit = ingredientRow.unit,
                            notes = ingredientRow.notes,
                            photos = emptyList()
                        )
                    }
                
                val steps = database.cookingStepQueries.selectStepsByRecipeId(recipeRow.id)
                    .executeAsList()
                    .map { stepRow ->
                        CookingStep(
                            id = stepRow.id,
                            stepNumber = stepRow.stepNumber.toInt(),
                            instruction = stepRow.instruction,
                            duration = stepRow.duration?.toInt(),
                            temperature = stepRow.temperature?.toInt(),
                            photos = emptyList(),
                            timerRequired = stepRow.timerRequired == 1L
                        )
                    }
                
                Recipe(
                    id = recipeRow.id,
                    title = recipeRow.title,
                    description = recipeRow.description,
                    ingredients = ingredients,
                    steps = steps,
                    preparationTime = recipeRow.preparationTime.toInt(),
                    cookingTime = recipeRow.cookingTime.toInt(),
                    servings = recipeRow.servings.toInt(),
                    tags = json.decodeFromString(recipeRow.tags),
                    createdAt = Instant.fromEpochSeconds(recipeRow.createdAt),
                    updatedAt = Instant.fromEpochSeconds(recipeRow.updatedAt),
                    version = recipeRow.version.toInt(),
                    parentRecipeId = recipeRow.parentRecipeId
                )
            }
            
            Result.success(recipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCollectionsForRecipe(recipeId: String): Result<List<RecipeCollection>> = withContext(Dispatchers.Default) {
        try {
            val collectionRows = database.recipeCollectionQueries.selectCollectionsByRecipeId(recipeId).executeAsList()
            
            val collections = collectionRows.map { collectionRow ->
                val recipeIds = database.recipeCollectionQueries.selectRecipesByCollectionId(collectionRow.id)
                    .executeAsList()
                    .map { it.id }
                
                RecipeCollection(
                    id = collectionRow.id,
                    name = collectionRow.name,
                    description = collectionRow.description,
                    recipeIds = recipeIds,
                    createdAt = Instant.fromEpochSeconds(collectionRow.createdAt),
                    updatedAt = Instant.fromEpochSeconds(collectionRow.updatedAt)
                )
            }
            
            Result.success(collections)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
