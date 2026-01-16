package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.CookingStep
import com.recipemanager.domain.model.CookingTimer
import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.model.TimerStatus
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.domain.service.TimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class CookingModeState(
    val recipe: Recipe? = null,
    val currentStepIndex: Int = 0,
    val activeTimers: Map<String, CookingTimer> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCookingSessionActive: Boolean = false
)

/**
 * ViewModel for cooking mode interface.
 * Manages cooking session state, step navigation, and timer controls.
 * Requirements: 5.4, 5.5, 7.2, 7.3
 */
class CookingModeViewModel(
    private val recipeRepository: RecipeRepository,
    private val timerService: TimerService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(CookingModeState())
    val state: StateFlow<CookingModeState> = _state.asStateFlow()

    init {
        // Observe active timers from TimerService
        scope.launch {
            timerService.activeTimers.collect { timers ->
                _state.value = _state.value.copy(activeTimers = timers)
            }
        }
    }

    /**
     * Load recipe and start cooking session.
     * Requirement 5.4: Start cooking session
     */
    fun startCookingSession(recipeId: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            recipeRepository.getRecipe(recipeId)
                .onSuccess { recipe ->
                    _state.value = _state.value.copy(
                        recipe = recipe,
                        currentStepIndex = 0,
                        isLoading = false,
                        isCookingSessionActive = true
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load recipe"
                    )
                }
        }
    }

    /**
     * Navigate to the next cooking step.
     * Requirement 7.2: Navigate through steps
     */
    fun nextStep() {
        val recipe = _state.value.recipe ?: return
        val currentIndex = _state.value.currentStepIndex
        
        if (currentIndex < recipe.steps.size - 1) {
            _state.value = _state.value.copy(currentStepIndex = currentIndex + 1)
        }
    }

    /**
     * Navigate to the previous cooking step.
     * Requirement 7.2: Navigate through steps
     */
    fun previousStep() {
        val currentIndex = _state.value.currentStepIndex
        
        if (currentIndex > 0) {
            _state.value = _state.value.copy(currentStepIndex = currentIndex - 1)
        }
    }

    /**
     * Jump to a specific step.
     * Requirement 7.2: Navigate through steps
     */
    fun goToStep(stepIndex: Int) {
        val recipe = _state.value.recipe ?: return
        
        if (stepIndex in 0 until recipe.steps.size) {
            _state.value = _state.value.copy(currentStepIndex = stepIndex)
        }
    }

    /**
     * Start a timer for the current step.
     * Requirement 5.1: Create timers for timed cooking steps
     */
    fun startStepTimer(step: CookingStep) {
        val recipe = _state.value.recipe ?: return
        val duration = step.duration ?: return
        
        scope.launch {
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
                    _state.value = _state.value.copy(
                        error = error.message ?: "Failed to start timer"
                    )
                }
        }
    }

    /**
     * Pause a running timer.
     * Requirement 5.4: Pause timers
     */
    fun pauseTimer(timerId: String) {
        scope.launch {
            timerService.pauseTimer(timerId)
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: "Failed to pause timer"
                    )
                }
        }
    }

    /**
     * Resume a paused timer.
     * Requirement 5.4: Resume timers
     */
    fun resumeTimer(timerId: String) {
        scope.launch {
            timerService.resumeTimer(timerId)
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: "Failed to resume timer"
                    )
                }
        }
    }

    /**
     * Cancel a timer.
     * Requirement 5.4: Cancel timers
     */
    fun cancelTimer(timerId: String) {
        scope.launch {
            timerService.cancelTimer(timerId)
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: "Failed to cancel timer"
                    )
                }
        }
    }

    /**
     * End the cooking session.
     * Requirement 5.4: End cooking session
     */
    fun endCookingSession() {
        // Cancel all active timers
        scope.launch {
            _state.value.activeTimers.keys.forEach { timerId ->
                timerService.cancelTimer(timerId)
            }
            
            _state.value = CookingModeState()
        }
    }

    /**
     * Get the current step.
     */
    fun getCurrentStep(): CookingStep? {
        val recipe = _state.value.recipe ?: return null
        val index = _state.value.currentStepIndex
        
        return recipe.steps.sortedBy { it.stepNumber }.getOrNull(index)
    }

    /**
     * Get timers for the current step.
     * Requirement 5.5: Display timers for current step
     */
    fun getTimersForCurrentStep(): List<CookingTimer> {
        val currentStep = getCurrentStep() ?: return emptyList()
        
        return _state.value.activeTimers.values.filter { timer ->
            timer.stepId == currentStep.id
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
