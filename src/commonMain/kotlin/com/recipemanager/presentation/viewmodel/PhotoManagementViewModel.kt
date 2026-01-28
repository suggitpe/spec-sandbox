package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.Photo
import com.recipemanager.domain.model.PhotoStage
import com.recipemanager.domain.service.PhotoAssociationService
import com.recipemanager.domain.service.PhotoCaptureService
import com.recipemanager.presentation.navigation.StatePersistence
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PhotoManagementState(
    val recipeId: String? = null,
    val photos: List<Photo> = emptyList(),
    val photosByStage: Map<PhotoStage, List<Photo>> = emptyMap(),
    val selectedPhoto: Photo? = null,
    val selectedStage: PhotoStage = PhotoStage.RAW_INGREDIENTS,
    val isCapturing: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val captionEditMode: Boolean = false,
    val editingCaption: String = "",
    val lastRefreshTime: Long = 0L
)

class PhotoManagementViewModel(
    private val photoCaptureService: PhotoCaptureService,
    private val photoAssociationService: PhotoAssociationService,
    statePersistence: StatePersistence? = null
) : BaseViewModel<PhotoManagementState>(
    initialState = PhotoManagementState(),
    statePersistence = statePersistence,
    stateKey = "photo_management"
) {
    
    override fun onInitialize() {
        // Load photos if we have a recipe ID and haven't loaded recently
        if (currentState.recipeId != null) {
            val shouldRefresh = currentState.photos.isEmpty() || 
                (System.currentTimeMillis() - currentState.lastRefreshTime) > 60_000 // 1 minute
            
            if (shouldRefresh) {
                loadPhotos()
            }
        }
    }

    fun setRecipeId(recipeId: String) {
        currentState = currentState.copy(recipeId = recipeId)
        loadPhotos()
    }

    fun loadPhotos() {
        val recipeId = currentState.recipeId ?: return
        
        viewModelScope.launch {
            setLoading(true)
            setError(null)
            
            photoAssociationService.getPhotosOrganizedByStage(recipeId)
                .onSuccess { photosByStage ->
                    val allPhotos = photosByStage.values.flatten()
                    currentState = currentState.copy(
                        photos = allPhotos,
                        photosByStage = photosByStage,
                        isLoading = false,
                        lastRefreshTime = System.currentTimeMillis()
                    )
                    setLoading(false)
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to load photos")
                    setLoading(false)
                }
        }
    }

    fun selectStage(stage: PhotoStage) {
        currentState = currentState.copy(selectedStage = stage)
    }

    fun capturePhoto(caption: String? = null) {
        viewModelScope.launch {
            currentState = currentState.copy(isCapturing = true)
            setError(null)
            
            photoCaptureService.capturePhoto(
                stage = currentState.selectedStage,
                caption = caption
            )
                .onSuccess { photo ->
                    currentState = currentState.copy(isCapturing = false)
                    loadPhotos() // Reload to show new photo
                }
                .onFailure { error ->
                    currentState = currentState.copy(isCapturing = false)
                    setError(error.message ?: "Failed to capture photo")
                }
        }
    }

    fun importPhoto(sourcePath: String, caption: String? = null) {
        viewModelScope.launch {
            setLoading(true)
            setError(null)
            
            photoCaptureService.importPhoto(
                sourcePath = sourcePath,
                stage = currentState.selectedStage,
                caption = caption
            )
                .onSuccess { photo ->
                    setLoading(false)
                    loadPhotos() // Reload to show new photo
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to import photo")
                    setLoading(false)
                }
        }
    }

    fun selectPhoto(photo: Photo) {
        currentState = currentState.copy(
            selectedPhoto = photo,
            editingCaption = photo.caption ?: ""
        )
    }

    fun deselectPhoto() {
        currentState = currentState.copy(
            selectedPhoto = null,
            captionEditMode = false,
            editingCaption = ""
        )
    }

    fun enableCaptionEdit() {
        currentState = currentState.copy(captionEditMode = true)
    }

    fun updateEditingCaption(caption: String) {
        currentState = currentState.copy(editingCaption = caption)
    }

    fun saveCaption() {
        val photo = currentState.selectedPhoto ?: return
        val caption = currentState.editingCaption
        
        viewModelScope.launch {
            photoCaptureService.updatePhotoCaption(photo.id, caption)
                .onSuccess { updatedPhoto ->
                    currentState = currentState.copy(
                        selectedPhoto = updatedPhoto,
                        captionEditMode = false
                    )
                    loadPhotos() // Reload to show updated caption
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to update caption")
                }
        }
    }

    fun cancelCaptionEdit() {
        currentState = currentState.copy(
            captionEditMode = false,
            editingCaption = currentState.selectedPhoto?.caption ?: ""
        )
    }

    fun deletePhoto(photoId: String) {
        viewModelScope.launch {
            setLoading(true)
            setError(null)
            
            photoAssociationService.deletePhotoWithAssociations(photoId)
                .onSuccess {
                    currentState = currentState.copy(
                        selectedPhoto = null
                    )
                    setLoading(false)
                    loadPhotos() // Reload to remove deleted photo
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to delete photo")
                    setLoading(false)
                }
        }
    }

    fun tagPhotoToIngredient(photoId: String, ingredientId: String) {
        viewModelScope.launch {
            photoAssociationService.tagPhotoToIngredient(photoId, ingredientId)
                .onSuccess {
                    loadPhotos() // Reload to show updated associations
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to tag photo to ingredient")
                }
        }
    }

    fun tagPhotoToCookingStep(photoId: String, stepId: String) {
        viewModelScope.launch {
            photoAssociationService.tagPhotoToCookingStep(photoId, stepId)
                .onSuccess {
                    loadPhotos() // Reload to show updated associations
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to tag photo to cooking step")
                }
        }
    }

    fun getPhotosForStage(stage: PhotoStage): List<Photo> {
        return currentState.photosByStage[stage] ?: emptyList()
    }
    
    override fun onAppResumed() {
        // Refresh photos when app comes back to foreground
        loadPhotos()
    }
    
    override fun serializeState(state: PhotoManagementState): String? {
        return try {
            Json.encodeToString(state)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun deserializeState(serializedState: String): PhotoManagementState? {
        return try {
            Json.decodeFromString<PhotoManagementState>(serializedState)
        } catch (e: Exception) {
            null
        }
    }
}
