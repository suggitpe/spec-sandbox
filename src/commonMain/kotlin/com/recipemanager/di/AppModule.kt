package com.recipemanager.di

import com.recipemanager.data.cloud.CloudSyncManager
import com.recipemanager.data.cloud.FirebaseConfig
import com.recipemanager.data.cloud.FirebaseFactory
import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.database.DatabaseManager
import com.recipemanager.data.repository.RecipeRepositoryImpl
import com.recipemanager.data.repository.PhotoRepositoryImpl
import com.recipemanager.data.repository.RecipeVersionRepositoryImpl
import com.recipemanager.data.repository.RecipeSnapshotRepositoryImpl
import com.recipemanager.data.repository.TimerRepositoryImpl
import com.recipemanager.data.repository.CollectionRepositoryImpl
import com.recipemanager.data.storage.PhotoStorage
import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.domain.repository.PhotoRepository
import com.recipemanager.domain.repository.RecipeVersionRepository
import com.recipemanager.domain.repository.RecipeSnapshotRepository
import com.recipemanager.domain.repository.TimerRepository
import com.recipemanager.domain.repository.CollectionRepository
import com.recipemanager.domain.service.PhotoCaptureService
import com.recipemanager.domain.service.PhotoCaptureProvider
import com.recipemanager.domain.service.PhotoAssociationService
import com.recipemanager.domain.service.ShareService
import com.recipemanager.domain.service.PlatformShareService
import com.recipemanager.domain.service.RecipeCopyManager
import com.recipemanager.domain.service.RecipeVersionManager
import com.recipemanager.domain.service.NotificationService
import com.recipemanager.domain.service.NotificationManager
import com.recipemanager.domain.service.TimerService
import com.recipemanager.domain.usecase.RecipeUseCases
import com.recipemanager.domain.usecase.ShareRecipeUseCase
import com.recipemanager.domain.usecase.ImportRecipeUseCase
import com.recipemanager.domain.validation.RecipeValidator

class AppModule(
    private val databaseDriverFactory: DatabaseDriverFactory,
    private val photoStorage: PhotoStorage,
    private val photoCaptureProvider: PhotoCaptureProvider,
    private val platformShareService: PlatformShareService,
    private val notificationService: NotificationService,
    private val firebaseConfig: FirebaseConfig = FirebaseConfig.DEFAULT
) {
    
    private val databaseManager: DatabaseManager by lazy {
        DatabaseManager(databaseDriverFactory)
    }
    
    private val database: RecipeDatabase by lazy {
        databaseManager.initialize()
    }
    
    private val firebaseFactory: FirebaseFactory by lazy {
        FirebaseFactory().apply {
            initialize(firebaseConfig)
        }
    }
    
    val cloudSyncManager: CloudSyncManager by lazy {
        CloudSyncManager(
            auth = firebaseFactory.createAuth(),
            storage = firebaseFactory.createStorage(),
            firestore = firebaseFactory.createFirestore()
        )
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
    
    val recipeVersionRepository: RecipeVersionRepository by lazy {
        RecipeVersionRepositoryImpl(database)
    }
    
    val recipeSnapshotRepository: RecipeSnapshotRepository by lazy {
        RecipeSnapshotRepositoryImpl(database)
    }
    
    val timerRepository: TimerRepository by lazy {
        TimerRepositoryImpl(database)
    }
    
    val collectionRepository: CollectionRepository by lazy {
        CollectionRepositoryImpl(database)
    }
    
    val notificationManager: NotificationManager by lazy {
        NotificationManager(notificationService)
    }
    
    val timerService: TimerService by lazy {
        TimerService(timerRepository, notificationService)
    }
    
    val recipeVersionManager: RecipeVersionManager by lazy {
        RecipeVersionManager(recipeRepository, recipeVersionRepository, recipeSnapshotRepository)
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
    
    val shareService: ShareService by lazy {
        ShareService(recipeValidator)
    }
    
    val recipeCopyManager: RecipeCopyManager by lazy {
        RecipeCopyManager()
    }
    
    val shareRecipeUseCase: ShareRecipeUseCase by lazy {
        ShareRecipeUseCase(shareService, platformShareService)
    }
    
    val importRecipeUseCase: ImportRecipeUseCase by lazy {
        ImportRecipeUseCase(shareService, recipeRepository, recipeCopyManager)
    }
}
