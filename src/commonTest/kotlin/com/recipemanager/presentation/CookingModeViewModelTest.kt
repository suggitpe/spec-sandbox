package com.recipemanager.presentation

import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.database.DatabaseManager
import com.recipemanager.data.repository.RecipeRepositoryImpl
import com.recipemanager.data.repository.TimerRepositoryImpl
import com.recipemanager.domain.model.CookingStep
import com.recipemanager.domain.model.Ingredient
import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.model.TimerStatus
import com.recipemanager.domain.service.TimerService
import com.recipemanager.presentation.viewmodel.CookingModeViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tests for CookingModeViewModel.
 * Validates cooking session management, step navigation, and timer controls.
 * Requirements: 5.4, 5.5, 7.2, 7.3
 */
class CookingModeViewModelTest : FunSpec({
    
    lateinit var databaseManager: DatabaseManager
    lateinit var recipeRepository: RecipeRepositoryImpl
    lateinit var timerRepository: TimerRepositoryImpl
    lateinit var timerService: TimerService
    lateinit var viewModel: CookingModeViewModel
    
    beforeEach {
        val driverFactory = DatabaseDriverFactory()
        databaseManager = DatabaseManager(driverFactory)
        databaseManager.initialize()
        recipeRepository = RecipeRepositoryImpl(databaseManager.getDatabase())
        timerRepository = TimerRepositoryImpl(databaseManager.getDatabase())
        timerService = TimerService(timerRepository)
        viewModel = CookingModeViewModel(recipeRepository, timerService)
    }
    
    afterEach {
        timerService.shutdown()
        databaseManager.close()
    }
    
    test("should start cooking session and load recipe") {
        // Create a test recipe
        val recipe = Recipe(
            id = "cooking-recipe-1",
            title = "Pasta Carbonara",
            description = "Classic Italian pasta",
            ingredients = listOf(
                Ingredient(id = "ing-1", name = "Spaghetti", quantity = 400.0, unit = "g"),
                Ingredient(id = "ing-2", name = "Eggs", quantity = 4.0, unit = "pieces")
            ),
            steps = listOf(
                CookingStep(
                    id = "step-1",
                    stepNumber = 1,
                    instruction = "Boil water and cook pasta",
                    duration = 10
                ),
                CookingStep(
                    id = "step-2",
                    stepNumber = 2,
                    instruction = "Mix eggs and cheese",
                    duration = 5
                ),
                CookingStep(
                    id = "step-3",
                    stepNumber = 3,
                    instruction = "Combine pasta with egg mixture",
                    duration = 2
                )
            ),
            preparationTime = 10,
            cookingTime = 20,
            servings = 4,
            tags = listOf("pasta", "italian"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        recipeRepository.createRecipe(recipe)
        
        // Start cooking session
        viewModel.startCookingSession("cooking-recipe-1")
        
        // Wait for async operation
        delay(100.milliseconds)
        
        // Verify state
        val state = viewModel.state.value
        state.isLoading shouldBe false
        state.recipe shouldNotBe null
        state.recipe?.id shouldBe "cooking-recipe-1"
        state.currentStepIndex shouldBe 0
        state.isCookingSessionActive shouldBe true
        state.error shouldBe null
    }
    
    test("should navigate to next step") {
        // Create and load recipe
        val recipe = Recipe(
            id = "nav-recipe-1",
            title = "Test Recipe",
            ingredients = listOf(
                Ingredient(id = "ing-1", name = "Ingredient", quantity = 1.0, unit = "unit")
            ),
            steps = listOf(
                CookingStep(id = "step-1", stepNumber = 1, instruction = "Step 1"),
                CookingStep(id = "step-2", stepNumber = 2, instruction = "Step 2"),
                CookingStep(id = "step-3", stepNumber = 3, instruction = "Step 3")
            ),
            preparationTime = 5,
            cookingTime = 10,
            servings = 2,
            tags = emptyList(),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        recipeRepository.createRecipe(recipe)
        viewModel.startCookingSession("nav-recipe-1")
        delay(100.milliseconds)
        
        // Navigate to next step
        viewModel.nextStep()
        
        // Verify step index updated
        viewModel.state.value.currentStepIndex shouldBe 1
        
        // Navigate again
        viewModel.nextStep()
        viewModel.state.value.currentStepIndex shouldBe 2
        
        // Try to go beyond last step (should not change)
        viewModel.nextStep()
        viewModel.state.value.currentStepIndex shouldBe 2
    }
    
    test("should navigate to previous step") {
        // Create and load recipe
        val recipe = Recipe(
            id = "nav-recipe-2",
            title = "Test Recipe",
            ingredients = listOf(
                Ingredient(id = "ing-1", name = "Ingredient", quantity = 1.0, unit = "unit")
            ),
            steps = listOf(
                CookingStep(id = "step-1", stepNumber = 1, instruction = "Step 1"),
                CookingStep(id = "step-2", stepNumber = 2, instruction = "Step 2"),
                CookingStep(id = "step-3", stepNumber = 3, instruction = "Step 3")
            ),
            preparationTime = 5,
            cookingTime = 10,
            servings = 2,
            tags = emptyList(),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        recipeRepository.createRecipe(recipe)
        viewModel.startCookingSession("nav-recipe-2")
        delay(100.milliseconds)
        
        // Navigate to step 2
        viewModel.goToStep(2)
        viewModel.state.value.currentStepIndex shouldBe 2
        
        // Navigate to previous step
        viewModel.previousStep()
        viewModel.state.value.currentStepIndex shouldBe 1
        
        // Navigate again
        viewModel.previousStep()
        viewModel.state.value.currentStepIndex shouldBe 0
        
        // Try to go before first step (should not change)
        viewModel.previousStep()
        viewModel.state.value.currentStepIndex shouldBe 0
    }
    
    test("should jump to specific step") {
        // Create and load recipe
        val recipe = Recipe(
            id = "nav-recipe-3",
            title = "Test Recipe",
            ingredients = listOf(
                Ingredient(id = "ing-1", name = "Ingredient", quantity = 1.0, unit = "unit")
            ),
            steps = listOf(
                CookingStep(id = "step-1", stepNumber = 1, instruction = "Step 1"),
                CookingStep(id = "step-2", stepNumber = 2, instruction = "Step 2"),
                CookingStep(id = "step-3", stepNumber = 3, instruction = "Step 3")
            ),
            preparationTime = 5,
            cookingTime = 10,
            servings = 2,
            tags = emptyList(),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        recipeRepository.createRecipe(recipe)
        viewModel.startCookingSession("nav-recipe-3")
        delay(100.milliseconds)
        
        // Jump to step 2 (index 1)
        viewModel.goToStep(1)
        viewModel.state.value.currentStepIndex shouldBe 1
        
        // Jump to last step
        viewModel.goToStep(2)
        viewModel.state.value.currentStepIndex shouldBe 2
        
        // Jump to first step
        viewModel.goToStep(0)
        viewModel.state.value.currentStepIndex shouldBe 0
    }
    
    test("should start timer for step with duration") {
        // Create recipe with timed step
        val recipe = Recipe(
            id = "timer-recipe-1",
            title = "Timer Recipe",
            ingredients = listOf(
                Ingredient(id = "ing-1", name = "Ingredient", quantity = 1.0, unit = "unit")
            ),
            steps = listOf(
                CookingStep(
                    id = "step-1",
                    stepNumber = 1,
                    instruction = "Cook for 10 minutes",
                    duration = 10
                )
            ),
            preparationTime = 5,
            cookingTime = 10,
            servings = 2,
            tags = emptyList(),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        recipeRepository.createRecipe(recipe)
        viewModel.startCookingSession("timer-recipe-1")
        delay(100.milliseconds)
        
        val currentStep = viewModel.getCurrentStep()
        currentStep shouldNotBe null
        
        // Start timer for current step
        viewModel.startStepTimer(currentStep!!)
        
        // Wait for timer to be created
        delay(200.milliseconds)
        
        // Verify timer was created
        val state = viewModel.state.value
        state.activeTimers.size shouldBe 1
        
        val timer = state.activeTimers.values.first()
        timer.recipeId shouldBe "timer-recipe-1"
        timer.stepId shouldBe "step-1"
        timer.duration shouldBe 600 // 10 minutes in seconds
        timer.status shouldBe TimerStatus.RUNNING
    }
    
    test("should pause and resume timer") {
        // Create recipe with timed step
        val recipe = Recipe(
            id = "timer-recipe-2",
            title = "Timer Recipe",
            ingredients = listOf(
                Ingredient(id = "ing-1", name = "Ingredient", quantity = 1.0, unit = "unit")
            ),
            steps = listOf(
                CookingStep(
                    id = "step-1",
                    stepNumber = 1,
                    instruction = "Cook for 5 minutes",
                    duration = 5
                )
            ),
            preparationTime = 5,
            cookingTime = 5,
            servings = 2,
            tags = emptyList(),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        recipeRepository.createRecipe(recipe)
        viewModel.startCookingSession("timer-recipe-2")
        delay(100.milliseconds)
        
        val currentStep = viewModel.getCurrentStep()!!
        viewModel.startStepTimer(currentStep)
        delay(200.milliseconds)
        
        val timerId = viewModel.state.value.activeTimers.keys.first()
        
        // Pause timer
        viewModel.pauseTimer(timerId)
        delay(100.milliseconds)
        
        val pausedTimer = viewModel.state.value.activeTimers[timerId]
        pausedTimer?.status shouldBe TimerStatus.PAUSED
        
        // Resume timer
        viewModel.resumeTimer(timerId)
        delay(100.milliseconds)
        
        val resumedTimer = viewModel.state.value.activeTimers[timerId]
        resumedTimer?.status shouldBe TimerStatus.RUNNING
    }
    
    test("should cancel timer") {
        // Create recipe with timed step
        val recipe = Recipe(
            id = "timer-recipe-3",
            title = "Timer Recipe",
            ingredients = listOf(
                Ingredient(id = "ing-1", name = "Ingredient", quantity = 1.0, unit = "unit")
            ),
            steps = listOf(
                CookingStep(
                    id = "step-1",
                    stepNumber = 1,
                    instruction = "Cook for 5 minutes",
                    duration = 5
                )
            ),
            preparationTime = 5,
            cookingTime = 5,
            servings = 2,
            tags = emptyList(),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        recipeRepository.createRecipe(recipe)
        viewModel.startCookingSession("timer-recipe-3")
        delay(100.milliseconds)
        
        val currentStep = viewModel.getCurrentStep()!!
        viewModel.startStepTimer(currentStep)
        delay(200.milliseconds)
        
        val timerId = viewModel.state.value.activeTimers.keys.first()
        
        // Cancel timer
        viewModel.cancelTimer(timerId)
        delay(100.milliseconds)
        
        // Verify timer is removed from active timers
        viewModel.state.value.activeTimers.containsKey(timerId) shouldBe false
    }
    
    test("should end cooking session and cancel all timers") {
        // Create recipe with multiple timed steps
        val recipe = Recipe(
            id = "session-recipe-1",
            title = "Session Recipe",
            ingredients = listOf(
                Ingredient(id = "ing-1", name = "Ingredient", quantity = 1.0, unit = "unit")
            ),
            steps = listOf(
                CookingStep(
                    id = "step-1",
                    stepNumber = 1,
                    instruction = "Cook for 5 minutes",
                    duration = 5
                ),
                CookingStep(
                    id = "step-2",
                    stepNumber = 2,
                    instruction = "Cook for 10 minutes",
                    duration = 10
                )
            ),
            preparationTime = 5,
            cookingTime = 15,
            servings = 2,
            tags = emptyList(),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        recipeRepository.createRecipe(recipe)
        viewModel.startCookingSession("session-recipe-1")
        delay(100.milliseconds)
        
        // Start timer for first step
        val step1 = viewModel.getCurrentStep()!!
        viewModel.startStepTimer(step1)
        delay(200.milliseconds)
        
        // Navigate to second step and start timer
        viewModel.nextStep()
        val step2 = viewModel.getCurrentStep()!!
        viewModel.startStepTimer(step2)
        delay(200.milliseconds)
        
        // Verify multiple timers are active
        viewModel.state.value.activeTimers.size shouldBe 2
        
        // End cooking session
        viewModel.endCookingSession()
        delay(200.milliseconds)
        
        // Verify session ended and all timers cancelled
        val state = viewModel.state.value
        state.recipe shouldBe null
        state.currentStepIndex shouldBe 0
        state.activeTimers.size shouldBe 0
        state.isCookingSessionActive shouldBe false
    }
    
    test("should get current step correctly") {
        // Create recipe
        val recipe = Recipe(
            id = "current-step-recipe",
            title = "Test Recipe",
            ingredients = listOf(
                Ingredient(id = "ing-1", name = "Ingredient", quantity = 1.0, unit = "unit")
            ),
            steps = listOf(
                CookingStep(id = "step-1", stepNumber = 1, instruction = "First step"),
                CookingStep(id = "step-2", stepNumber = 2, instruction = "Second step"),
                CookingStep(id = "step-3", stepNumber = 3, instruction = "Third step")
            ),
            preparationTime = 5,
            cookingTime = 10,
            servings = 2,
            tags = emptyList(),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        recipeRepository.createRecipe(recipe)
        viewModel.startCookingSession("current-step-recipe")
        delay(100.milliseconds)
        
        // Check first step
        var currentStep = viewModel.getCurrentStep()
        currentStep?.stepNumber shouldBe 1
        currentStep?.instruction shouldBe "First step"
        
        // Navigate and check second step
        viewModel.nextStep()
        currentStep = viewModel.getCurrentStep()
        currentStep?.stepNumber shouldBe 2
        currentStep?.instruction shouldBe "Second step"
        
        // Navigate and check third step
        viewModel.nextStep()
        currentStep = viewModel.getCurrentStep()
        currentStep?.stepNumber shouldBe 3
        currentStep?.instruction shouldBe "Third step"
    }
})
