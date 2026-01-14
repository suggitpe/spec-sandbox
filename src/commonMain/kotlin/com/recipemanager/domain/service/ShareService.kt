package com.recipemanager.domain.service

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.validation.RecipeValidator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Service for exporting and importing recipes with serialization support.
 * Handles recipe data serialization using Kotlinx Serialization.
 */
class ShareService(
    private val validator: RecipeValidator
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Exports a recipe to JSON format for sharing.
     * 
     * @param recipe The recipe to export
     * @return Result containing the JSON string or an error
     */
    fun exportRecipe(recipe: Recipe): Result<String> {
        return try {
            val jsonString = json.encodeToString(recipe)
            Result.success(jsonString)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to export recipe: ${e.message}", e))
        }
    }

    /**
     * Imports a recipe from JSON format with validation.
     * 
     * @param jsonString The JSON string containing recipe data
     * @return Result containing the imported recipe or an error
     */
    fun importRecipe(jsonString: String): Result<Recipe> {
        return try {
            val recipe = json.decodeFromString<Recipe>(jsonString)
            
            // Validate the imported recipe
            val validationResult = validator.validateRecipe(recipe)
            if (validationResult !is com.recipemanager.domain.validation.ValidationResult.Success) {
                return Result.failure(Exception("Invalid recipe data: $validationResult"))
            }
            
            Result.success(recipe)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to import recipe: ${e.message}", e))
        }
    }

    /**
     * Exports multiple recipes to JSON format.
     * 
     * @param recipes The list of recipes to export
     * @return Result containing the JSON string or an error
     */
    fun exportRecipes(recipes: List<Recipe>): Result<String> {
        return try {
            val jsonString = json.encodeToString(recipes)
            Result.success(jsonString)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to export recipes: ${e.message}", e))
        }
    }

    /**
     * Imports multiple recipes from JSON format with validation.
     * 
     * @param jsonString The JSON string containing recipe data
     * @return Result containing the list of imported recipes or an error
     */
    fun importRecipes(jsonString: String): Result<List<Recipe>> {
        return try {
            val recipes = json.decodeFromString<List<Recipe>>(jsonString)
            
            // Validate all imported recipes
            val invalidRecipes = recipes.filter { recipe ->
                validator.validateRecipe(recipe) !is com.recipemanager.domain.validation.ValidationResult.Success
            }
            
            if (invalidRecipes.isNotEmpty()) {
                return Result.failure(Exception("Invalid recipe data found in ${invalidRecipes.size} recipe(s)"))
            }
            
            Result.success(recipes)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to import recipes: ${e.message}", e))
        }
    }
}
