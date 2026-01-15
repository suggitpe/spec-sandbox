package com.recipemanager.domain.service

import com.recipemanager.domain.model.CookingTimer

/**
 * Enum representing different types of notifications.
 */
enum class NotificationType {
    TIMER_COMPLETED,
    TIMER_WARNING,
    COOKING_REMINDER,
    GENERAL
}

/**
 * Data class representing a notification to be displayed.
 */
data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timerId: String? = null,
    val recipeId: String? = null
)

/**
 * Platform-specific interface for managing notifications.
 * Implementations should be provided for each platform (Android, iOS, JVM).
 */
interface NotificationService {
    /**
     * Shows a notification to the user.
     * 
     * @param notification The notification to display
     * @return Result indicating success or failure
     */
    suspend fun showNotification(notification: Notification): Result<Unit>
    
    /**
     * Shows a notification when a timer completes.
     * 
     * @param timer The completed timer
     * @param recipeName Optional recipe name for context
     * @return Result indicating success or failure
     */
    suspend fun showTimerCompletedNotification(
        timer: CookingTimer,
        recipeName: String? = null
    ): Result<Unit>
    
    /**
     * Shows a notification as a cooking reminder.
     * 
     * @param title The reminder title
     * @param message The reminder message
     * @param recipeId Optional recipe ID for context
     * @return Result indicating success or failure
     */
    suspend fun showCookingReminder(
        title: String,
        message: String,
        recipeId: String? = null
    ): Result<Unit>
    
    /**
     * Cancels a notification by ID.
     * 
     * @param notificationId The ID of the notification to cancel
     * @return Result indicating success or failure
     */
    suspend fun cancelNotification(notificationId: String): Result<Unit>
    
    /**
     * Cancels all notifications.
     * 
     * @return Result indicating success or failure
     */
    suspend fun cancelAllNotifications(): Result<Unit>
    
    /**
     * Checks if notifications are enabled/permitted on the platform.
     * 
     * @return true if notifications are enabled, false otherwise
     */
    fun areNotificationsEnabled(): Boolean
    
    /**
     * Requests notification permissions from the user (if required by platform).
     * 
     * @return Result indicating if permission was granted
     */
    suspend fun requestNotificationPermission(): Result<Boolean>
}
