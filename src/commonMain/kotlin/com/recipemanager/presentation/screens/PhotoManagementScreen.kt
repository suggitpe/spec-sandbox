package com.recipemanager.presentation.screens

import com.recipemanager.domain.model.Photo
import com.recipemanager.domain.model.PhotoStage
import com.recipemanager.presentation.viewmodel.PhotoManagementState

/**
 * Photo Management Screen
 * 
 * This screen provides a comprehensive interface for managing photos associated with recipes.
 * It supports:
 * - Photo capture using device camera
 * - Photo import from gallery
 * - Stage-specific photo organization (Raw Ingredients, Processed Ingredients, Cooking Steps, Final Result)
 * - Photo tagging and caption management
 * - Photo deletion with cleanup
 * 
 * Requirements: 2.1, 2.2, 2.6, 2.7
 */
object PhotoManagementScreen {
    
    /**
     * Render the main photo management interface.
     * 
     * @param state Current photo management state
     * @param onStageSelected Callback when a photo stage is selected
     * @param onCapturePhoto Callback to capture a new photo
     * @param onImportPhoto Callback to import a photo from gallery
     * @param onPhotoSelected Callback when a photo is selected for viewing/editing
     * @param onDeletePhoto Callback to delete a photo
     * @param onBack Callback to navigate back
     */
    fun render(
        state: PhotoManagementState,
        onStageSelected: (PhotoStage) -> Unit,
        onCapturePhoto: () -> Unit,
        onImportPhoto: () -> Unit,
        onPhotoSelected: (Photo) -> Unit,
        onDeletePhoto: (String) -> Unit,
        onBack: () -> Unit
    ): String {
        return buildString {
            appendLine("=== Photo Management ===")
            appendLine()
            
            // Stage selector
            appendLine("Select Stage:")
            PhotoStage.values().forEach { stage ->
                val indicator = if (stage == state.selectedStage) ">" else " "
                val count = state.photosByStage[stage]?.size ?: 0
                appendLine("$indicator ${stage.displayName()} ($count photos)")
            }
            appendLine()
            
            // Action buttons
            appendLine("Actions:")
            appendLine("1. Capture Photo")
            appendLine("2. Import from Gallery")
            appendLine("3. Back")
            appendLine()
            
            // Display photos for selected stage
            val photosForStage = state.photosByStage[state.selectedStage] ?: emptyList()
            if (photosForStage.isNotEmpty()) {
                appendLine("Photos for ${state.selectedStage.displayName()}:")
                photosForStage.forEachIndexed { index, photo ->
                    appendLine("${index + 1}. ${photo.caption ?: "Untitled"}")
                    appendLine("   Path: ${photo.localPath}")
                    appendLine("   Timestamp: ${photo.timestamp}")
                    appendLine("   Sync: ${photo.syncStatus}")
                }
            } else {
                appendLine("No photos for ${state.selectedStage.displayName()}")
            }
            
            // Loading/error states
            if (state.isLoading) {
                appendLine()
                appendLine("Loading...")
            }
            
            if (state.isCapturing) {
                appendLine()
                appendLine("Capturing photo...")
            }
            
            state.error?.let { error ->
                appendLine()
                appendLine("Error: $error")
            }
        }
    }
    
    /**
     * Render the photo detail view for viewing and editing a single photo.
     * 
     * @param photo The photo to display
     * @param state Current photo management state
     * @param onUpdateCaption Callback to update photo caption
     * @param onSaveCaption Callback to save caption changes
     * @param onCancelEdit Callback to cancel caption editing
     * @param onEnableEdit Callback to enable caption editing
     * @param onDelete Callback to delete the photo
     * @param onClose Callback to close the detail view
     */
    fun renderPhotoDetail(
        photo: Photo,
        state: PhotoManagementState,
        onUpdateCaption: (String) -> Unit,
        onSaveCaption: () -> Unit,
        onCancelEdit: () -> Unit,
        onEnableEdit: () -> Unit,
        onDelete: () -> Unit,
        onClose: () -> Unit
    ): String {
        return buildString {
            appendLine("=== Photo Detail ===")
            appendLine()
            appendLine("Photo ID: ${photo.id}")
            appendLine("Stage: ${photo.stage.displayName()}")
            appendLine("Path: ${photo.localPath}")
            appendLine("Timestamp: ${photo.timestamp}")
            appendLine("Sync Status: ${photo.syncStatus}")
            appendLine()
            
            // Caption section
            if (state.captionEditMode) {
                appendLine("Caption (editing):")
                appendLine(state.editingCaption)
                appendLine()
                appendLine("1. Save Caption")
                appendLine("2. Cancel")
            } else {
                appendLine("Caption: ${photo.caption ?: "(No caption)"}")
                appendLine()
                appendLine("1. Edit Caption")
            }
            
            appendLine("2. Delete Photo")
            appendLine("3. Close")
            
            state.error?.let { error ->
                appendLine()
                appendLine("Error: $error")
            }
        }
    }
    
    /**
     * Render the photo gallery view organized by stages.
     * Shows all photos grouped by their stage for easy navigation.
     * 
     * @param state Current photo management state
     * @param onPhotoSelected Callback when a photo is selected
     * @param onStageSelected Callback when a stage filter is selected
     * @param onBack Callback to navigate back
     */
    fun renderPhotoGallery(
        state: PhotoManagementState,
        onPhotoSelected: (Photo) -> Unit,
        onStageSelected: (PhotoStage) -> Unit,
        onBack: () -> Unit
    ): String {
        return buildString {
            appendLine("=== Photo Gallery ===")
            appendLine()
            
            if (state.photos.isEmpty()) {
                appendLine("No photos available")
                appendLine()
                appendLine("1. Back")
                return@buildString
            }
            
            // Display photos organized by stage
            PhotoStage.values().forEach { stage ->
                val photosForStage = state.photosByStage[stage] ?: emptyList()
                if (photosForStage.isNotEmpty()) {
                    appendLine("--- ${stage.displayName()} (${photosForStage.size}) ---")
                    photosForStage.forEach { photo ->
                        appendLine("  • ${photo.caption ?: "Untitled"}")
                        appendLine("    ${photo.timestamp}")
                    }
                    appendLine()
                }
            }
            
            appendLine("Actions:")
            appendLine("1. Filter by Stage")
            appendLine("2. Back")
            
            if (state.isLoading) {
                appendLine()
                appendLine("Loading...")
            }
            
            state.error?.let { error ->
                appendLine()
                appendLine("Error: $error")
            }
        }
    }
    
    /**
     * Render the photo tagging interface for associating photos with ingredients or cooking steps.
     * 
     * @param photo The photo to tag
     * @param availableIngredients List of ingredients that can be tagged
     * @param availableSteps List of cooking steps that can be tagged
     * @param onTagToIngredient Callback to tag photo to an ingredient
     * @param onTagToStep Callback to tag photo to a cooking step
     * @param onClose Callback to close the tagging interface
     */
    fun renderPhotoTagging(
        photo: Photo,
        availableIngredients: List<Pair<String, String>>, // (id, name)
        availableSteps: List<Pair<String, String>>, // (id, instruction)
        onTagToIngredient: (String) -> Unit,
        onTagToStep: (String) -> Unit,
        onClose: () -> Unit
    ): String {
        return buildString {
            appendLine("=== Tag Photo ===")
            appendLine()
            appendLine("Photo: ${photo.caption ?: "Untitled"}")
            appendLine("Stage: ${photo.stage.displayName()}")
            appendLine()
            
            // Show appropriate tagging options based on photo stage
            when (photo.stage) {
                PhotoStage.RAW_INGREDIENTS, PhotoStage.PROCESSED_INGREDIENTS -> {
                    if (availableIngredients.isNotEmpty()) {
                        appendLine("Tag to Ingredient:")
                        availableIngredients.forEachIndexed { index, (id, name) ->
                            appendLine("${index + 1}. $name")
                        }
                    } else {
                        appendLine("No ingredients available for tagging")
                    }
                }
                PhotoStage.COOKING_STEP, PhotoStage.FINAL_RESULT -> {
                    if (availableSteps.isNotEmpty()) {
                        appendLine("Tag to Cooking Step:")
                        availableSteps.forEachIndexed { index, (id, instruction) ->
                            appendLine("${index + 1}. ${instruction.take(50)}...")
                        }
                    } else {
                        appendLine("No cooking steps available for tagging")
                    }
                }
            }
            
            appendLine()
            appendLine("0. Close")
        }
    }
}

/**
 * Extension function to get display name for PhotoStage enum.
 */
private fun PhotoStage.displayName(): String {
    return when (this) {
        PhotoStage.RAW_INGREDIENTS -> "Raw Ingredients"
        PhotoStage.PROCESSED_INGREDIENTS -> "Processed Ingredients"
        PhotoStage.COOKING_STEP -> "Cooking Step"
        PhotoStage.FINAL_RESULT -> "Final Result"
    }
}
