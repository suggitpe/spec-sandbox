package com.recipemanager.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.model.CookingStep
import com.recipemanager.domain.model.Ingredient
import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RecipeRepositoryImpl(
    private val database: RecipeDatabase
) : RecipeRepository {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun createRecipe(recipe: Recipe): Result<Recipe> = withContext(Dispatchers.Default) {
        try {
            database.transaction {
                // Insert recipe
                database.recipeQueries.insertRecipe(
                    id = recipe.id,
                    title = recipe.title,
                    description = recipe.description,
                    preparationTime = recipe.preparationTime.toLong(),
                    cookingTime = recipe.cookingTime.toLong(),
                    servings = recipe.servings.toLong(),
                    tags = json.encodeToString(recipe.tags),
                    createdAt = recipe.createdAt.epochSeconds,
                    updatedAt = recipe.updatedAt.epochSeconds,
                    version = recipe.version.toLong(),
                    parentRecipeId = recipe.parentRecipeId
                )
                
                // Insert ingredients
                recipe.ingredients.forEach { ingredient ->
                    database.ingredientQueries.insertIngredient(
                        id = ingredient.id,
                        recipeId = recipe.id,
                        name = ingredient.name,
                        quantity = ingredient.quantity,
                        unit = ingredient.unit,
                        notes = ingredient.notes
                    )
                }
                
                // Insert cooking steps
                recipe.steps.forEach { step ->
                    database.cookingStepQueries.insertCookingStep(
                        id = step.id,
                        recipeId = recipe.id,
                        stepNumber = step.stepNumber.toLong(),
                        instruction = step.instruction,
                        duration = step.duration?.toLong(),
                        temperature = step.temperature?.toLong(),
                        timerRequired = if (step.timerRequired) 1L else 0L
                    )
                }
            }
            Result.success(recipe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getRecipe(id: String): Result<Recipe?> = withContext(Dispatchers.Default) {
        try {
            val recipeRow = database.recipeQueries.selectRecipeById(id).executeAsOneOrNull()
            
            if (recipeRow == null) {
                return@withContext Result.success(null)
            }
            
            val ingredients = database.ingredientQueries.selectIngredientsByRecipeId(id)
                .executeAsList()
                .map { ingredientRow ->
                    Ingredient(
                        id = ingredientRow.id,
                        name = ingredientRow.name,
                        quantity = ingredientRow.quantity,
                        unit = ingredientRow.unit,
                        notes = ingredientRow.notes,
                        photos = emptyList() // Photos will be loaded separately if needed
                    )
                }
            
            val steps = database.cookingStepQueries.selectStepsByRecipeId(id)
                .executeAsList()
                .map { stepRow ->
                    CookingStep(
                        id = stepRow.id,
                        stepNumber = stepRow.stepNumber.toInt(),
                        instruction = stepRow.instruction,
                        duration = stepRow.duration?.toInt(),
                        temperature = stepRow.temperature?.toInt(),
                        photos = emptyList(), // Photos will be loaded separately if needed
                        timerRequired = stepRow.timerRequired == 1L
                    )
                }
            
            val recipe = Recipe(
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
            
            Result.success(recipe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateRecipe(recipe: Recipe): Result<Recipe> = withContext(Dispatchers.Default) {
        try {
            database.transaction {
                // Update recipe
                database.recipeQueries.updateRecipe(
                    title = recipe.title,
                    description = recipe.description,
                    preparationTime = recipe.preparationTime.toLong(),
                    cookingTime = recipe.cookingTime.toLong(),
                    servings = recipe.servings.toLong(),
                    tags = json.encodeToString(recipe.tags),
                    updatedAt = recipe.updatedAt.epochSeconds,
                    version = recipe.version.toLong(),
                    parentRecipeId = recipe.parentRecipeId,
                    id = recipe.id
                )
                
                // Delete existing ingredients and steps
                database.ingredientQueries.deleteIngredientsByRecipeId(recipe.id)
                database.cookingStepQueries.deleteCookingStepsByRecipeId(recipe.id)
                
                // Insert updated ingredients
                recipe.ingredients.forEach { ingredient ->
                    database.ingredientQueries.insertIngredient(
                        id = ingredient.id,
                        recipeId = recipe.id,
                        name = ingredient.name,
                        quantity = ingredient.quantity,
                        unit = ingredient.unit,
                        notes = ingredient.notes
                    )
                }
                
                // Insert updated cooking steps
                recipe.steps.forEach { step ->
                    database.cookingStepQueries.insertCookingStep(
                        id = step.id,
                        recipeId = recipe.id,
                        stepNumber = step.stepNumber.toLong(),
                        instruction = step.instruction,
                        duration = step.duration?.toLong(),
                        temperature = step.temperature?.toLong(),
                        timerRequired = if (step.timerRequired) 1L else 0L
                    )
                }
            }
            Result.success(recipe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteRecipe(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.transaction {
                // Delete ingredients and steps (cascade will handle this, but explicit is clearer)
                database.ingredientQueries.deleteIngredientsByRecipeId(id)
                database.cookingStepQueries.deleteCookingStepsByRecipeId(id)
                
                // Delete recipe
                database.recipeQueries.deleteRecipe(id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun searchRecipes(query: String): Result<List<Recipe>> = withContext(Dispatchers.Default) {
        try {
            val recipeRows = database.recipeQueries.searchRecipes(query, query, query).executeAsList()
            
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
    
    override fun observeRecipes(): Flow<List<Recipe>> {
        return database.recipeQueries.selectAllRecipes()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { recipeRows ->
                recipeRows.map { recipeRow ->
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
            }
    }
    
    override suspend fun getAllRecipes(): Result<List<Recipe>> = withContext(Dispatchers.Default) {
        try {
            val recipeRows = database.recipeQueries.selectAllRecipes().executeAsList()
            
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
}
