package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.CookingStep
import com.recipemanager.domain.model.Ingredient
import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.domain.validation.RecipeValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class RecipeFormState(
    val recipeId: String? = null,
    val title: String = "",
    val description: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<CookingStep> = emptyList(),
    val preparationTime: Int = 0,
    val cookingTime: Int = 0,
    val servings: Int = 1,
    val tags: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val saveSuccess: Boolean = false
)

class RecipeFormViewModel(
    private val recipeRepository: RecipeRepository,
    private val recipeValidator: RecipeValidator,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(RecipeFormState())
    val state: StateFlow<RecipeFormState> = _state.asStateFlow()

    fun loadRecipe(recipeId: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            recipeRepository.getRecipe(recipeId)
                .onSuccess { recipe ->
                    recipe?.let {
                        _state.value = _state.value.copy(
                            recipeId = it.id,
                            title = it.title,
                            description = it.description ?: "",
                            ingredients = it.ingredients,
                            steps = it.steps,
                            preparationTime = it.preparationTime,
                            cookingTime = it.cookingTime,
                            servings = it.servings,
                            tags = it.tags,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load recipe"
                    )
                }
        }
    }

    fun updateTitle(title: String) {
        _state.value = _state.value.copy(title = title)
        clearValidationError("title")
    }

    fun updateDescription(description: String) {
        _state.value = _state.value.copy(description = description)
    }

    fun updatePreparationTime(time: Int) {
        _state.value = _state.value.copy(preparationTime = time.coerceAtLeast(0))
    }

    fun updateCookingTime(time: Int) {
        _state.value = _state.value.copy(cookingTime = time.coerceAtLeast(0))
    }

    fun updateServings(servings: Int) {
        _state.value = _state.value.copy(servings = servings.coerceAtLeast(1))
    }

    fun addIngredient(ingredient: Ingredient) {
        _state.value = _state.value.copy(
            ingredients = _state.value.ingredients + ingredient
        )
        clearValidationError("ingredients")
    }

    fun removeIngredient(ingredientId: String) {
        _state.value = _state.value.copy(
            ingredients = _state.value.ingredients.filter { it.id != ingredientId }
        )
    }

    fun addStep(step: CookingStep) {
        _state.value = _state.value.copy(
            steps = _state.value.steps + step
        )
        clearValidationError("steps")
    }

    fun removeStep(stepId: String) {
        _state.value = _state.value.copy(
            steps = _state.value.steps.filter { it.id != stepId }
        )
    }

    fun addTag(tag: String) {
        if (tag.isNotBlank() && !_state.value.tags.contains(tag)) {
            _state.value = _state.value.copy(
                tags = _state.value.tags + tag
            )
        }
    }

    fun removeTag(tag: String) {
        _state.value = _state.value.copy(
            tags = _state.value.tags.filter { it != tag }
        )
    }

    fun saveRecipe() {
        val currentState = _state.value
        
        // Build recipe object
        val now = Clock.System.now()
        val recipe = Recipe(
            id = currentState.recipeId ?: generateId(),
            title = currentState.title,
            description = currentState.description.ifBlank { null },
            ingredients = currentState.ingredients,
            steps = currentState.steps,
            preparationTime = currentState.preparationTime,
            cookingTime = currentState.cookingTime,
            servings = currentState.servings,
            tags = currentState.tags,
            createdAt = now,
            updatedAt = now,
            version = 1
        )

        // Validate recipe
        val validationResult = recipeValidator.validateRecipe(recipe)
        if (validationResult is com.recipemanager.domain.validation.ValidationResult.Error) {
            val errors = mutableMapOf<String, String>()
            validationResult.errors.forEach { error ->
                when {
                    error.contains("title") -> errors["title"] = error
                    error.contains("ingredient") -> errors["ingredients"] = error
                    error.contains("step") -> errors["steps"] = error
                    else -> errors["general"] = error
                }
            }
            _state.value = _state.value.copy(validationErrors = errors)
            return
        }

        scope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            
            val result = if (currentState.recipeId != null) {
                recipeRepository.updateRecipe(recipe)
            } else {
                recipeRepository.createRecipe(recipe)
            }

            result
                .onSuccess {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = error.message ?: "Failed to save recipe"
                    )
                }
        }
    }

    private fun clearValidationError(field: String) {
        _state.value = _state.value.copy(
            validationErrors = _state.value.validationErrors.filterKeys { it != field }
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun resetSaveSuccess() {
        _state.value = _state.value.copy(saveSuccess = false)
    }

    private fun generateId(): String {
        return "recipe_${Clock.System.now().toEpochMilliseconds()}"
    }
}
