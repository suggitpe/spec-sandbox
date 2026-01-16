package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.Photo
import com.recipemanager.domain.model.PhotoStage
import com.recipemanager.domain.service.PhotoAssociationService
import com.recipemanager.domain.service.PhotoCaptureService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val editingCaption: String = ""
)

class PhotoManagementViewModel(
    private val photoCaptureService: PhotoCaptureService,
    private val photoAssociationService: PhotoAssociationService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(PhotoManagementState())
    val state: StateFlow<PhotoManagementState> = _state.asStateFlow()

    fun setRecipeId(recipeId: String) {
        _state.value = _state.value.copy(recipeId = recipeId)
        loadPhotos()
    }

    fun loadPhotos() {
        val recipeId = _state.value.recipeId ?: return
        
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            photoAssociationService.getPhotosOrganizedByStage(recipeId)
                .onSuccess { photosByStage ->
                    val allPhotos = photosByStage.values.flatten()
                    _state.value = _state.value.copy(
                        photos = allPhotos,
                        photosByStage = photosByStage,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load photos"
                    )
                }
        }
    }

    fun selectStage(stage: PhotoStage) {
        _state.value = _state.value.copy(selectedStage = stage)
    }

    fun capturePhoto(caption: String? = null) {
        scope.launch {
            _state.value = _state.value.copy(isCapturing = true, error = null)
            
            photoCaptureService.capturePhoto(
                stage = _state.value.selectedStage,
                caption = caption
            )
                .onSuccess { photo ->
                    _state.value = _state.value.copy(isCapturing = false)
                    loadPhotos() // Reload to show new photo
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isCapturing = false,
                        error = error.message ?: "Failed to capture photo"
                    )
                }
        }
    }

    fun importPhoto(sourcePath: String, caption: String? = null) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            photoCaptureService.importPhoto(
                sourcePath = sourcePath,
                stage = _state.value.selectedStage,
                caption = caption
            )
                .onSuccess { photo ->
                    _state.value = _state.value.copy(isLoading = false)
                    loadPhotos() // Reload to show new photo
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to import photo"
                    )
                }
        }
    }

    fun selectPhoto(photo: Photo) {
        _state.value = _state.value.copy(
            selectedPhoto = photo,
            editingCaption = photo.caption ?: ""
        )
    }

    fun deselectPhoto() {
        _state.value = _state.value.copy(
            selectedPhoto = null,
            captionEditMode = false,
            editingCaption = ""
        )
    }

    fun enableCaptionEdit() {
        _state.value = _state.value.copy(captionEditMode = true)
    }

    fun updateEditingCaption(caption: String) {
        _state.value = _state.value.copy(editingCaption = caption)
    }

    fun saveCaption() {
        val photo = _state.value.selectedPhoto ?: return
        val caption = _state.value.editingCaption
        
        scope.launch {
            photoCaptureService.updatePhotoCaption(photo.id, caption)
                .onSuccess { updatedPhoto ->
                    _state.value = _state.value.copy(
                        selectedPhoto = updatedPhoto,
                        captionEditMode = false
                    )
                    loadPhotos() // Reload to show updated caption
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: "Failed to update caption"
                    )
                }
        }
    }

    fun cancelCaptionEdit() {
        _state.value = _state.value.copy(
            captionEditMode = false,
            editingCaption = _state.value.selectedPhoto?.caption ?: ""
        )
    }

    fun deletePhoto(photoId: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            photoAssociationService.deletePhotoWithAssociations(photoId)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        selectedPhoto = null
                    )
                    loadPhotos() // Reload to remove deleted photo
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to delete photo"
                    )
                }
        }
    }

    fun tagPhotoToIngredient(photoId: String, ingredientId: String) {
        scope.launch {
            photoAssociationService.tagPhotoToIngredient(photoId, ingredientId)
                .onSuccess {
                    loadPhotos() // Reload to show updated associations
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: "Failed to tag photo to ingredient"
                    )
                }
        }
    }

    fun tagPhotoToCookingStep(photoId: String, stepId: String) {
        scope.launch {
            photoAssociationService.tagPhotoToCookingStep(photoId, stepId)
                .onSuccess {
                    loadPhotos() // Reload to show updated associations
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: "Failed to tag photo to cooking step"
                    )
                }
        }
    }

    fun getPhotosForStage(stage: PhotoStage): List<Photo> {
        return _state.value.photosByStage[stage] ?: emptyList()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
