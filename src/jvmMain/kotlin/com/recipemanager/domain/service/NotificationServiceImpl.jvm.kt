package com.recipemanager.domain.service

import com.recipemanager.domain.model.CookingTimer
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Toolkit
import java.awt.Image
import java.awt.image.BufferedImage

/**
 * JVM implementation of NotificationService using system tray notifications.
 * Provides desktop notifications for timer alerts and cooking reminders.
 */
class NotificationServiceImpl : NotificationService {
    
    private val notifications = mutableMapOf<String, TrayIcon>()
    
    override suspend fun showNotification(notification: Notification): Result<Unit> {
        return try {
            if (!SystemTray.isSupported()) {
                // Fallback to console output for testing/headless environments
                println("NOTIFICATION [${notification.type}]: ${notification.title} - ${notification.message}")
                return Result.success(Unit)
            }
            
            val tray = SystemTray.getSystemTray()
            val image = createNotificationImage()
            
            val trayIcon = TrayIcon(image, "Recipe Manager")
            trayIcon.isImageAutoSize = true
            trayIcon.toolTip = "Recipe Manager"
            
            // Add to system tray
            tray.add(trayIcon)
            
            // Show notification
            val messageType = when (notification.type) {
                NotificationType.TIMER_COMPLETED -> TrayIcon.MessageType.INFO
                NotificationType.TIMER_WARNING -> TrayIcon.MessageType.WARNING
                NotificationType.COOKING_REMINDER -> TrayIcon.MessageType.INFO
                NotificationType.GENERAL -> TrayIcon.MessageType.INFO
            }
            
            trayIcon.displayMessage(
                notification.title,
                notification.message,
                messageType
            )
            
            // Store for potential cancellation
            notifications[notification.id] = trayIcon
            
            // Remove from tray after a delay (cleanup)
            kotlinx.coroutines.delay(5000)
            tray.remove(trayIcon)
            notifications.remove(notification.id)
            
            Result.success(Unit)
        } catch (e: Exception) {
            // Fallback to console output
            println("NOTIFICATION [${notification.type}]: ${notification.title} - ${notification.message}")
            println("Note: System tray notification failed: ${e.message}")
            Result.success(Unit) // Don't fail the operation if notification fails
        }
    }
    
    override suspend fun showTimerCompletedNotification(
        timer: CookingTimer,
        recipeName: String?
    ): Result<Unit> {
        val title = "Timer Completed!"
        val message = buildString {
            if (recipeName != null) {
                append("Recipe: $recipeName\n")
            }
            append("Step timer has finished")
        }
        
        val notification = Notification(
            id = "timer_${timer.id}",
            title = title,
            message = message,
            type = NotificationType.TIMER_COMPLETED,
            timerId = timer.id,
            recipeId = timer.recipeId
        )
        
        return showNotification(notification)
    }
    
    override suspend fun showCookingReminder(
        title: String,
        message: String,
        recipeId: String?
    ): Result<Unit> {
        val notification = Notification(
            id = "reminder_${System.currentTimeMillis()}",
            title = title,
            message = message,
            type = NotificationType.COOKING_REMINDER,
            recipeId = recipeId
        )
        
        return showNotification(notification)
    }
    
    override suspend fun cancelNotification(notificationId: String): Result<Unit> {
        return try {
            notifications[notificationId]?.let { trayIcon ->
                if (SystemTray.isSupported()) {
                    SystemTray.getSystemTray().remove(trayIcon)
                }
                notifications.remove(notificationId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to cancel notification: ${e.message}", e))
        }
    }
    
    override suspend fun cancelAllNotifications(): Result<Unit> {
        return try {
            if (SystemTray.isSupported()) {
                val tray = SystemTray.getSystemTray()
                notifications.values.forEach { trayIcon ->
                    tray.remove(trayIcon)
                }
            }
            notifications.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to cancel all notifications: ${e.message}", e))
        }
    }
    
    override fun areNotificationsEnabled(): Boolean {
        // On JVM, we consider notifications enabled if system tray is supported
        // or if we're in a headless environment (where we use console fallback)
        return SystemTray.isSupported() || java.awt.GraphicsEnvironment.isHeadless()
    }
    
    override suspend fun requestNotificationPermission(): Result<Boolean> {
        // On JVM, no explicit permission is needed
        // Return true if system tray is supported or headless mode
        return Result.success(areNotificationsEnabled())
    }
    
    /**
     * Creates a simple image for the notification icon.
     * In a real application, this would load an actual icon file.
     */
    private fun createNotificationImage(): Image {
        // Create a simple 16x16 image as a placeholder
        val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        
        // Draw a simple colored square
        graphics.color = java.awt.Color(0, 120, 215) // Blue color
        graphics.fillRect(0, 0, 16, 16)
        graphics.dispose()
        
        return image
    }
}
