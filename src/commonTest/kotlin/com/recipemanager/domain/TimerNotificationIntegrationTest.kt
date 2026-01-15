package com.recipemanager.domain

import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.repository.TimerRepositoryImpl
import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.model.CookingTimer
import com.recipemanager.domain.model.TimerStatus
import com.recipemanager.domain.service.TimerService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Integration test for TimerService with NotificationService.
 * Validates that notifications are triggered when timers complete.
 */
class TimerNotificationIntegrationTest : FunSpec({
    
    lateinit var database: RecipeDatabase
    lateinit var timerRepository: TimerRepositoryImpl
    lateinit var mockNotificationService: MockNotificationService
    lateinit var timerService: TimerService
    
    beforeEach {
        // Create in-memory database for testing
        val driver = DatabaseDriverFactory().createDriver()
        database = RecipeDatabase(driver)
        timerRepository = TimerRepositoryImpl(database)
        mockNotificationService = MockNotificationService()
        timerService = TimerService(timerRepository, mockNotificationService)
    }
    
    afterEach {
        timerService.shutdown()
        mockNotificationService.reset()
    }
    
    test("should send notification when timer completes") {
        val timer = CookingTimer(
            id = "timer-1",
            recipeId = "recipe-1",
            stepId = "step-1",
            duration = 2,
            remainingTime = 2,
            status = TimerStatus.READY,
            createdAt = Clock.System.now()
        )
        
        // Start the timer
        val startResult = timerService.startTimer(timer)
        startResult.isSuccess shouldBe true
        
        // Wait for timer to complete
        delay(3.seconds)
        
        // Verify notification was sent
        mockNotificationService.timerNotifications.size shouldBe 1
        mockNotificationService.timerNotifications[0].first.id shouldBe "timer-1"
        mockNotificationService.timerNotifications[0].first.status shouldBe TimerStatus.COMPLETED
    }
    
    test("should send notifications for multiple concurrent timers") {
        val timer1 = CookingTimer(
            id = "timer-2",
            recipeId = "recipe-1",
            stepId = "step-1",
            duration = 2,
            remainingTime = 2,
            status = TimerStatus.READY,
            createdAt = Clock.System.now()
        )
        
        val timer2 = CookingTimer(
            id = "timer-3",
            recipeId = "recipe-1",
            stepId = "step-2",
            duration = 3,
            remainingTime = 3,
            status = TimerStatus.READY,
            createdAt = Clock.System.now()
        )
        
        // Start both timers
        timerService.startTimer(timer1)
        timerService.startTimer(timer2)
        
        // Wait for both timers to complete
        delay(4.seconds)
        
        // Verify both notifications were sent
        mockNotificationService.timerNotifications.size shouldBe 2
        
        val notifiedTimerIds = mockNotificationService.timerNotifications.map { it.first.id }
        notifiedTimerIds shouldBe listOf("timer-2", "timer-3")
    }
    
    test("should not send notification when timer is cancelled") {
        val timer = CookingTimer(
            id = "timer-4",
            recipeId = "recipe-1",
            stepId = "step-1",
            duration = 5,
            remainingTime = 5,
            status = TimerStatus.READY,
            createdAt = Clock.System.now()
        )
        
        // Start the timer
        timerService.startTimer(timer)
        
        // Cancel the timer before it completes
        delay(1.seconds)
        timerService.cancelTimer("timer-4")
        
        // Wait past the original completion time
        delay(5.seconds)
        
        // Verify no notification was sent
        mockNotificationService.timerNotifications.size shouldBe 0
    }
    
    test("should send notification when paused timer is resumed and completes") {
        val timer = CookingTimer(
            id = "timer-5",
            recipeId = "recipe-1",
            stepId = "step-1",
            duration = 3,
            remainingTime = 3,
            status = TimerStatus.READY,
            createdAt = Clock.System.now()
        )
        
        // Start the timer
        timerService.startTimer(timer)
        
        // Pause after 1 second
        delay(1.seconds)
        timerService.pauseTimer("timer-5")
        
        // Wait a bit while paused
        delay(1.seconds)
        
        // Resume the timer
        timerService.resumeTimer("timer-5")
        
        // Wait for timer to complete (should take ~2 more seconds)
        delay(3.seconds)
        
        // Verify notification was sent
        mockNotificationService.timerNotifications.size shouldBe 1
        mockNotificationService.timerNotifications[0].first.id shouldBe "timer-5"
    }
    
    test("should work correctly when notification service is not provided") {
        // Create timer service without notification service
        val timerServiceWithoutNotifications = TimerService(timerRepository, null)
        
        val timer = CookingTimer(
            id = "timer-6",
            recipeId = "recipe-1",
            stepId = "step-1",
            duration = 2,
            remainingTime = 2,
            status = TimerStatus.READY,
            createdAt = Clock.System.now()
        )
        
        // Start the timer
        val startResult = timerServiceWithoutNotifications.startTimer(timer)
        startResult.isSuccess shouldBe true
        
        // Wait for timer to complete
        delay(3.seconds)
        
        // Timer should complete successfully even without notifications
        val retrieveResult = timerRepository.getTimer("timer-6")
        retrieveResult.isSuccess shouldBe true
        retrieveResult.getOrNull()?.status shouldBe TimerStatus.COMPLETED
        
        timerServiceWithoutNotifications.shutdown()
    }
})
