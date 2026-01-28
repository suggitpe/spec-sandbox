package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.CookingStep
import com.recipemanager.domain.model.Ingredient
import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.domain.validation.RecipeValidator
import com.recipemanager.presentation.navigation.StatePersistence
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
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
    val saveSuccess: Boolean = false,
    val isDirty: Boolean = false, // Track if form has unsaved changes
    val lastSaveTime: Long = 0L
)

class RecipeFormViewModel(
    private val recipeRepository: RecipeRepository,
    private val recipeValidator: RecipeValidator,
    statePersistence: StatePersistence? = null
) : BaseViewModel<RecipeFormState>(
    initialState = RecipeFormState(),
    statePersistence = statePersistence,
    stateKey = "recipe_form"
) {
    
    override fun onInitialize() {
        // If we have a recipe ID in restored state, load it
        currentState.recipeId?.let { recipeId ->
            loadRecipe(recipeId)
        }
    }

    fun loadRecipe(recipeId: String) {
        viewModelScope.launch {
            setLoading(true)
            setError(null)
            
            recipeRepository.getRecipe(recipeId)
                .onSuccess { recipe ->
                    recipe?.let {
                        currentState = currentState.copy(
                            recipeId = it.id,
                            title = it.title,
                            description = it.description ?: "",
                            ingredients = it.ingredients,
                            steps = it.steps,
                            preparationTime = it.preparationTime,
                            cookingTime = it.cookingTime,
                            servings = it.servings,
                            tags = it.tags,
                            isLoading = false,
                            isDirty = false
                        )
                        setLoading(false)
                    }
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to load recipe")
                    setLoading(false)
                }
        }
    }

    fun updateTitle(title: String) {
        currentState = currentState.copy(title = title, isDirty = true)
        clearValidationError("title")
    }

    fun updateDescription(description: String) {
        currentState = currentState.copy(description = description, isDirty = true)
    }

    fun updatePreparationTime(time: Int) {
        currentState = currentState.copy(preparationTime = time.coerceAtLeast(0), isDirty = true)
    }

    fun updateCookingTime(time: Int) {
        currentState = currentState.copy(cookingTime = time.coerceAtLeast(0), isDirty = true)
    }

    fun updateServings(servings: Int) {
        currentState = currentState.copy(servings = servings.coerceAtLeast(1), isDirty = true)
    }

    fun addIngredient(ingredient: Ingredient) {
        currentState = currentState.copy(
            ingredients = currentState.ingredients + ingredient,
            isDirty = true
        )
        clearValidationError("ingredients")
    }

    fun removeIngredient(ingredientId: String) {
        currentState = currentState.copy(
            ingredients = currentState.ingredients.filter { it.id != ingredientId },
            isDirty = true
        )
    }

    fun addStep(step: CookingStep) {
        currentState = currentState.copy(
            steps = currentState.steps + step,
            isDirty = true
        )
        clearValidationError("steps")
    }

    fun removeStep(stepId: String) {
        currentState = currentState.copy(
            steps = currentState.steps.filter { it.id != stepId },
            isDirty = true
        )
    }

    fun addTag(tag: String) {
        if (tag.isNotBlank() && !currentState.tags.contains(tag)) {
            currentState = currentState.copy(
                tags = currentState.tags + tag,
                isDirty = true
            )
        }
    }

    fun removeTag(tag: String) {
        currentState = currentState.copy(
            tags = currentState.tags.filter { it != tag },
            isDirty = true
        )
    }

    fun saveRecipe() {
        val currentState = this.currentState
        
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
            this.currentState = this.currentState.copy(validationErrors = errors)
            return
        }

        viewModelScope.launch {
            this@RecipeFormViewModel.currentState = this@RecipeFormViewModel.currentState.copy(isSaving = true)
            setError(null)
            
            val result = if (currentState.recipeId != null) {
                recipeRepository.updateRecipe(recipe)
            } else {
                recipeRepository.createRecipe(recipe)
            }

            result
                .onSuccess {
                    this@RecipeFormViewModel.currentState = this@RecipeFormViewModel.currentState.copy(
                        isSaving = false,
                        saveSuccess = true,
                        isDirty = false,
                        lastSaveTime = System.currentTimeMillis()
                    )
                }
                .onFailure { error ->
                    this@RecipeFormViewModel.currentState = this@RecipeFormViewModel.currentState.copy(
                        isSaving = false
                    )
                    setError(error.message ?: "Failed to save recipe")
                }
        }
    }

    private fun clearValidationError(field: String) {
        currentState = currentState.copy(
            validationErrors = currentState.validationErrors.filterKeys { it != field }
        )
    }

    fun resetSaveSuccess() {
        currentState = currentState.copy(saveSuccess = false)
    }
    
    fun resetForm() {
        currentState = RecipeFormState()
    }
    
    fun hasUnsavedChanges(): Boolean = currentState.isDirty

    private fun generateId(): String {
        return "recipe_${Clock.System.now().toEpochMilliseconds()}"
    }
    
    override fun onAppPaused() {
        super.onAppPaused()
        // Auto-save draft if there are unsaved changes
        if (currentState.isDirty && currentState.title.isNotBlank()) {
            // Could implement auto-save to drafts here
        }
    }
    
    override fun serializeState(state: RecipeFormState): String? {
        return try {
            Json.encodeToString(state)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun deserializeState(serializedState: String): RecipeFormState? {
        return try {
            Json.decodeFromString<RecipeFormState>(serializedState)
        } catch (e: Exception) {
            null
        }
    }
}
