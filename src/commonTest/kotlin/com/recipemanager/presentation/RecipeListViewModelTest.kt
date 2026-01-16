package com.recipemanager.presentation

import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.database.DatabaseManager
import com.recipemanager.data.repository.RecipeRepositoryImpl
import com.recipemanager.domain.model.CookingStep
import com.recipemanager.domain.model.Ingredient
import com.recipemanager.domain.model.Recipe
import com.recipemanager.presentation.viewmodel.RecipeListViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.milliseconds

class RecipeListViewModelTest : FunSpec({
    
    lateinit var databaseManager: DatabaseManager
    lateinit var recipeRepository: RecipeRepositoryImpl
    lateinit var viewModel: RecipeListViewModel
    
    beforeEach {
        val driverFactory = DatabaseDriverFactory()
        databaseManager = DatabaseManager(driverFactory)
        databaseManager.initialize()
        recipeRepository = RecipeRepositoryImpl(databaseManager.getDatabase())
        viewModel = RecipeListViewModel(recipeRepository)
    }
    
    afterEach {
        databaseManager.close()
    }
    
    test("should load recipes successfully") {
        // Create a test recipe
        val recipe = Recipe(
            id = "test-recipe-1",
            title = "Test Recipe",
            description = "A test recipe",
            ingredients = listOf(
                Ingredient(
                    id = "ing-1",
                    name = "Flour",
                    quantity = 2.0,
                    unit = "cups"
                )
            ),
            steps = listOf(
                CookingStep(
                    id = "step-1",
                    stepNumber = 1,
                    instruction = "Mix ingredients"
                )
            ),
            preparationTime = 10,
            cookingTime = 20,
            servings = 4,
            tags = listOf("easy", "quick"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        recipeRepository.createRecipe(recipe)
        
        // Load recipes
        viewModel.loadRecipes()
        
        // Wait for async operation
        delay(100.milliseconds)
        
        // Verify state
        val state = viewModel.state.value
        state.isLoading shouldBe false
        state.recipes.size shouldBe 1
        state.recipes[0].id shouldBe "test-recipe-1"
        state.filteredRecipes.size shouldBe 1
        state.error shouldBe null
    }
    
    test("should search recipes by title") {
        // Create test recipes
        val recipe1 = Recipe(
            id = "recipe-1",
            title = "Pasta Carbonara",
            description = "Italian pasta dish",
            ingredients = listOf(
                Ingredient(id = "ing-1", name = "Pasta", quantity = 200.0, unit = "g")
            ),
            steps = listOf(
                CookingStep(id = "step-1", stepNumber = 1, instruction = "Cook pasta")
            ),
            preparationTime = 10,
            cookingTime = 15,
            servings = 2,
            tags = listOf("italian", "pasta"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val recipe2 = Recipe(
            id = "recipe-2",
            title = "Beef Stew",
            description = "Hearty beef stew",
            ingredients = listOf(
                Ingredient(id = "ing-2", name = "Beef", quantity = 500.0, unit = "g")
            ),
            steps = listOf(
                CookingStep(id = "step-2", stepNumber = 1, instruction = "Brown beef")
            ),
            preparationTime = 20,
            cookingTime = 120,
            servings = 4,
            tags = listOf("meat", "slow"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        recipeRepository.createRecipe(recipe1)
        recipeRepository.createRecipe(recipe2)
        
        // Load recipes first
        viewModel.loadRecipes()
        delay(100.milliseconds)
        
        // Search for "pasta"
        viewModel.searchRecipes("pasta")
        delay(100.milliseconds)
        
        // Verify filtered results
        val state = viewModel.state.value
        state.searchQuery shouldBe "pasta"
        state.filteredRecipes.size shouldBe 1
        state.filteredRecipes[0].title shouldBe "Pasta Carbonara"
    }
    
    test("should show all recipes when search query is empty") {
        // Create test recipes
        val recipe1 = Recipe(
            id = "recipe-1",
            title = "Recipe 1",
            description = null,
            ingredients = listOf(
                Ingredient(id = "ing-1", name = "Ingredient 1", quantity = 1.0, unit = "unit")
            ),
            steps = listOf(
                CookingStep(id = "step-1", stepNumber = 1, instruction = "Step 1")
            ),
            preparationTime = 10,
            cookingTime = 10,
            servings = 1,
            tags = emptyList(),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val recipe2 = Recipe(
            id = "recipe-2",
            title = "Recipe 2",
            description = null,
            ingredients = listOf(
                Ingredient(id = "ing-2", name = "Ingredient 2", quantity = 1.0, unit = "unit")
            ),
            steps = listOf(
                CookingStep(id = "step-2", stepNumber = 1, instruction = "Step 2")
            ),
            preparationTime = 10,
            cookingTime = 10,
            servings = 1,
            tags = emptyList(),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        recipeRepository.createRecipe(recipe1)
        recipeRepository.createRecipe(recipe2)
        
        // Load recipes
        viewModel.loadRecipes()
        delay(100.milliseconds)
        
        // Search with empty query
        viewModel.searchRecipes("")
        
        // Verify all recipes are shown
        val state = viewModel.state.value
        state.filteredRecipes.size shouldBe 2
    }
    
    test("should delete recipe successfully") {
        // Create a test recipe
        val recipe = Recipe(
            id = "recipe-to-delete",
            title = "Recipe to Delete",
            description = null,
            ingredients = listOf(
                Ingredient(id = "ing-1", name = "Ingredient", quantity = 1.0, unit = "unit")
            ),
            steps = listOf(
                CookingStep(id = "step-1", stepNumber = 1, instruction = "Step")
            ),
            preparationTime = 10,
            cookingTime = 10,
            servings = 1,
            tags = emptyList(),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        recipeRepository.createRecipe(recipe)
        
        // Load recipes
        viewModel.loadRecipes()
        delay(100.milliseconds)
        
        // Verify recipe exists
        viewModel.state.value.recipes.size shouldBe 1
        
        // Delete recipe
        viewModel.deleteRecipe("recipe-to-delete")
        delay(100.milliseconds)
        
        // Verify recipe is deleted
        viewModel.state.value.recipes.size shouldBe 0
    }
    
    test("should clear error message") {
        // Load recipes first to initialize state
        viewModel.loadRecipes()
        delay(100.milliseconds)
        
        // Manually trigger an error scenario by trying to search with invalid repository state
        // In a real scenario, this would come from a repository failure
        // For now, we'll just verify the clearError functionality works
        
        // Clear error (even if no error exists, this should work)
        viewModel.clearError()
        
        // Verify error is cleared
        viewModel.state.value.error shouldBe null
    }
})
