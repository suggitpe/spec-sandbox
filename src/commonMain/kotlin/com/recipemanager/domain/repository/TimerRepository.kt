package com.recipemanager.domain.repository

import com.recipemanager.domain.model.CookingTimer
import kotlinx.coroutines.flow.Flow

interface TimerRepository {
    suspend fun createTimer(timer: CookingTimer): Result<CookingTimer>
    suspend fun getTimer(id: String): Result<CookingTimer?>
    suspend fun updateTimer(timer: CookingTimer): Result<CookingTimer>
    suspend fun deleteTimer(id: String): Result<Unit>
    suspend fun getActiveTimers(): Result<List<CookingTimer>>
    fun observeTimers(): Flow<List<CookingTimer>>
}