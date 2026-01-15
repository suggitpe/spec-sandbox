package com.recipemanager.domain

import com.recipemanager.domain.model.CookingTimer
import com.recipemanager.domain.model.TimerStatus
import com.recipemanager.domain.service.Notification
import com.recipemanager.domain.service.NotificationService
import com.recipemanager.domain.service.NotificationManager
import com.recipemanager.domain.service.NotificationType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Mock implementation of NotificationService for testing.
 */
class MockNotificationService : NotificationService {
    
    val notifications = mutableListOf<Notification>()
    val timerNotifications = mutableListOf<Pair<CookingTimer, String?>>()
    val cookingReminders = mutableListOf<Triple<String, String, String?>>()
    var notificationsEnabled = true
    var permissionGranted = true
    
    override suspend fun showNotification(notification: Notification): Result<Unit> {
        notifications.add(notification)
        return Result.success(Unit)
    }
    
    override suspend fun showTimerCompletedNotification(
        timer: CookingTimer,
        recipeName: String?
    ): Result<Unit> {
        timerNotifications.add(timer to recipeName)
        val notification = Notification(
            id = "timer_${timer.id}",
            title = "Timer Completed!",
            message = "Recipe: ${recipeName ?: "Unknown"}",
            type = NotificationType.TIMER_COMPLETED,
            timerId = timer.id,
            recipeId = timer.recipeId
        )
        notifications.add(notification)
        return Result.success(Unit)
    }
    
    override suspend fun showCookingReminder(
        title: String,
        message: String,
        recipeId: String?
    ): Result<Unit> {
        cookingReminders.add(Triple(title, message, recipeId))
        val notification = Notification(
            id = "reminder_${System.currentTimeMillis()}",
            title = title,
            message = message,
            type = NotificationType.COOKING_REMINDER,
            recipeId = recipeId
        )
        notifications.add(notification)
        return Result.success(Unit)
    }
    
    override suspend fun cancelNotification(notificationId: String): Result<Unit> {
        notifications.removeIf { it.id == notificationId }
        return Result.success(Unit)
    }
    
    override suspend fun cancelAllNotifications(): Result<Unit> {
        notifications.clear()
        return Result.success(Unit)
    }
    
    override fun areNotificationsEnabled(): Boolean {
        return notificationsEnabled
    }
    
    override suspend fun requestNotificationPermission(): Result<Boolean> {
        return Result.success(permissionGranted)
    }
    
    fun reset() {
        notifications.clear()
        timerNotifications.clear()
        cookingReminders.clear()
        notificationsEnabled = true
        permissionGranted = true
    }
}

class NotificationServiceTest : FunSpec({
    
    lateinit var mockNotificationService: MockNotificationService
    lateinit var notificationManager: NotificationManager
    
    beforeEach {
        mockNotificationService = MockNotificationService()
        notificationManager = NotificationManager(mockNotificationService)
    }
    
    afterEach {
        notificationManager.shutdown()
        mockNotificationService.reset()
    }
    
    test("should initialize and request notification permission") {
        val result = notificationManager.initialize()
        
        result.isSuccess shouldBe true
        result.getOrNull() shouldBe true
    }
    
    test("should send timer completed notification") {
        val timer = CookingTimer(
            id = "timer-1",
            recipeId = "recipe-1",
            stepId = "step-1",
            duration = 10,
            remainingTime = 0,
            status = TimerStatus.COMPLETED,
            createdAt = Clock.System.now()
        )
        
        val result = notificationManager.notifyTimerCompleted(timer, null)
        
        result.isSuccess shouldBe true
        mockNotificationService.timerNotifications.size shouldBe 1
        mockNotificationService.timerNotifications[0].first.id shouldBe "timer-1"
        mockNotificationService.notifications.size shouldBe 1
        mockNotificationService.notifications[0].type shouldBe NotificationType.TIMER_COMPLETED
    }
    
    test("should send timer warning notification") {
        val timer = CookingTimer(
            id = "timer-3",
            recipeId = "recipe-1",
            stepId = "step-1",
            duration = 60,
            remainingTime = 10,
            status = TimerStatus.RUNNING,
            createdAt = Clock.System.now()
        )
        
        val result = notificationManager.notifyTimerWarning(timer, 10, null)
        
        result.isSuccess shouldBe true
        mockNotificationService.notifications.size shouldBe 1
        mockNotificationService.notifications[0].type shouldBe NotificationType.TIMER_WARNING
        mockNotificationService.notifications[0].message shouldBe "10 seconds remaining"
    }
    
    test("should schedule cooking reminder for future time") {
        val scheduledTime = Clock.System.now() + 100.milliseconds
        
        val result = notificationManager.scheduleCookingReminder(
            reminderId = "reminder-1",
            title = "Start Cooking",
            message = "Time to prepare dinner",
            scheduledTime = scheduledTime,
            recipeId = "recipe-1"
        )
        
        result.isSuccess shouldBe true
        
        // Wait for reminder to trigger
        delay(200.milliseconds)
        
        mockNotificationService.cookingReminders.size shouldBe 1
        mockNotificationService.cookingReminders[0].first shouldBe "Start Cooking"
        mockNotificationService.cookingReminders[0].second shouldBe "Time to prepare dinner"
    }
    
    test("should schedule cooking reminder with duration") {
        val result = notificationManager.scheduleCookingReminderIn(
            reminderId = "reminder-2",
            title = "Preheat Oven",
            message = "Set oven to 350°F",
            delay = 100.milliseconds,
            recipeId = "recipe-1"
        )
        
        result.isSuccess shouldBe true
        
        // Wait for reminder to trigger
        delay(200.milliseconds)
        
        mockNotificationService.cookingReminders.size shouldBe 1
        mockNotificationService.cookingReminders[0].first shouldBe "Preheat Oven"
    }
    
    test("should show reminder immediately if scheduled time has passed") {
        val pastTime = Clock.System.now() - 1.seconds
        
        val result = notificationManager.scheduleCookingReminder(
            reminderId = "reminder-3",
            title = "Immediate Reminder",
            message = "This should show immediately",
            scheduledTime = pastTime,
            recipeId = null
        )
        
        result.isSuccess shouldBe true
        mockNotificationService.cookingReminders.size shouldBe 1
    }
    
    test("should cancel scheduled cooking reminder") {
        val scheduledTime = Clock.System.now() + 300.milliseconds
        
        notificationManager.scheduleCookingReminder(
            reminderId = "reminder-4",
            title = "Cancelled Reminder",
            message = "This should not show",
            scheduledTime = scheduledTime,
            recipeId = null
        )
        
        // Cancel before it triggers
        delay(50.milliseconds)
        val cancelResult = notificationManager.cancelCookingReminder("reminder-4")
        
        cancelResult.isSuccess shouldBe true
        
        // Wait past the scheduled time
        delay(300.milliseconds)
        
        // Reminder should not have triggered
        mockNotificationService.cookingReminders.size shouldBe 0
    }
    
    test("should cancel all cooking reminders") {
        val scheduledTime = Clock.System.now() + 300.milliseconds
        
        notificationManager.scheduleCookingReminder(
            reminderId = "reminder-5",
            title = "Reminder 1",
            message = "Message 1",
            scheduledTime = scheduledTime,
            recipeId = null
        )
        
        notificationManager.scheduleCookingReminder(
            reminderId = "reminder-6",
            title = "Reminder 2",
            message = "Message 2",
            scheduledTime = scheduledTime,
            recipeId = null
        )
        
        delay(50.milliseconds)
        val cancelResult = notificationManager.cancelAllCookingReminders()
        
        cancelResult.isSuccess shouldBe true
        
        // Wait past the scheduled time
        delay(300.milliseconds)
        
        // No reminders should have triggered
        mockNotificationService.cookingReminders.size shouldBe 0
    }
    
    test("should send general notification") {
        val result = notificationManager.sendNotification(
            title = "General Alert",
            message = "This is a general notification"
        )
        
        result.isSuccess shouldBe true
        mockNotificationService.notifications.size shouldBe 1
        mockNotificationService.notifications[0].type shouldBe NotificationType.GENERAL
        mockNotificationService.notifications[0].title shouldBe "General Alert"
    }
    
    test("should check if notifications are enabled") {
        val enabled = notificationManager.areNotificationsEnabled()
        
        enabled shouldBe true
    }
    
    test("should handle disabled notifications") {
        mockNotificationService.notificationsEnabled = false
        
        val enabled = notificationManager.areNotificationsEnabled()
        
        enabled shouldBe false
    }
    
    test("should replace scheduled reminder with same ID") {
        val scheduledTime1 = Clock.System.now() + 200.milliseconds
        val scheduledTime2 = Clock.System.now() + 400.milliseconds
        
        // Schedule first reminder
        notificationManager.scheduleCookingReminder(
            reminderId = "reminder-7",
            title = "First Reminder",
            message = "This should be replaced",
            scheduledTime = scheduledTime1,
            recipeId = null
        )
        
        // Schedule second reminder with same ID (should replace first)
        notificationManager.scheduleCookingReminder(
            reminderId = "reminder-7",
            title = "Second Reminder",
            message = "This should show",
            scheduledTime = scheduledTime2,
            recipeId = null
        )
        
        // Wait past first scheduled time but before second
        delay(300.milliseconds)
        mockNotificationService.cookingReminders.size shouldBe 0
        
        // Wait for second reminder
        delay(200.milliseconds)
        mockNotificationService.cookingReminders.size shouldBe 1
        mockNotificationService.cookingReminders[0].first shouldBe "Second Reminder"
    }
})
