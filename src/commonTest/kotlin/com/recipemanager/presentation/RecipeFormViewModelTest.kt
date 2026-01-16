package com.recipemanager.presentation

import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.database.DatabaseManager
import com.recipemanager.data.repository.RecipeRepositoryImpl
import com.recipemanager.domain.model.CookingStep
import com.recipemanager.domain.model.Ingredient
import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.validation.RecipeValidator
import com.recipemanager.presentation.viewmodel.RecipeFormViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.milliseconds

class RecipeFormViewModelTest : FunSpec({
    
    lateinit var databaseManager: DatabaseManager
    lateinit var recipeRepository: RecipeRepositoryImpl
    lateinit var recipeValidator: RecipeValidator
    lateinit var viewModel: RecipeFormViewModel
    
    beforeEach {
        val driverFactory = DatabaseDriverFactory()
        databaseManager = DatabaseManager(driverFactory)
        databaseManager.initialize()
        recipeRepository = RecipeRepositoryImpl(databaseManager.getDatabase())
        recipeValidator = RecipeValidator()
        viewModel = RecipeFormViewModel(recipeRepository, recipeValidator)
    }
    
    afterEach {
        databaseManager.close()
    }
    
    test("should update title") {
        viewModel.updateTitle("New Recipe Title")
        
        viewModel.state.value.title shouldBe "New Recipe Title"
    }
    
    test("should update description") {
        viewModel.updateDescription("This is a description")
        
        viewModel.state.value.description shouldBe "This is a description"
    }
    
    test("should update preparation time") {
        viewModel.updatePreparationTime(30)
        
        viewModel.state.value.preparationTime shouldBe 30
    }
    
    test("should not allow negative preparation time") {
        viewModel.updatePreparationTime(-10)
        
        viewModel.state.value.preparationTime shouldBe 0
    }
    
    test("should update cooking time") {
        viewModel.updateCookingTime(45)
        
        viewModel.state.value.cookingTime shouldBe 45
    }
    
    test("should update servings") {
        viewModel.updateServings(6)
        
        viewModel.state.value.servings shouldBe 6
    }
    
    test("should not allow servings less than 1") {
        viewModel.updateServings(0)
        
        viewModel.state.value.servings shouldBe 1
    }
    
    test("should add ingredient") {
        val ingredient = Ingredient(
            id = "ing-1",
            name = "Flour",
            quantity = 2.0,
            unit = "cups"
        )
        
        viewModel.addIngredient(ingredient)
        
        viewModel.state.value.ingredients.size shouldBe 1
        viewModel.state.value.ingredients[0] shouldBe ingredient
    }
    
    test("should remove ingredient") {
        val ingredient1 = Ingredient(id = "ing-1", name = "Flour", quantity = 2.0, unit = "cups")
        val ingredient2 = Ingredient(id = "ing-2", name = "Sugar", quantity = 1.0, unit = "cup")
        
        viewModel.addIngredient(ingredient1)
        viewModel.addIngredient(ingredient2)
        
        viewModel.state.value.ingredients.size shouldBe 2
        
        viewModel.removeIngredient("ing-1")
        
        viewModel.state.value.ingredients.size shouldBe 1
        viewModel.state.value.ingredients[0].id shouldBe "ing-2"
    }
    
    test("should add cooking step") {
        val step = CookingStep(
            id = "step-1",
            stepNumber = 1,
            instruction = "Mix ingredients"
        )
        
        viewModel.addStep(step)
        
        viewModel.state.value.steps.size shouldBe 1
        viewModel.state.value.steps[0] shouldBe step
    }
    
    test("should remove cooking step") {
        val step1 = CookingStep(id = "step-1", stepNumber = 1, instruction = "Step 1")
        val step2 = CookingStep(id = "step-2", stepNumber = 2, instruction = "Step 2")
        
        viewModel.addStep(step1)
        viewModel.addStep(step2)
        
        viewModel.state.value.steps.size shouldBe 2
        
        viewModel.removeStep("step-1")
        
        viewModel.state.value.steps.size shouldBe 1
        viewModel.state.value.steps[0].id shouldBe "step-2"
    }
    
    test("should add tag") {
        viewModel.addTag("easy")
        
        viewModel.state.value.tags.size shouldBe 1
        viewModel.state.value.tags[0] shouldBe "easy"
    }
    
    test("should not add duplicate tag") {
        viewModel.addTag("easy")
        viewModel.addTag("easy")
        
        viewModel.state.value.tags.size shouldBe 1
    }
    
    test("should not add blank tag") {
        viewModel.addTag("  ")
        
        viewModel.state.value.tags.size shouldBe 0
    }
    
    test("should remove tag") {
        viewModel.addTag("easy")
        viewModel.addTag("quick")
        
        viewModel.state.value.tags.size shouldBe 2
        
        viewModel.removeTag("easy")
        
        viewModel.state.value.tags.size shouldBe 1
        viewModel.state.value.tags[0] shouldBe "quick"
    }
    
    test("should save new recipe successfully") {
        // Set up recipe data
        viewModel.updateTitle("Test Recipe")
        viewModel.updateDescription("A test recipe")
        viewModel.updatePreparationTime(10)
        viewModel.updateCookingTime(20)
        viewModel.updateServings(4)
        
        viewModel.addIngredient(
            Ingredient(id = "ing-1", name = "Flour", quantity = 2.0, unit = "cups")
        )
        
        viewModel.addStep(
            CookingStep(id = "step-1", stepNumber = 1, instruction = "Mix ingredients")
        )
        
        viewModel.addTag("easy")
        
        // Save recipe
        viewModel.saveRecipe()
        
        // Wait for async operation
        delay(100.milliseconds)
        
        // Verify save success
        viewModel.state.value.saveSuccess shouldBe true
        viewModel.state.value.isSaving shouldBe false
        viewModel.state.value.error shouldBe null
    }
    
    test("should fail validation when title is empty") {
        // Set up recipe with empty title
        viewModel.updateTitle("")
        viewModel.addIngredient(
            Ingredient(id = "ing-1", name = "Flour", quantity = 2.0, unit = "cups")
        )
        viewModel.addStep(
            CookingStep(id = "step-1", stepNumber = 1, instruction = "Mix")
        )
        
        // Try to save
        viewModel.saveRecipe()
        
        // Wait for validation
        delay(50.milliseconds)
        
        // Verify validation error
        viewModel.state.value.validationErrors.isNotEmpty() shouldBe true
        viewModel.state.value.saveSuccess shouldBe false
    }
    
    test("should fail validation when no ingredients") {
        // Set up recipe without ingredients
        viewModel.updateTitle("Test Recipe")
        viewModel.addStep(
            CookingStep(id = "step-1", stepNumber = 1, instruction = "Mix")
        )
        
        // Try to save
        viewModel.saveRecipe()
        
        // Wait for validation
        delay(50.milliseconds)
        
        // Verify validation error
        viewModel.state.value.validationErrors.isNotEmpty() shouldBe true
        viewModel.state.value.saveSuccess shouldBe false
    }
    
    test("should fail validation when no cooking steps") {
        // Set up recipe without steps
        viewModel.updateTitle("Test Recipe")
        viewModel.addIngredient(
            Ingredient(id = "ing-1", name = "Flour", quantity = 2.0, unit = "cups")
        )
        
        // Try to save
        viewModel.saveRecipe()
        
        // Wait for validation
        delay(50.milliseconds)
        
        // Verify validation error
        viewModel.state.value.validationErrors.isNotEmpty() shouldBe true
        viewModel.state.value.saveSuccess shouldBe false
    }
    
    test("should load existing recipe for editing") {
        // Create a recipe first
        val recipe = Recipe(
            id = "recipe-1",
            title = "Existing Recipe",
            description = "An existing recipe",
            ingredients = listOf(
                Ingredient(id = "ing-1", name = "Flour", quantity = 2.0, unit = "cups")
            ),
            steps = listOf(
                CookingStep(id = "step-1", stepNumber = 1, instruction = "Mix")
            ),
            preparationTime = 15,
            cookingTime = 30,
            servings = 4,
            tags = listOf("easy"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        recipeRepository.createRecipe(recipe)
        
        // Load recipe for editing
        viewModel.loadRecipe("recipe-1")
        
        // Wait for async operation
        delay(100.milliseconds)
        
        // Verify state is populated
        val state = viewModel.state.value
        state.recipeId shouldBe "recipe-1"
        state.title shouldBe "Existing Recipe"
        state.description shouldBe "An existing recipe"
        state.ingredients.size shouldBe 1
        state.steps.size shouldBe 1
        state.preparationTime shouldBe 15
        state.cookingTime shouldBe 30
        state.servings shouldBe 4
        state.tags shouldBe listOf("easy")
    }
    
    test("should clear error message") {
        // Trigger validation error
        viewModel.updateTitle("")
        viewModel.saveRecipe()
        delay(50.milliseconds)
        
        // Verify error exists
        viewModel.state.value.validationErrors.isNotEmpty() shouldBe true
        
        // Clear error
        viewModel.clearError()
        
        // Verify error is cleared
        viewModel.state.value.error shouldBe null
    }
    
    test("should reset save success flag") {
        // Set up and save a valid recipe
        viewModel.updateTitle("Test Recipe")
        viewModel.addIngredient(
            Ingredient(id = "ing-1", name = "Flour", quantity = 2.0, unit = "cups")
        )
        viewModel.addStep(
            CookingStep(id = "step-1", stepNumber = 1, instruction = "Mix")
        )
        viewModel.saveRecipe()
        delay(100.milliseconds)
        
        // Verify save success
        viewModel.state.value.saveSuccess shouldBe true
        
        // Reset save success
        viewModel.resetSaveSuccess()
        
        // Verify flag is reset
        viewModel.state.value.saveSuccess shouldBe false
    }
})
