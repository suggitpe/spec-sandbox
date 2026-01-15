package com.recipemanager.domain.service

import com.recipemanager.domain.model.CookingTimer
import com.recipemanager.domain.model.Recipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * High-level manager for coordinating notifications and cooking reminders.
 * Provides a simplified API for common notification scenarios.
 */
class NotificationManager(
    private val notificationService: NotificationService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    
    private val scheduledReminders = mutableMapOf<String, kotlinx.coroutines.Job>()
    
    /**
     * Initialize the notification manager and request permissions if needed.
     */
    suspend fun initialize(): Result<Boolean> {
        return notificationService.requestNotificationPermission()
    }
    
    /**
     * Sends a notification when a timer completes.
     * 
     * @param timer The completed timer
     * @param recipe Optional recipe for additional context
     * @return Result indicating success or failure
     */
    suspend fun notifyTimerCompleted(
        timer: CookingTimer,
        recipe: Recipe? = null
    ): Result<Unit> {
        return notificationService.showTimerCompletedNotification(
            timer = timer,
            recipeName = recipe?.title
        )
    }
    
    /**
     * Sends a notification when a timer is about to complete (warning).
     * 
     * @param timer The timer that is about to complete
     * @param secondsRemaining Seconds remaining on the timer
     * @param recipe Optional recipe for additional context
     * @return Result indicating success or failure
     */
    suspend fun notifyTimerWarning(
        timer: CookingTimer,
        secondsRemaining: Int,
        recipe: Recipe? = null
    ): Result<Unit> {
        val title = "Timer Warning"
        val message = buildString {
            if (recipe != null) {
                append("${recipe.title}: ")
            }
            append("$secondsRemaining seconds remaining")
        }
        
        val notification = Notification(
            id = "timer_warning_${timer.id}",
            title = title,
            message = message,
            type = NotificationType.TIMER_WARNING,
            timerId = timer.id,
            recipeId = timer.recipeId
        )
        
        return notificationService.showNotification(notification)
    }
    
    /**
     * Schedules a cooking reminder for a specific time.
     * 
     * @param reminderId Unique identifier for the reminder
     * @param title The reminder title
     * @param message The reminder message
     * @param scheduledTime When to show the reminder
     * @param recipeId Optional recipe ID for context
     * @return Result indicating success or failure
     */
    suspend fun scheduleCookingReminder(
        reminderId: String,
        title: String,
        message: String,
        scheduledTime: Instant,
        recipeId: String? = null
    ): Result<Unit> {
        return try {
            val now = Clock.System.now()
            val delay = (scheduledTime - now).inWholeMilliseconds
            
            if (delay <= 0) {
                // Time has already passed, show immediately
                return notificationService.showCookingReminder(title, message, recipeId)
            }
            
            // Cancel any existing reminder with the same ID
            scheduledReminders[reminderId]?.cancel()
            
            // Schedule the reminder
            val job = scope.launch {
                kotlinx.coroutines.delay(delay)
                notificationService.showCookingReminder(title, message, recipeId)
                scheduledReminders.remove(reminderId)
            }
            
            scheduledReminders[reminderId] = job
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to schedule cooking reminder: ${e.message}", e))
        }
    }
    
    /**
     * Schedules a cooking reminder for a specific duration from now.
     * 
     * @param reminderId Unique identifier for the reminder
     * @param title The reminder title
     * @param message The reminder message
     * @param delay Duration to wait before showing the reminder
     * @param recipeId Optional recipe ID for context
     * @return Result indicating success or failure
     */
    suspend fun scheduleCookingReminderIn(
        reminderId: String,
        title: String,
        message: String,
        delay: Duration,
        recipeId: String? = null
    ): Result<Unit> {
        val scheduledTime = Clock.System.now() + delay
        return scheduleCookingReminder(reminderId, title, message, scheduledTime, recipeId)
    }
    
    /**
     * Cancels a scheduled cooking reminder.
     * 
     * @param reminderId The ID of the reminder to cancel
     * @return Result indicating success or failure
     */
    suspend fun cancelCookingReminder(reminderId: String): Result<Unit> {
        return try {
            scheduledReminders[reminderId]?.cancel()
            scheduledReminders.remove(reminderId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to cancel cooking reminder: ${e.message}", e))
        }
    }
    
    /**
     * Cancels all scheduled cooking reminders.
     * 
     * @return Result indicating success or failure
     */
    suspend fun cancelAllCookingReminders(): Result<Unit> {
        return try {
            scheduledReminders.values.forEach { it.cancel() }
            scheduledReminders.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to cancel all cooking reminders: ${e.message}", e))
        }
    }
    
    /**
     * Sends a general notification.
     * 
     * @param title The notification title
     * @param message The notification message
     * @return Result indicating success or failure
     */
    suspend fun sendNotification(
        title: String,
        message: String
    ): Result<Unit> {
        val notification = Notification(
            id = "general_${System.currentTimeMillis()}",
            title = title,
            message = message,
            type = NotificationType.GENERAL
        )
        
        return notificationService.showNotification(notification)
    }
    
    /**
     * Checks if notifications are enabled.
     * 
     * @return true if notifications are enabled, false otherwise
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationService.areNotificationsEnabled()
    }
    
    /**
     * Cleans up resources when the manager is no longer needed.
     */
    fun shutdown() {
        scheduledReminders.values.forEach { it.cancel() }
        scheduledReminders.clear()
    }
}
