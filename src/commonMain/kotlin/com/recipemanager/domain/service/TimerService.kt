package com.recipemanager.domain.service

import com.recipemanager.domain.model.CookingTimer
import com.recipemanager.domain.model.TimerStatus
import com.recipemanager.domain.repository.TimerRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Service for managing multiple concurrent cooking timers.
 * Handles timer lifecycle (start, pause, resume, cancel) and persistence.
 * Integrates with NotificationService to send alerts when timers complete.
 */
class TimerService(
    private val timerRepository: TimerRepository,
    private val notificationService: NotificationService? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    
    // Map of timer ID to its coroutine job
    private val activeTimerJobs = mutableMapOf<String, Job>()
    
    // State flow for active timers
    private val _activeTimers = MutableStateFlow<Map<String, CookingTimer>>(emptyMap())
    val activeTimers: StateFlow<Map<String, CookingTimer>> = _activeTimers.asStateFlow()
    
    /**
     * Initialize the service by restoring any persisted timers.
     * Should be called when the app starts.
     */
    suspend fun initialize() {
        val result = timerRepository.getActiveTimers()
        result.onSuccess { timers ->
            timers.forEach { timer ->
                when (timer.status) {
                    TimerStatus.RUNNING -> {
                        // Restart running timers
                        startTimerCountdown(timer)
                    }
                    TimerStatus.PAUSED -> {
                        // Keep paused timers in state but don't start countdown
                        _activeTimers.value = _activeTimers.value + (timer.id to timer)
                    }
                    else -> {
                        // Ignore completed or cancelled timers
                    }
                }
            }
        }
    }
    
    /**
     * Start a new timer.
     * @param timer The timer to start
     * @return Result containing the started timer or an error
     */
    suspend fun startTimer(timer: CookingTimer): Result<CookingTimer> {
        // Ensure timer is in READY or RUNNING state
        val timerToStart = timer.copy(status = TimerStatus.RUNNING)
        
        // Persist to database
        val createResult = timerRepository.createTimer(timerToStart)
        if (createResult.isFailure) {
            return createResult
        }
        
        // Start countdown
        startTimerCountdown(timerToStart)
        
        return Result.success(timerToStart)
    }
    
    /**
     * Pause a running timer.
     * @param timerId The ID of the timer to pause
     * @return Result containing the paused timer or an error
     */
    suspend fun pauseTimer(timerId: String): Result<CookingTimer> {
        val currentTimer = _activeTimers.value[timerId]
            ?: return Result.failure(IllegalArgumentException("Timer not found: $timerId"))
        
        if (currentTimer.status != TimerStatus.RUNNING) {
            return Result.failure(IllegalStateException("Timer is not running: $timerId"))
        }
        
        // Cancel the countdown job
        activeTimerJobs[timerId]?.cancel()
        activeTimerJobs.remove(timerId)
        
        // Update timer status
        val pausedTimer = currentTimer.copy(status = TimerStatus.PAUSED)
        
        // Persist to database
        val updateResult = timerRepository.updateTimer(pausedTimer)
        if (updateResult.isFailure) {
            return updateResult
        }
        
        // Update state
        _activeTimers.value = _activeTimers.value + (timerId to pausedTimer)
        
        return Result.success(pausedTimer)
    }
    
    /**
     * Resume a paused timer.
     * @param timerId The ID of the timer to resume
     * @return Result containing the resumed timer or an error
     */
    suspend fun resumeTimer(timerId: String): Result<CookingTimer> {
        val currentTimer = _activeTimers.value[timerId]
            ?: return Result.failure(IllegalArgumentException("Timer not found: $timerId"))
        
        if (currentTimer.status != TimerStatus.PAUSED) {
            return Result.failure(IllegalStateException("Timer is not paused: $timerId"))
        }
        
        // Update timer status
        val resumedTimer = currentTimer.copy(status = TimerStatus.RUNNING)
        
        // Persist to database
        val updateResult = timerRepository.updateTimer(resumedTimer)
        if (updateResult.isFailure) {
            return updateResult
        }
        
        // Start countdown
        startTimerCountdown(resumedTimer)
        
        return Result.success(resumedTimer)
    }
    
    /**
     * Cancel a timer.
     * @param timerId The ID of the timer to cancel
     * @return Result containing Unit or an error
     */
    suspend fun cancelTimer(timerId: String): Result<Unit> {
        val currentTimer = _activeTimers.value[timerId]
            ?: return Result.failure(IllegalArgumentException("Timer not found: $timerId"))
        
        // Cancel the countdown job if running
        activeTimerJobs[timerId]?.cancel()
        activeTimerJobs.remove(timerId)
        
        // Update timer status
        val cancelledTimer = currentTimer.copy(
            status = TimerStatus.CANCELLED,
            remainingTime = 0
        )
        
        // Persist to database
        val updateResult = timerRepository.updateTimer(cancelledTimer)
        if (updateResult.isFailure) {
            return Result.failure(updateResult.exceptionOrNull()!!)
        }
        
        // Remove from active timers
        _activeTimers.value = _activeTimers.value - timerId
        
        return Result.success(Unit)
    }
    
    /**
     * Get a specific timer by ID.
     * @param timerId The ID of the timer
     * @return Result containing the timer or an error
     */
    suspend fun getTimer(timerId: String): Result<CookingTimer?> {
        // Check active timers first
        _activeTimers.value[timerId]?.let {
            return Result.success(it)
        }
        
        // Fall back to database
        return timerRepository.getTimer(timerId)
    }
    
    /**
     * Get all active timers (running or paused).
     * @return Map of timer ID to timer
     */
    fun getActiveTimers(): Map<String, CookingTimer> {
        return _activeTimers.value
    }
    
    /**
     * Start the countdown for a timer.
     * Updates the timer state every second and persists to database.
     */
    private fun startTimerCountdown(timer: CookingTimer) {
        // Cancel any existing job for this timer
        activeTimerJobs[timer.id]?.cancel()
        
        // Add to active timers
        _activeTimers.value = _activeTimers.value + (timer.id to timer)
        
        // Start countdown job
        val job = scope.launch {
            var currentTimer = timer
            
            while (currentTimer.remainingTime > 0 && isActive) {
                delay(1.seconds)
                
                // Decrement remaining time
                currentTimer = currentTimer.copy(
                    remainingTime = (currentTimer.remainingTime - 1).coerceAtLeast(0)
                )
                
                // Update state
                _activeTimers.value = _activeTimers.value + (timer.id to currentTimer)
                
                // Persist to database every 5 seconds or when completed
                if (currentTimer.remainingTime % 5 == 0 || currentTimer.remainingTime == 0) {
                    timerRepository.updateTimer(currentTimer)
                }
            }
            
            // Timer completed
            if (currentTimer.remainingTime == 0) {
                val completedTimer = currentTimer.copy(status = TimerStatus.COMPLETED)
                timerRepository.updateTimer(completedTimer)
                
                // Send notification
                notificationService?.showTimerCompletedNotification(completedTimer)
                
                _activeTimers.value = _activeTimers.value - timer.id
                activeTimerJobs.remove(timer.id)
            }
        }
        
        activeTimerJobs[timer.id] = job
    }
    
    /**
     * Clean up resources when the service is no longer needed.
     */
    fun shutdown() {
        // Cancel all active timer jobs
        activeTimerJobs.values.forEach { it.cancel() }
        activeTimerJobs.clear()
        
        // Cancel the scope
        scope.cancel()
    }
}
