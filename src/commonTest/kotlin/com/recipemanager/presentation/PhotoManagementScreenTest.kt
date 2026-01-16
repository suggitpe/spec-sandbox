package com.recipemanager.presentation

import com.recipemanager.domain.model.Photo
import com.recipemanager.domain.model.PhotoStage
import com.recipemanager.domain.model.SyncStatus
import com.recipemanager.presentation.screens.PhotoManagementScreen
import com.recipemanager.presentation.viewmodel.PhotoManagementState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.datetime.Clock

class PhotoManagementScreenTest : FunSpec({
    
    test("should render empty photo management screen") {
        val state = PhotoManagementState(
            recipeId = "recipe_123",
            selectedStage = PhotoStage.RAW_INGREDIENTS
        )
        
        val rendered = PhotoManagementScreen.render(
            state = state,
            onStageSelected = {},
            onCapturePhoto = {},
            onImportPhoto = {},
            onPhotoSelected = {},
            onDeletePhoto = {},
            onBack = {}
        )
        
        rendered shouldContain "Photo Management"
        rendered shouldContain "Select Stage:"
        rendered shouldContain "Raw Ingredients (0 photos)"
        rendered shouldContain "Capture Photo"
        rendered shouldContain "Import from Gallery"
        rendered shouldContain "No photos for Raw Ingredients"
    }
    
    test("should render photo management screen with photos") {
        val photos = listOf(
            Photo(
                id = "photo_1",
                localPath = "/path/photo1.jpg",
                caption = "Fresh tomatoes",
                stage = PhotoStage.RAW_INGREDIENTS,
                timestamp = Clock.System.now(),
                syncStatus = SyncStatus.LOCAL_ONLY
            ),
            Photo(
                id = "photo_2",
                localPath = "/path/photo2.jpg",
                caption = "Chopped vegetables",
                stage = PhotoStage.PROCESSED_INGREDIENTS,
                timestamp = Clock.System.now(),
                syncStatus = SyncStatus.SYNCED
            )
        )
        
        val photosByStage = mapOf(
            PhotoStage.RAW_INGREDIENTS to listOf(photos[0]),
            PhotoStage.PROCESSED_INGREDIENTS to listOf(photos[1])
        )
        
        val state = PhotoManagementState(
            recipeId = "recipe_123",
            photos = photos,
            photosByStage = photosByStage,
            selectedStage = PhotoStage.RAW_INGREDIENTS
        )
        
        val rendered = PhotoManagementScreen.render(
            state = state,
            onStageSelected = {},
            onCapturePhoto = {},
            onImportPhoto = {},
            onPhotoSelected = {},
            onDeletePhoto = {},
            onBack = {}
        )
        
        rendered shouldContain "Raw Ingredients (1 photos)"
        rendered shouldContain "Processed Ingredients (1 photos)"
        rendered shouldContain "Fresh tomatoes"
        rendered shouldContain "/path/photo1.jpg"
        rendered shouldNotContain "No photos for Raw Ingredients"
    }
    
    test("should indicate selected stage") {
        val state = PhotoManagementState(
            recipeId = "recipe_123",
            selectedStage = PhotoStage.COOKING_STEP
        )
        
        val rendered = PhotoManagementScreen.render(
            state = state,
            onStageSelected = {},
            onCapturePhoto = {},
            onImportPhoto = {},
            onPhotoSelected = {},
            onDeletePhoto = {},
            onBack = {}
        )
        
        rendered shouldContain "> Cooking Step"
        rendered shouldContain "  Raw Ingredients"
    }
    
    test("should show loading state") {
        val state = PhotoManagementState(
            recipeId = "recipe_123",
            isLoading = true
        )
        
        val rendered = PhotoManagementScreen.render(
            state = state,
            onStageSelected = {},
            onCapturePhoto = {},
            onImportPhoto = {},
            onPhotoSelected = {},
            onDeletePhoto = {},
            onBack = {}
        )
        
        rendered shouldContain "Loading..."
    }
    
    test("should show capturing state") {
        val state = PhotoManagementState(
            recipeId = "recipe_123",
            isCapturing = true
        )
        
        val rendered = PhotoManagementScreen.render(
            state = state,
            onStageSelected = {},
            onCapturePhoto = {},
            onImportPhoto = {},
            onPhotoSelected = {},
            onDeletePhoto = {},
            onBack = {}
        )
        
        rendered shouldContain "Capturing photo..."
    }
    
    test("should show error message") {
        val state = PhotoManagementState(
            recipeId = "recipe_123",
            error = "Failed to capture photo"
        )
        
        val rendered = PhotoManagementScreen.render(
            state = state,
            onStageSelected = {},
            onCapturePhoto = {},
            onImportPhoto = {},
            onPhotoSelected = {},
            onDeletePhoto = {},
            onBack = {}
        )
        
        rendered shouldContain "Error: Failed to capture photo"
    }
    
    test("should render photo detail view") {
        val photo = Photo(
            id = "photo_1",
            localPath = "/path/photo.jpg",
            caption = "Test photo",
            stage = PhotoStage.RAW_INGREDIENTS,
            timestamp = Clock.System.now(),
            syncStatus = SyncStatus.LOCAL_ONLY
        )
        
        val state = PhotoManagementState(
            selectedPhoto = photo
        )
        
        val rendered = PhotoManagementScreen.renderPhotoDetail(
            photo = photo,
            state = state,
            onUpdateCaption = {},
            onSaveCaption = {},
            onCancelEdit = {},
            onEnableEdit = {},
            onDelete = {},
            onClose = {}
        )
        
        rendered shouldContain "Photo Detail"
        rendered shouldContain "Photo ID: photo_1"
        rendered shouldContain "Stage: Raw Ingredients"
        rendered shouldContain "Caption: Test photo"
        rendered shouldContain "Edit Caption"
        rendered shouldContain "Delete Photo"
    }
    
    test("should render photo detail in edit mode") {
        val photo = Photo(
            id = "photo_1",
            localPath = "/path/photo.jpg",
            caption = "Original caption",
            stage = PhotoStage.RAW_INGREDIENTS,
            timestamp = Clock.System.now(),
            syncStatus = SyncStatus.LOCAL_ONLY
        )
        
        val state = PhotoManagementState(
            selectedPhoto = photo,
            captionEditMode = true,
            editingCaption = "Updated caption"
        )
        
        val rendered = PhotoManagementScreen.renderPhotoDetail(
            photo = photo,
            state = state,
            onUpdateCaption = {},
            onSaveCaption = {},
            onCancelEdit = {},
            onEnableEdit = {},
            onDelete = {},
            onClose = {}
        )
        
        rendered shouldContain "Caption (editing):"
        rendered shouldContain "Updated caption"
        rendered shouldContain "Save Caption"
        rendered shouldContain "Cancel"
    }
    
    test("should render photo gallery with no photos") {
        val state = PhotoManagementState(
            recipeId = "recipe_123",
            photos = emptyList()
        )
        
        val rendered = PhotoManagementScreen.renderPhotoGallery(
            state = state,
            onPhotoSelected = {},
            onStageSelected = {},
            onBack = {}
        )
        
        rendered shouldContain "Photo Gallery"
        rendered shouldContain "No photos available"
    }
    
    test("should render photo gallery organized by stages") {
        val photos = listOf(
            Photo(
                id = "photo_1",
                localPath = "/path/photo1.jpg",
                caption = "Raw ingredients",
                stage = PhotoStage.RAW_INGREDIENTS,
                timestamp = Clock.System.now(),
                syncStatus = SyncStatus.LOCAL_ONLY
            ),
            Photo(
                id = "photo_2",
                localPath = "/path/photo2.jpg",
                caption = "Cooking in progress",
                stage = PhotoStage.COOKING_STEP,
                timestamp = Clock.System.now(),
                syncStatus = SyncStatus.LOCAL_ONLY
            ),
            Photo(
                id = "photo_3",
                localPath = "/path/photo3.jpg",
                caption = "Final dish",
                stage = PhotoStage.FINAL_RESULT,
                timestamp = Clock.System.now(),
                syncStatus = SyncStatus.SYNCED
            )
        )
        
        val photosByStage = mapOf(
            PhotoStage.RAW_INGREDIENTS to listOf(photos[0]),
            PhotoStage.COOKING_STEP to listOf(photos[1]),
            PhotoStage.FINAL_RESULT to listOf(photos[2])
        )
        
        val state = PhotoManagementState(
            recipeId = "recipe_123",
            photos = photos,
            photosByStage = photosByStage
        )
        
        val rendered = PhotoManagementScreen.renderPhotoGallery(
            state = state,
            onPhotoSelected = {},
            onStageSelected = {},
            onBack = {}
        )
        
        rendered shouldContain "--- Raw Ingredients (1) ---"
        rendered shouldContain "Raw ingredients"
        rendered shouldContain "--- Cooking Step (1) ---"
        rendered shouldContain "Cooking in progress"
        rendered shouldContain "--- Final Result (1) ---"
        rendered shouldContain "Final dish"
    }
    
    test("should render photo tagging for ingredient stage") {
        val photo = Photo(
            id = "photo_1",
            localPath = "/path/photo.jpg",
            caption = "Tomatoes",
            stage = PhotoStage.RAW_INGREDIENTS,
            timestamp = Clock.System.now(),
            syncStatus = SyncStatus.LOCAL_ONLY
        )
        
        val ingredients = listOf(
            "ing_1" to "Tomatoes",
            "ing_2" to "Onions",
            "ing_3" to "Garlic"
        )
        
        val rendered = PhotoManagementScreen.renderPhotoTagging(
            photo = photo,
            availableIngredients = ingredients,
            availableSteps = emptyList(),
            onTagToIngredient = {},
            onTagToStep = {},
            onClose = {}
        )
        
        rendered shouldContain "Tag Photo"
        rendered shouldContain "Photo: Tomatoes"
        rendered shouldContain "Stage: Raw Ingredients"
        rendered shouldContain "Tag to Ingredient:"
        rendered shouldContain "1. Tomatoes"
        rendered shouldContain "2. Onions"
        rendered shouldContain "3. Garlic"
    }
    
    test("should render photo tagging for cooking step stage") {
        val photo = Photo(
            id = "photo_1",
            localPath = "/path/photo.jpg",
            caption = "Simmering",
            stage = PhotoStage.COOKING_STEP,
            timestamp = Clock.System.now(),
            syncStatus = SyncStatus.LOCAL_ONLY
        )
        
        val steps = listOf(
            "step_1" to "Heat oil in a large pan over medium heat",
            "step_2" to "Add onions and cook until translucent, about 5 minutes",
            "step_3" to "Add tomatoes and simmer for 20 minutes"
        )
        
        val rendered = PhotoManagementScreen.renderPhotoTagging(
            photo = photo,
            availableIngredients = emptyList(),
            availableSteps = steps,
            onTagToIngredient = {},
            onTagToStep = {},
            onClose = {}
        )
        
        rendered shouldContain "Tag Photo"
        rendered shouldContain "Photo: Simmering"
        rendered shouldContain "Stage: Cooking Step"
        rendered shouldContain "Tag to Cooking Step:"
        rendered shouldContain "1. Heat oil in a large pan over medium heat..."
        rendered shouldContain "2. Add onions and cook until translucent, about"
    }
    
    test("should show no tagging options when none available") {
        val photo = Photo(
            id = "photo_1",
            localPath = "/path/photo.jpg",
            stage = PhotoStage.RAW_INGREDIENTS,
            timestamp = Clock.System.now(),
            syncStatus = SyncStatus.LOCAL_ONLY
        )
        
        val rendered = PhotoManagementScreen.renderPhotoTagging(
            photo = photo,
            availableIngredients = emptyList(),
            availableSteps = emptyList(),
            onTagToIngredient = {},
            onTagToStep = {},
            onClose = {}
        )
        
        rendered shouldContain "No ingredients available for tagging"
    }
})
