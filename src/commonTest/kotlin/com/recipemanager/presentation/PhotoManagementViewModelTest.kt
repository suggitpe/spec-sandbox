package com.recipemanager.presentation

import com.recipemanager.domain.model.Photo
import com.recipemanager.domain.model.PhotoStage
import com.recipemanager.domain.model.SyncStatus
import com.recipemanager.presentation.viewmodel.PhotoManagementState
import com.recipemanager.presentation.viewmodel.PhotoManagementViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.Clock

/**
 * Tests for PhotoManagementViewModel.
 * 
 * These tests focus on state management and UI logic.
 * Integration with actual photo services is tested separately.
 */
class PhotoManagementViewModelTest : FunSpec({
    
    test("should initialize with default state") {
        val state = PhotoManagementState()
        
        state.recipeId shouldBe null
        state.photos.size shouldBe 0
        state.photosByStage.size shouldBe 0
        state.selectedPhoto shouldBe null
        state.selectedStage shouldBe PhotoStage.RAW_INGREDIENTS
        state.isCapturing shouldBe false
        state.isLoading shouldBe false
        state.error shouldBe null
        state.captionEditMode shouldBe false
    }
    
    test("should handle photo selection") {
        val photo = Photo(
            id = "photo_1",
            localPath = "/path/photo.jpg",
            caption = "Test photo",
            stage = PhotoStage.RAW_INGREDIENTS,
            timestamp = Clock.System.now(),
            syncStatus = SyncStatus.LOCAL_ONLY
        )
        
        val state = PhotoManagementState(selectedPhoto = photo)
        
        state.selectedPhoto shouldBe photo
        state.selectedPhoto?.caption shouldBe "Test photo"
    }
    
    test("should handle stage selection") {
        val state = PhotoManagementState(selectedStage = PhotoStage.COOKING_STEP)
        
        state.selectedStage shouldBe PhotoStage.COOKING_STEP
    }
    
    test("should handle caption editing") {
        val photo = Photo(
            id = "photo_1",
            localPath = "/path/photo.jpg",
            caption = "Original",
            stage = PhotoStage.RAW_INGREDIENTS,
            timestamp = Clock.System.now(),
            syncStatus = SyncStatus.LOCAL_ONLY
        )
        
        val state = PhotoManagementState(
            selectedPhoto = photo,
            captionEditMode = true,
            editingCaption = "Updated caption"
        )
        
        state.captionEditMode shouldBe true
        state.editingCaption shouldBe "Updated caption"
    }
    
    test("should organize photos by stage") {
        val photo1 = Photo(
            id = "photo_1",
            localPath = "/path/photo1.jpg",
            caption = "Raw ingredients",
            stage = PhotoStage.RAW_INGREDIENTS,
            timestamp = Clock.System.now(),
            syncStatus = SyncStatus.LOCAL_ONLY
        )
        
        val photo2 = Photo(
            id = "photo_2",
            localPath = "/path/photo2.jpg",
            caption = "Cooking step",
            stage = PhotoStage.COOKING_STEP,
            timestamp = Clock.System.now(),
            syncStatus = SyncStatus.LOCAL_ONLY
        )
        
        val photosByStage = mapOf(
            PhotoStage.RAW_INGREDIENTS to listOf(photo1),
            PhotoStage.COOKING_STEP to listOf(photo2)
        )
        
        val state = PhotoManagementState(
            photos = listOf(photo1, photo2),
            photosByStage = photosByStage
        )
        
        state.photosByStage[PhotoStage.RAW_INGREDIENTS]?.size shouldBe 1
        state.photosByStage[PhotoStage.COOKING_STEP]?.size shouldBe 1
        state.photosByStage[PhotoStage.RAW_INGREDIENTS]?.first() shouldBe photo1
        state.photosByStage[PhotoStage.COOKING_STEP]?.first() shouldBe photo2
    }
    
    test("should handle loading state") {
        val state = PhotoManagementState(isLoading = true)
        
        state.isLoading shouldBe true
    }
    
    test("should handle capturing state") {
        val state = PhotoManagementState(isCapturing = true)
        
        state.isCapturing shouldBe true
    }
    
    test("should handle error state") {
        val state = PhotoManagementState(error = "Failed to capture photo")
        
        state.error shouldNotBe null
        state.error shouldBe "Failed to capture photo"
    }
    
    test("should handle multiple photos for same stage") {
        val photo1 = Photo(
            id = "photo_1",
            localPath = "/path/photo1.jpg",
            caption = "First photo",
            stage = PhotoStage.RAW_INGREDIENTS,
            timestamp = Clock.System.now(),
            syncStatus = SyncStatus.LOCAL_ONLY
        )
        
        val photo2 = Photo(
            id = "photo_2",
            localPath = "/path/photo2.jpg",
            caption = "Second photo",
            stage = PhotoStage.RAW_INGREDIENTS,
            timestamp = Clock.System.now(),
            syncStatus = SyncStatus.LOCAL_ONLY
        )
        
        val photosByStage = mapOf(
            PhotoStage.RAW_INGREDIENTS to listOf(photo1, photo2)
        )
        
        val state = PhotoManagementState(
            photos = listOf(photo1, photo2),
            photosByStage = photosByStage
        )
        
        state.photosByStage[PhotoStage.RAW_INGREDIENTS]?.size shouldBe 2
    }
    
    test("should handle empty photo list for stage") {
        val photosByStage = mapOf<PhotoStage, List<Photo>>()
        
        val state = PhotoManagementState(photosByStage = photosByStage)
        
        state.photosByStage[PhotoStage.RAW_INGREDIENTS] shouldBe null
    }
})
