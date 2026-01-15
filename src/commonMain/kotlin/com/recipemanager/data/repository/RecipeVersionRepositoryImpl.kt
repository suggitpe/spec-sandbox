package com.recipemanager.data.repository

import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.model.RecipeVersion
import com.recipemanager.domain.repository.RecipeVersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class RecipeVersionRepositoryImpl(
    private val database: RecipeDatabase
) : RecipeVersionRepository {
    
    override suspend fun createVersion(version: RecipeVersion): Result<RecipeVersion> = withContext(Dispatchers.Default) {
        try {
            database.recipeVersionQueries.insertVersion(
                id = version.id,
                recipeId = version.recipeId,
                version = version.version.toLong(),
                parentRecipeId = version.parentRecipeId,
                upgradeNotes = version.upgradeNotes,
                createdAt = version.createdAt.epochSeconds,
                createdBy = version.createdBy
            )
            Result.success(version)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getVersionsByRecipeId(recipeId: String): Result<List<RecipeVersion>> = withContext(Dispatchers.Default) {
        try {
            val versions = database.recipeVersionQueries.selectVersionsByRecipeId(recipeId)
                .executeAsList()
                .map { row ->
                    RecipeVersion(
                        id = row.id,
                        recipeId = row.recipeId,
                        version = row.version.toInt(),
                        parentRecipeId = row.parentRecipeId,
                        upgradeNotes = row.upgradeNotes,
                        createdAt = Instant.fromEpochSeconds(row.createdAt),
                        createdBy = row.createdBy
                    )
                }
            Result.success(versions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getVersionById(id: String): Result<RecipeVersion?> = withContext(Dispatchers.Default) {
        try {
            val row = database.recipeVersionQueries.selectVersionById(id).executeAsOneOrNull()
            
            val version = row?.let {
                RecipeVersion(
                    id = it.id,
                    recipeId = it.recipeId,
                    version = it.version.toInt(),
                    parentRecipeId = it.parentRecipeId,
                    upgradeNotes = it.upgradeNotes,
                    createdAt = Instant.fromEpochSeconds(it.createdAt),
                    createdBy = it.createdBy
                )
            }
            
            Result.success(version)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getVersionByRecipeIdAndVersion(
        recipeId: String,
        version: Int
    ): Result<RecipeVersion?> = withContext(Dispatchers.Default) {
        try {
            val row = database.recipeVersionQueries.selectVersionByRecipeIdAndVersion(
                recipeId = recipeId,
                version = version.toLong()
            ).executeAsOneOrNull()
            
            val versionEntry = row?.let {
                RecipeVersion(
                    id = it.id,
                    recipeId = it.recipeId,
                    version = it.version.toInt(),
                    parentRecipeId = it.parentRecipeId,
                    upgradeNotes = it.upgradeNotes,
                    createdAt = Instant.fromEpochSeconds(it.createdAt),
                    createdBy = it.createdBy
                )
            }
            
            Result.success(versionEntry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getRecipeFamilyVersions(recipeId: String): Result<List<RecipeVersion>> = withContext(Dispatchers.Default) {
        try {
            val versions = database.recipeVersionQueries.selectAllVersionsForRecipeFamily(
                recipeId,
                recipeId
            ).executeAsList()
                .map { row ->
                    RecipeVersion(
                        id = row.id,
                        recipeId = row.recipeId,
                        version = row.version.toInt(),
                        parentRecipeId = row.parentRecipeId,
                        upgradeNotes = row.upgradeNotes,
                        createdAt = Instant.fromEpochSeconds(row.createdAt),
                        createdBy = row.createdBy
                    )
                }
            Result.success(versions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteVersionsByRecipeId(recipeId: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.recipeVersionQueries.deleteVersionsByRecipeId(recipeId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
