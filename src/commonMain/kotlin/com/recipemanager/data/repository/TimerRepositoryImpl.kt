package com.recipemanager.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.model.CookingTimer
import com.recipemanager.domain.model.TimerStatus
import com.recipemanager.domain.repository.TimerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class TimerRepositoryImpl(
    private val database: RecipeDatabase
) : TimerRepository {
    
    override suspend fun createTimer(timer: CookingTimer): Result<CookingTimer> = withContext(Dispatchers.Default) {
        try {
            database.cookingTimerQueries.insertTimer(
                id = timer.id,
                recipeId = timer.recipeId,
                stepId = timer.stepId,
                duration = timer.duration.toLong(),
                remainingTime = timer.remainingTime.toLong(),
                status = timer.status.name,
                createdAt = timer.createdAt.epochSeconds
            )
            Result.success(timer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getTimer(id: String): Result<CookingTimer?> = withContext(Dispatchers.Default) {
        try {
            val timerRow = database.cookingTimerQueries.selectTimerById(id).executeAsOneOrNull()
            
            if (timerRow == null) {
                return@withContext Result.success(null)
            }
            
            val timer = CookingTimer(
                id = timerRow.id,
                recipeId = timerRow.recipeId,
                stepId = timerRow.stepId,
                duration = timerRow.duration.toInt(),
                remainingTime = timerRow.remainingTime.toInt(),
                status = TimerStatus.valueOf(timerRow.status),
                createdAt = Instant.fromEpochSeconds(timerRow.createdAt)
            )
            
            Result.success(timer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateTimer(timer: CookingTimer): Result<CookingTimer> = withContext(Dispatchers.Default) {
        try {
            database.cookingTimerQueries.updateTimer(
                remainingTime = timer.remainingTime.toLong(),
                status = timer.status.name,
                id = timer.id
            )
            Result.success(timer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteTimer(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cookingTimerQueries.deleteTimer(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getActiveTimers(): Result<List<CookingTimer>> = withContext(Dispatchers.Default) {
        try {
            val timerRows = database.cookingTimerQueries.selectActiveTimers().executeAsList()
            
            val timers = timerRows.map { timerRow ->
                CookingTimer(
                    id = timerRow.id,
                    recipeId = timerRow.recipeId,
                    stepId = timerRow.stepId,
                    duration = timerRow.duration.toInt(),
                    remainingTime = timerRow.remainingTime.toInt(),
                    status = TimerStatus.valueOf(timerRow.status),
                    createdAt = Instant.fromEpochSeconds(timerRow.createdAt)
                )
            }
            
            Result.success(timers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun observeTimers(): Flow<List<CookingTimer>> {
        return database.cookingTimerQueries.selectAllTimers()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { timerRows ->
                timerRows.map { timerRow ->
                    CookingTimer(
                        id = timerRow.id,
                        recipeId = timerRow.recipeId,
                        stepId = timerRow.stepId,
                        duration = timerRow.duration.toInt(),
                        remainingTime = timerRow.remainingTime.toInt(),
                        status = TimerStatus.valueOf(timerRow.status),
                        createdAt = Instant.fromEpochSeconds(timerRow.createdAt)
                    )
                }
            }
    }
}
