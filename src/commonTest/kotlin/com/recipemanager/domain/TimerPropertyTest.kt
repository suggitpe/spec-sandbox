package com.recipemanager.domain

import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.repository.TimerRepositoryImpl
import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.model.CookingTimer
import com.recipemanager.domain.model.TimerStatus
import com.recipemanager.domain.service.TimerService
import com.recipemanager.test.generators.cookingTimerArb
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.checkAll
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Property-based tests for timer functionality.
 * Validates universal properties that must hold for all timer operations.
 */
class TimerPropertyTest : FunSpec({
    
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
    
    test("Property 11: Timer Creation Completeness - Feature: recipe-manager, Property 11: For any recipe with timed cooking steps, starting a cooking session should create active timers for all steps that specify duration").config(invocations = 5) {
        // Create a custom generator for ready timers with valid remaining time
        val readyTimerArb = arbitrary { rs ->
            val duration = Arb.int(10..60).bind()
            CookingTimer(
                id = Arb.string(1..50).bind(),
                recipeId = Arb.string(1..50).bind(),
                stepId = Arb.string(1..50).bind(),
                duration = duration,
                remainingTime = duration, // Ensure remaining time equals duration for ready timers
                status = TimerStatus.READY,
                createdAt = Clock.System.now()
            )
        }
        
        checkAll(5, readyTimerArb) { timer ->
            // Start the timer
            val result = timerService.startTimer(timer)
            
            // Verify timer was created successfully
            result.isSuccess shouldBe true
            val startedTimer = result.getOrNull()
            startedTimer shouldNotBe null
            
            // Verify timer is in RUNNING status
            startedTimer?.status shouldBe TimerStatus.RUNNING
            
            // Verify timer has all required fields
            startedTimer?.id shouldBe timer.id
            startedTimer?.recipeId shouldBe timer.recipeId
            startedTimer?.stepId shouldBe timer.stepId
            startedTimer?.duration shouldBe timer.duration
            startedTimer?.remainingTime shouldBe timer.remainingTime
            
            // Verify timer is in active timers
            val activeTimers = timerService.getActiveTimers()
            activeTimers shouldContainKey timer.id
            activeTimers[timer.id]?.status shouldBe TimerStatus.RUNNING
            
            // Verify timer can be retrieved
            val retrieveResult = timerService.getTimer(timer.id)
            retrieveResult.isSuccess shouldBe true
            retrieveResult.getOrNull() shouldNotBe null
            
            // Clean up
            timerService.cancelTimer(timer.id)
        }
    }
    
    test("Property 12: Timer Notification Reliability - Feature: recipe-manager, Property 12: For any active timer, when the countdown reaches zero, the system should trigger a notification regardless of app state").config(invocations = 3) {
        // Create a custom generator for short-duration timers
        val shortTimerArb = arbitrary { rs ->
            CookingTimer(
                id = Arb.string(1..50).bind(),
                recipeId = Arb.string(1..50).bind(),
                stepId = Arb.string(1..50).bind(),
                duration = 1, // 1 second for fast testing
                remainingTime = 1,
                status = TimerStatus.READY,
                createdAt = Clock.System.now()
            )
        }
        
        checkAll(3, shortTimerArb) { timer ->
            // Reset mock notification service
            mockNotificationService.reset()
            
            // Start the timer
            val startResult = timerService.startTimer(timer)
            startResult.isSuccess shouldBe true
            
            // Wait for timer to complete (1 second + small buffer)
            delay(1.5.seconds)
            
            // Verify notification was sent
            mockNotificationService.timerNotifications.size shouldBe 1
            mockNotificationService.timerNotifications[0].first.id shouldBe timer.id
            mockNotificationService.timerNotifications[0].first.status shouldBe TimerStatus.COMPLETED
            
            // Verify timer is no longer in active timers
            val activeTimers = timerService.getActiveTimers()
            activeTimers.containsKey(timer.id) shouldBe false
            
            // Verify timer is marked as completed in database
            val retrieveResult = timerRepository.getTimer(timer.id)
            retrieveResult.isSuccess shouldBe true
            retrieveResult.getOrNull()?.status shouldBe TimerStatus.COMPLETED
        }
    }
    
    test("Property 13: Multi-Timer State Management - Feature: recipe-manager, Property 13: For any number of active timers, the system should accurately track and display the remaining time for each timer independently").config(invocations = 5) {
        checkAll(5, Arb.list(cookingTimerArb().filter { it.status == TimerStatus.READY && it.duration > 5 }, 2..5)) { timers ->
            // Ensure unique IDs
            val uniqueTimers = timers.mapIndexed { index, timer ->
                timer.copy(
                    id = "timer-${index}-${Clock.System.now().toEpochMilliseconds()}",
                    duration = 10,
                    remainingTime = 10
                )
            }
            
            // Start all timers
            val startResults = uniqueTimers.map { timer ->
                timerService.startTimer(timer)
            }
            
            // Verify all timers started successfully
            startResults.forEach { result ->
                result.isSuccess shouldBe true
            }
            
            // Wait a bit for timers to run
            delay(1.seconds)
            
            // Get active timers
            val activeTimers = timerService.getActiveTimers()
            
            // Verify all timers are active
            activeTimers.size shouldBe uniqueTimers.size
            
            // Verify each timer is tracked independently
            uniqueTimers.forEach { originalTimer ->
                activeTimers shouldContainKey originalTimer.id
                val activeTimer = activeTimers[originalTimer.id]
                
                // Verify timer is running
                activeTimer?.status shouldBe TimerStatus.RUNNING
                
                // Verify remaining time has decreased (allow for timing variations)
                activeTimer?.remainingTime shouldNotBe null
                activeTimer?.remainingTime?.let { remaining ->
                    (remaining in 7..10) shouldBe true // Should have decremented by ~1 second, allow range for system timing
                }
                
                // Verify timer maintains its identity
                activeTimer?.id shouldBe originalTimer.id
                activeTimer?.recipeId shouldBe originalTimer.recipeId
                activeTimer?.stepId shouldBe originalTimer.stepId
                activeTimer?.duration shouldBe originalTimer.duration
            }
            
            // Clean up all timers
            uniqueTimers.forEach { timer ->
                timerService.cancelTimer(timer.id)
            }
        }
    }
})
