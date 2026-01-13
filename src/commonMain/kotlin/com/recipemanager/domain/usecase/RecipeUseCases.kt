package com.recipemanager.domain.usecase

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.domain.validation.RecipeValidator
import com.recipemanager.domain.validation.ValidationException
import com.recipemanager.domain.validation.ValidationResult
import kotlinx.coroutines.flow.Flow

class RecipeUseCases(
    private val repository: RecipeRepository,
    private val validator: RecipeValidator
) {
    
    suspend fun createRecipe(recipe: Recipe): Result<Recipe> {
        return try {
            when (val validationResult = validator.validateRecipe(recipe)) {
                is ValidationResult.Success -> repository.createRecipe(recipe)
                is ValidationResult.Error -> Result.failure(
                    ValidationException(validationResult.errors.joinToString(", "))
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRecipe(id: String): Result<Recipe?> {
        return repository.getRecipe(id)
    }
    
    suspend fun updateRecipe(recipe: Recipe): Result<Recipe> {
        return try {
            when (val validationResult = validator.validateRecipe(recipe)) {
                is ValidationResult.Success -> repository.updateRecipe(recipe)
                is ValidationResult.Error -> Result.failure(
                    ValidationException(validationResult.errors.joinToString(", "))
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteRecipe(id: String): Result<Unit> {
        return repository.deleteRecipe(id)
    }
    
    suspend fun searchRecipes(query: String): Result<List<Recipe>> {
        return repository.searchRecipes(query)
    }
    
    fun observeRecipes(): Flow<List<Recipe>> {
        return repository.observeRecipes()
    }
    
    suspend fun getAllRecipes(): Result<List<Recipe>> {
        return repository.getAllRecipes()
    }
}