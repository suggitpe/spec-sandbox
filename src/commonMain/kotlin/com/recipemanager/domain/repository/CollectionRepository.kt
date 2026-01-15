package com.recipemanager.domain.repository

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.model.RecipeCollection
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    suspend fun createCollection(collection: RecipeCollection): Result<RecipeCollection>
    suspend fun getCollection(id: String): Result<RecipeCollection?>
    suspend fun updateCollection(collection: RecipeCollection): Result<RecipeCollection>
    suspend fun deleteCollection(id: String): Result<Unit>
    suspend fun getAllCollections(): Result<List<RecipeCollection>>
    fun observeCollections(): Flow<List<RecipeCollection>>
    
    // Recipe-Collection association methods
    suspend fun addRecipeToCollection(recipeId: String, collectionId: String): Result<Unit>
    suspend fun removeRecipeFromCollection(recipeId: String, collectionId: String): Result<Unit>
    suspend fun getRecipesInCollection(collectionId: String): Result<List<Recipe>>
    suspend fun getCollectionsForRecipe(recipeId: String): Result<List<RecipeCollection>>
}
