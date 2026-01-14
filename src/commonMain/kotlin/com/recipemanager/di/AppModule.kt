package com.recipemanager.di

import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.database.DatabaseManager
import com.recipemanager.data.repository.RecipeRepositoryImpl
import com.recipemanager.data.repository.PhotoRepositoryImpl
import com.recipemanager.data.storage.PhotoStorage
import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.domain.repository.PhotoRepository
import com.recipemanager.domain.service.PhotoCaptureService
import com.recipemanager.domain.service.PhotoCaptureProvider
import com.recipemanager.domain.service.PhotoAssociationService
import com.recipemanager.domain.usecase.RecipeUseCases
import com.recipemanager.domain.validation.RecipeValidator

class AppModule(
    private val databaseDriverFactory: DatabaseDriverFactory,
    private val photoStorage: PhotoStorage,
    private val photoCaptureProvider: PhotoCaptureProvider
) {
    
    private val databaseManager: DatabaseManager by lazy {
        DatabaseManager(databaseDriverFactory)
    }
    
    private val database: RecipeDatabase by lazy {
        databaseManager.initialize()
    }
    
    private val recipeValidator: RecipeValidator by lazy {
        RecipeValidator()
    }
    
    val recipeRepository: RecipeRepository by lazy {
        RecipeRepositoryImpl(database)
    }
    
    val photoRepository: PhotoRepository by lazy {
        PhotoRepositoryImpl(database, photoStorage)
    }
    
    val recipeUseCases: RecipeUseCases by lazy {
        RecipeUseCases(recipeRepository, recipeValidator)
    }
    
    val photoCaptureService: PhotoCaptureService by lazy {
        PhotoCaptureService(photoRepository, photoCaptureProvider)
    }
    
    val photoAssociationService: PhotoAssociationService by lazy {
        PhotoAssociationService(photoRepository as PhotoRepositoryImpl)
    }
}
