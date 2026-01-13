package com.recipemanager.domain.repository

import com.recipemanager.domain.model.Photo
import com.recipemanager.domain.model.PhotoStage
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    suspend fun savePhoto(photo: Photo): Result<Photo>
    suspend fun getPhoto(id: String): Result<Photo?>
    suspend fun getPhotosByStage(recipeId: String, stage: PhotoStage): Result<List<Photo>>
    suspend fun deletePhoto(id: String): Result<Unit>
    fun observePhotos(recipeId: String): Flow<List<Photo>>
}