package com.recipemanager.domain.service

import com.recipemanager.data.repository.PhotoRepositoryImpl
import com.recipemanager.domain.model.Photo
import com.recipemanager.domain.model.PhotoStage

/**
 * Service for managing photo associations with recipe stages.
 * Handles tagging photos to ingredients and cooking steps.
 */
class PhotoAssociationService(
    private val photoRepository: PhotoRepositoryImpl
) {
    
    /**
     * Tag a photo to an ingredient.
     * @param photoId The ID of the photo to tag
     * @param ingredientId The ID of the ingredient
     * @return Result indicating success or failure
     */
    suspend fun tagPhotoToIngredient(photoId: String, ingredientId: String): Result<Unit> {
        return try {
            // Verify photo exists
            val photoResult = photoRepository.getPhoto(photoId)
            if (photoResult.isFailure) {
                return Result.failure(photoResult.exceptionOrNull()!!)
            }
            
            val photo = photoResult.getOrNull()
                ?: return Result.failure(IllegalArgumentException("Photo not found: $photoId"))
            
            // Verify photo stage is appropriate for ingredients
            if (photo.stage != PhotoStage.RAW_INGREDIENTS && 
                photo.stage != PhotoStage.PROCESSED_INGREDIENTS) {
                return Result.failure(
                    IllegalArgumentException(
                        "Photo stage ${photo.stage} is not valid for ingredient association"
                    )
                )
            }
            
            photoRepository.associatePhotoWithIngredient(photoId, ingredientId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Tag a photo to a cooking step.
     * @param photoId The ID of the photo to tag
     * @param stepId The ID of the cooking step
     * @return Result indicating success or failure
     */
    suspend fun tagPhotoToCookingStep(photoId: String, stepId: String): Result<Unit> {
        return try {
            // Verify photo exists
            val photoResult = photoRepository.getPhoto(photoId)
            if (photoResult.isFailure) {
                return Result.failure(photoResult.exceptionOrNull()!!)
            }
            
            val photo = photoResult.getOrNull()
                ?: return Result.failure(IllegalArgumentException("Photo not found: $photoId"))
            
            // Verify photo stage is appropriate for cooking steps
            if (photo.stage != PhotoStage.COOKING_STEP && 
                photo.stage != PhotoStage.FINAL_RESULT) {
                return Result.failure(
                    IllegalArgumentException(
                        "Photo stage ${photo.stage} is not valid for cooking step association"
                    )
                )
            }
            
            photoRepository.associatePhotoWithCookingStep(photoId, stepId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all photos associated with a specific ingredient.
     * @param ingredientId The ID of the ingredient
     * @return Result containing list of photos
     */
    suspend fun getPhotosForIngredient(ingredientId: String): Result<List<Photo>> {
        return photoRepository.getPhotosByIngredientId(ingredientId)
    }
    
    /**
     * Get all photos associated with a specific cooking step.
     * @param stepId The ID of the cooking step
     * @return Result containing list of photos
     */
    suspend fun getPhotosForCookingStep(stepId: String): Result<List<Photo>> {
        return photoRepository.getPhotosByStepId(stepId)
    }
    
    /**
     * Get all photos for a specific stage.
     * @param recipeId The ID of the recipe
     * @param stage The photo stage to filter by
     * @return Result containing list of photos
     */
    suspend fun getPhotosByStage(recipeId: String, stage: PhotoStage): Result<List<Photo>> {
        return photoRepository.getPhotosByStage(recipeId, stage)
    }
    
    /**
     * Remove photo tag from an ingredient.
     * @param photoId The ID of the photo
     * @param ingredientId The ID of the ingredient
     * @return Result indicating success or failure
     */
    suspend fun removePhotoFromIngredient(photoId: String, ingredientId: String): Result<Unit> {
        return photoRepository.removePhotoIngredientAssociation(photoId, ingredientId)
    }
    
    /**
     * Remove photo tag from a cooking step.
     * @param photoId The ID of the photo
     * @param stepId The ID of the cooking step
     * @return Result indicating success or failure
     */
    suspend fun removePhotoFromCookingStep(photoId: String, stepId: String): Result<Unit> {
        return photoRepository.removePhotoCookingStepAssociation(photoId, stepId)
    }
    
    /**
     * Delete a photo and all its associations.
     * This will cascade delete all associations due to foreign key constraints.
     * @param photoId The ID of the photo to delete
     * @return Result indicating success or failure
     */
    suspend fun deletePhotoWithAssociations(photoId: String): Result<Unit> {
        return photoRepository.deletePhoto(photoId)
    }
    
    /**
     * Get photos organized by stage for a recipe.
     * Returns a map of PhotoStage to list of photos.
     * @param recipeId The ID of the recipe
     * @return Result containing map of stage to photos
     */
    suspend fun getPhotosOrganizedByStage(recipeId: String): Result<Map<PhotoStage, List<Photo>>> {
        return try {
            val photosByStage = mutableMapOf<PhotoStage, List<Photo>>()
            
            for (stage in PhotoStage.values()) {
                val result = photoRepository.getPhotosByStage(recipeId, stage)
                if (result.isSuccess) {
                    photosByStage[stage] = result.getOrNull() ?: emptyList()
                }
            }
            
            Result.success(photosByStage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
