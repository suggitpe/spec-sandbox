package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.repository.CollectionRepository
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.domain.service.PhotoAssociationService
import com.recipemanager.domain.service.PhotoCaptureService
import com.recipemanager.domain.service.PlatformShareService
import com.recipemanager.domain.service.ShareService
import com.recipemanager.domain.service.TimerService
import com.recipemanager.domain.validation.RecipeValidator
import com.recipemanager.presentation.navigation.StatePersistence

/**
 * Factory for creating ViewModels with proper dependency injection and lifecycle management.
 * Implements Requirements 7.5 (state preservation) and 8.1, 8.2 (data persistence).
 */
class ViewModelFactory(
    private val recipeRepository: RecipeRepository,
    private val collectionRepository: CollectionRepository,
    private val timerService: TimerService,
    private val photoCaptureService: PhotoCaptureService,
    private val photoAssociationService: PhotoAssociationService,
    private val shareService: ShareService,
    private val platformShareService: PlatformShareService,
    private val recipeValidator: RecipeValidator,
    private val statePersistence: StatePersistence,
    private val lifecycleManager: ViewModelLifecycleManager
) {
    
    /**
     * Create RecipeListViewModel with state persistence.
     */
    fun createRecipeListViewModel(): RecipeListViewModel {
        val viewModel = RecipeListViewModel(
            recipeRepository = recipeRepository,
            statePersistence = statePersistence
        )
        lifecycleManager.registerViewModel("recipe_list", viewModel)
        return viewModel
    }
    
    /**
     * Create RecipeDetailViewModel with state persistence.
     */
    fun createRecipeDetailViewModel(): RecipeDetailViewModel {
        val viewModel = RecipeDetailViewModel(
            recipeRepository = recipeRepository,
            statePersistence = statePersistence
        )
        lifecycleManager.registerViewModel("recipe_detail", viewModel)
        return viewModel
    }
    
    /**
     * Create RecipeFormViewModel with state persistence.
     */
    fun createRecipeFormViewModel(): RecipeFormViewModel {
        val viewModel = RecipeFormViewModel(
            recipeRepository = recipeRepository,
            recipeValidator = recipeValidator,
            statePersistence = statePersistence
        )
        lifecycleManager.registerViewModel("recipe_form", viewModel)
        return viewModel
    }
    
    /**
     * Create CookingModeViewModel with state persistence.
     */
    fun createCookingModeViewModel(): CookingModeViewModel {
        val viewModel = CookingModeViewModel(
            recipeRepository = recipeRepository,
            timerService = timerService,
            statePersistence = statePersistence
        )
        lifecycleManager.registerViewModel("cooking_mode", viewModel)
        return viewModel
    }
    
    /**
     * Create PhotoManagementViewModel with state persistence.
     */
    fun createPhotoManagementViewModel(): PhotoManagementViewModel {
        val viewModel = PhotoManagementViewModel(
            photoCaptureService = photoCaptureService,
            photoAssociationService = photoAssociationService,
            statePersistence = statePersistence
        )
        lifecycleManager.registerViewModel("photo_management", viewModel)
        return viewModel
    }
    
    /**
     * Create CollectionListViewModel with state persistence.
     */
    fun createCollectionListViewModel(): CollectionListViewModel {
        val viewModel = CollectionListViewModel(
            collectionRepository = collectionRepository,
            statePersistence = statePersistence
        )
        lifecycleManager.registerViewModel("collection_list", viewModel)
        return viewModel
    }
    
    /**
     * Create CollectionDetailViewModel with state persistence.
     */
    fun createCollectionDetailViewModel(): CollectionDetailViewModel {
        val viewModel = CollectionDetailViewModel(
            collectionRepository = collectionRepository,
            statePersistence = statePersistence
        )
        lifecycleManager.registerViewModel("collection_detail", viewModel)
        return viewModel
    }
    
    /**
     * Create ShareViewModel with state persistence.
     */
    fun createShareViewModel(): ShareViewModel {
        val viewModel = ShareViewModel(
            recipeRepository = recipeRepository,
            shareService = shareService,
            platformShareService = platformShareService,
            statePersistence = statePersistence
        )
        lifecycleManager.registerViewModel("share", viewModel)
        return viewModel
    }
    
    /**
     * Create ImportViewModel with state persistence.
     */
    fun createImportViewModel(): ImportViewModel {
        val viewModel = ImportViewModel(
            recipeRepository = recipeRepository,
            shareService = shareService,
            statePersistence = statePersistence
        )
        lifecycleManager.registerViewModel("import", viewModel)
        return viewModel
    }
    
    /**
     * Clean up a specific ViewModel.
     */
    fun destroyViewModel(key: String) {
        lifecycleManager.unregisterViewModel(key)
    }
    
    /**
     * Clean up all ViewModels.
     */
    fun destroyAllViewModels() {
        lifecycleManager.cleanup()
    }
}

/**
 * Extension function to create ViewModels with automatic lifecycle management.
 */
inline fun <reified T : BaseViewModel<*>> ViewModelFactory.create(key: String): T {
    return when (T::class) {
        RecipeListViewModel::class -> createRecipeListViewModel() as T
        RecipeDetailViewModel::class -> createRecipeDetailViewModel() as T
        RecipeFormViewModel::class -> createRecipeFormViewModel() as T
        CookingModeViewModel::class -> createCookingModeViewModel() as T
        PhotoManagementViewModel::class -> createPhotoManagementViewModel() as T
        CollectionListViewModel::class -> createCollectionListViewModel() as T
        CollectionDetailViewModel::class -> createCollectionDetailViewModel() as T
        ShareViewModel::class -> createShareViewModel() as T
        ImportViewModel::class -> createImportViewModel() as T
        else -> throw IllegalArgumentException("Unknown ViewModel type: ${T::class.simpleName}")
    }
}