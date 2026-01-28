package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.CookingStep
import com.recipemanager.domain.model.CookingTimer
import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.model.TimerStatus
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.domain.service.TimerService
import com.recipemanager.presentation.navigation.StatePersistence
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CookingModeState(
    val recipeId: String? = null,
    val recipe: Recipe? = null,
    val currentStepIndex: Int = 0,
    val activeTimers: Map<String, CookingTimer> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCookingSessionActive: Boolean = false,
    val sessionStartTime: Long = 0L,
    val completedSteps: Set<Int> = emptySet()
)

/**
 * ViewModel for cooking mode interface.
 * Manages cooking session state, step navigation, and timer controls.
 * Requirements: 5.4, 5.5, 7.2, 7.3
 */
class CookingModeViewModel(
    private val recipeRepository: RecipeRepository,
    private val timerService: TimerService,
    statePersistence: StatePersistence? = null
) : BaseViewModel<CookingModeState>(
    initialState = CookingModeState(),
    statePersistence = statePersistence,
    stateKey = "cooking_mode"
) {

    override fun onInitialize() {
        // Observe active timers from TimerService
        viewModelScope.launch {
            timerService.activeTimers.collect { timers ->
                currentState = currentState.copy(activeTimers = timers)
            }
        }
        
        // If we have a cooking session in progress, restore it
        if (currentState.isCookingSessionActive && currentState.recipeId != null) {
            loadRecipeForSession(currentState.recipeId!!)
        }
    }

    /**
     * Load recipe and start cooking session.
     * Requirement 5.4: Start cooking session
     */
    fun startCookingSession(recipeId: String) {
        currentState = currentState.copy(recipeId = recipeId)
        loadRecipeForSession(recipeId)
    }
    
    private fun loadRecipeForSession(recipeId: String) {
        viewModelScope.launch {
            setLoading(true)
            setError(null)
            
            recipeRepository.getRecipe(recipeId)
                .onSuccess { recipe ->
                    currentState = currentState.copy(
                        recipe = recipe,
                        currentStepIndex = 0,
                        isLoading = false,
                        isCookingSessionActive = true,
                        sessionStartTime = System.currentTimeMillis()
                    )
                    setLoading(false)
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to load recipe")
                    setLoading(false)
                }
        }
    }

    /**
     * Navigate to the next cooking step.
     * Requirement 7.2: Navigate through steps
     */
    fun nextStep() {
        val recipe = currentState.recipe ?: return
        val currentIndex = currentState.currentStepIndex
        
        if (currentIndex < recipe.steps.size - 1) {
            currentState = currentState.copy(
                currentStepIndex = currentIndex + 1,
                completedSteps = currentState.completedSteps + currentIndex
            )
        }
    }

    /**
     * Navigate to the previous cooking step.
     * Requirement 7.2: Navigate through steps
     */
    fun previousStep() {
        val currentIndex = currentState.currentStepIndex
        
        if (currentIndex > 0) {
            currentState = currentState.copy(currentStepIndex = currentIndex - 1)
        }
    }

    /**
     * Jump to a specific step.
     * Requirement 7.2: Navigate through steps
     */
    fun goToStep(stepIndex: Int) {
        val recipe = currentState.recipe ?: return
        
        if (stepIndex in 0 until recipe.steps.size) {
            currentState = currentState.copy(currentStepIndex = stepIndex)
        }
    }
    
    /**
     * Mark current step as completed.
     */
    fun markCurrentStepCompleted() {
        val currentIndex = currentState.currentStepIndex
        currentState = currentState.copy(
            completedSteps = currentState.completedSteps + currentIndex
        )
    }

    /**
     * Start a timer for the current step.
     * Requirement 5.1: Create timers for timed cooking steps
     */
    fun startStepTimer(step: CookingStep) {
        val recipe = currentState.recipe ?: return
        val duration = step.duration ?: return
        
        viewModelScope.launch {
            val timer = CookingTimer(
                id = "${recipe.id}_${step.id}_${Clock.System.now().toEpochMilliseconds()}",
                recipeId = recipe.id,
                stepId = step.id,
                duration = duration * 60, // Convert minutes to seconds
                remainingTime = duration * 60,
                status = TimerStatus.RUNNING,
                createdAt = Clock.System.now()
            )
            
            timerService.startTimer(timer)
                .onFailure { error ->
                    setError(error.message ?: "Failed to start timer")
                }
        }
    }

    /**
     * Pause a running timer.
     * Requirement 5.4: Pause timers
     */
    fun pauseTimer(timerId: String) {
        viewModelScope.launch {
            timerService.pauseTimer(timerId)
                .onFailure { error ->
                    setError(error.message ?: "Failed to pause timer")
                }
        }
    }

    /**
     * Resume a paused timer.
     * Requirement 5.4: Resume timers
     */
    fun resumeTimer(timerId: String) {
        viewModelScope.launch {
            timerService.resumeTimer(timerId)
                .onFailure { error ->
                    setError(error.message ?: "Failed to resume timer")
                }
        }
    }

    /**
     * Cancel a timer.
     * Requirement 5.4: Cancel timers
     */
    fun cancelTimer(timerId: String) {
        viewModelScope.launch {
            timerService.cancelTimer(timerId)
                .onFailure { error ->
                    setError(error.message ?: "Failed to cancel timer")
                }
        }
    }

    /**
     * End the cooking session.
     * Requirement 5.4: End cooking session
     */
    fun endCookingSession() {
        // Cancel all active timers
        viewModelScope.launch {
            currentState.activeTimers.keys.forEach { timerId ->
                timerService.cancelTimer(timerId)
            }
            
            currentState = CookingModeState()
        }
    }

    /**
     * Get the current step.
     */
    fun getCurrentStep(): CookingStep? {
        val recipe = currentState.recipe ?: return null
        val index = currentState.currentStepIndex
        
        return recipe.steps.sortedBy { it.stepNumber }.getOrNull(index)
    }

    /**
     * Get timers for the current step.
     * Requirement 5.5: Display timers for current step
     */
    fun getTimersForCurrentStep(): List<CookingTimer> {
        val currentStep = getCurrentStep() ?: return emptyList()
        
        return currentState.activeTimers.values.filter { timer ->
            timer.stepId == currentStep.id
        }
    }
    
    /**
     * Get cooking session duration in milliseconds.
     */
    fun getSessionDuration(): Long {
        return if (currentState.isCookingSessionActive) {
            System.currentTimeMillis() - currentState.sessionStartTime
        } else {
            0L
        }
    }
    
    /**
     * Check if all steps are completed.
     */
    fun isRecipeCompleted(): Boolean {
        val recipe = currentState.recipe ?: return false
        return currentState.completedSteps.size >= recipe.steps.size
    }
    
    override fun serializeState(state: CookingModeState): String? {
        return try {
            Json.encodeToString(state)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun deserializeState(serializedState: String): CookingModeState? {
        return try {
            Json.decodeFromString<CookingModeState>(serializedState)
        } catch (e: Exception) {
            null
        }
    }
}
