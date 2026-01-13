package com.recipemanager.domain.validation

import com.recipemanager.domain.model.Recipe

class RecipeValidator {
    
    fun validateRecipe(recipe: Recipe): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (recipe.title.isBlank()) {
            errors.add("Recipe title cannot be empty")
        }
        
        if (recipe.ingredients.isEmpty()) {
            errors.add("Recipe must have at least one ingredient")
        }
        
        if (recipe.steps.isEmpty()) {
            errors.add("Recipe must have at least one cooking step")
        }
        
        if (recipe.preparationTime < 0) {
            errors.add("Preparation time cannot be negative")
        }
        
        if (recipe.cookingTime < 0) {
            errors.add("Cooking time cannot be negative")
        }
        
        if (recipe.servings <= 0) {
            errors.add("Servings must be greater than zero")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val errors: List<String>) : ValidationResult()
}

class ValidationException(message: String) : Exception(message)