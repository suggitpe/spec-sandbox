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
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

class TimerServiceTest : FunSpec({
    
    lateinit var database: RecipeDatabase
    lateinit var timerRepository: TimerRepositoryImpl
    lateinit var timerService: TimerService
    
    beforeEach {
        // Create in-memory database for testing
        val driver = DatabaseDriverFactory().createDriver()
        database = RecipeDatabase(driver)
        timerRepository = TimerRepositoryImpl(database)
        timerService = TimerService(timerRepository)
    }
    
    afterEach {
        timerService.shutdown()
    }
    
    test("should create and start a timer") {
        val timer = CookingTimer(
            id = "timer-1",
            recipeId = "recipe-1",
            stepId = "step-1",
            duration = 10,
            remainingTime = 10,
            status = TimerStatus.READY,
            createdAt = Clock.System.now()
        )
        
        val result = timerService.startTimer(timer)
        
        result.isSuccess shouldBe true
        val startedTimer = result.getOrNull()
        startedTimer shouldNotBe null
        startedTimer?.status shouldBe TimerStatus.RUNNING
        
        // Verify timer is in active timers
        val activeTimers = timerService.getActiveTimers()
        activeTimers.containsKey("timer-1") shouldBe true
    }
    
    test("should pause a running timer") {
        val timer = CookingTimer(
            id = "timer-2",
            recipeId = "recipe-1",
            stepId = "step-1",
            duration = 10,
            remainingTime = 10,
            status = TimerStatus.READY,
            createdAt = Clock.System.now()
        )
        
        timerService.startTimer(timer)
        delay(1.seconds)
        
        val pauseResult = timerService.pauseTimer("timer-2")
        
        pauseResult.isSuccess shouldBe true
        val pausedTimer = pauseResult.getOrNull()
        pausedTimer?.status shouldBe TimerStatus.PAUSED
        pausedTimer?.remainingTime shouldBe 9 // Should have decremented by 1 second
    }
    
    test("should resume a paused timer") {
        val timer = CookingTimer(
            id = "timer-3",
            recipeId = "recipe-1",
            stepId = "step-1",
            duration = 10,
            remainingTime = 10,
            status = TimerStatus.READY,
            createdAt = Clock.System.now()
        )
        
        timerService.startTimer(timer)
        delay(1.seconds)
        timerService.pauseTimer("timer-3")
        
        val resumeResult = timerService.resumeTimer("timer-3")
        
        resumeResult.isSuccess shouldBe true
        val resumedTimer = resumeResult.getOrNull()
        resumedTimer?.status shouldBe TimerStatus.RUNNING
    }
    
    test("should cancel a timer") {
        val timer = CookingTimer(
            id = "timer-4",
            recipeId = "recipe-1",
            stepId = "step-1",
            duration = 10,
            remainingTime = 10,
            status = TimerStatus.READY,
            createdAt = Clock.System.now()
        )
        
        timerService.startTimer(timer)
        
        val cancelResult = timerService.cancelTimer("timer-4")
        
        cancelResult.isSuccess shouldBe true
        
        // Verify timer is removed from active timers
        val activeTimers = timerService.getActiveTimers()
        activeTimers.containsKey("timer-4") shouldBe false
    }
    
    test("should handle multiple concurrent timers") {
        val timer1 = CookingTimer(
            id = "timer-5",
            recipeId = "recipe-1",
            stepId = "step-1",
            duration = 10,
            remainingTime = 10,
            status = TimerStatus.READY,
            createdAt = Clock.System.now()
        )
        
        val timer2 = CookingTimer(
            id = "timer-6",
            recipeId = "recipe-1",
            stepId = "step-2",
            duration = 15,
            remainingTime = 15,
            status = TimerStatus.READY,
            createdAt = Clock.System.now()
        )
        
        val result1 = timerService.startTimer(timer1)
        val result2 = timerService.startTimer(timer2)
        
        result1.isSuccess shouldBe true
        result2.isSuccess shouldBe true
        
        // Give timers time to start
        delay(0.5.seconds)
        
        val activeTimers = timerService.getActiveTimers()
        activeTimers.size shouldBe 2
        activeTimers.containsKey("timer-5") shouldBe true
        activeTimers.containsKey("timer-6") shouldBe true
    }
    
    test("should persist timer state to database") {
        val timer = CookingTimer(
            id = "timer-7",
            recipeId = "recipe-1",
            stepId = "step-1",
            duration = 10,
            remainingTime = 10,
            status = TimerStatus.READY,
            createdAt = Clock.System.now()
        )
        
        timerService.startTimer(timer)
        delay(2.seconds)
        
        // Retrieve timer from database
        val retrieveResult = timerRepository.getTimer("timer-7")
        retrieveResult.isSuccess shouldBe true
        
        val retrievedTimer = retrieveResult.getOrNull()
        retrievedTimer shouldNotBe null
        retrievedTimer?.status shouldBe TimerStatus.RUNNING
    }
    
    test("should complete timer when countdown reaches zero") {
        val timer = CookingTimer(
            id = "timer-8",
            recipeId = "recipe-1",
            stepId = "step-1",
            duration = 2,
            remainingTime = 2,
            status = TimerStatus.READY,
            createdAt = Clock.System.now()
        )
        
        timerService.startTimer(timer)
        
        // Wait for timer to complete
        delay(3.seconds)
        
        // Timer should be removed from active timers
        val activeTimers = timerService.getActiveTimers()
        activeTimers.containsKey("timer-8") shouldBe false
        
        // Timer should be marked as completed in database
        val retrieveResult = timerRepository.getTimer("timer-8")
        retrieveResult.isSuccess shouldBe true
        retrieveResult.getOrNull()?.status shouldBe TimerStatus.COMPLETED
    }
    
    test("should restore active timers on initialization") {
        // Create and start a timer
        val timer = CookingTimer(
            id = "timer-9",
            recipeId = "recipe-1",
            stepId = "step-1",
            duration = 20,
            remainingTime = 20,
            status = TimerStatus.READY,
            createdAt = Clock.System.now()
        )
        
        timerService.startTimer(timer)
        delay(1.seconds)
        
        // Shutdown the service
        timerService.shutdown()
        
        // Create a new service instance (simulating app restart)
        val newTimerService = TimerService(timerRepository)
        newTimerService.initialize()
        
        // Timer should be restored
        val activeTimers = newTimerService.getActiveTimers()
        activeTimers.containsKey("timer-9") shouldBe true
        activeTimers["timer-9"]?.status shouldBe TimerStatus.RUNNING
        
        newTimerService.shutdown()
    }
})
