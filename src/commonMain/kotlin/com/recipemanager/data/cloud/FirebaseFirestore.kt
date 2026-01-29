package com.recipemanager.data.cloud

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.model.RecipeCollection
import kotlinx.coroutines.flow.Flow

/**
 * Firebase Firestore interface for recipe data synchronization
 */
interface FirebaseFirestore {
    
    /**
     * Save a recipe to Firestore
     * @param recipe The recipe to save
     * @param userId The user ID who owns the recipe
     * @return Result indicating success or failure
     */
    suspend fun saveRecipe(recipe: Recipe, userId: String): Result<Unit>
    
    /**
     * Get a recipe from Firestore
     * @param recipeId The ID of the recipe to retrieve
     * @param userId The user ID who owns the recipe
     * @return Result containing the recipe or null if not found
     */
    suspend fun getRecipe(recipeId: String, userId: String): Result<Recipe?>
    
    /**
     * Get all recipes for a user
     * @param userId The user ID
     * @return Flow of recipe lists
     */
    fun getUserRecipes(userId: String): Flow<List<Recipe>>
    
    /**
     * Delete a recipe from Firestore
     * @param recipeId The ID of the recipe to delete
     * @param userId The user ID who owns the recipe
     * @return Result indicating success or failure
     */
    suspend fun deleteRecipe(recipeId: String, userId: String): Result<Unit>
}