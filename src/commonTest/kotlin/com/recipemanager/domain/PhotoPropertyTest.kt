package com.recipemanager.domain

import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.repository.PhotoRepositoryImpl
import com.recipemanager.data.storage.PhotoStorage
import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.model.*
import com.recipemanager.domain.service.PhotoAssociationService
import com.recipemanager.test.generators.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.datetime.Clock

/**
 * Property-based tests for photo management functionality.
 * Tests photo-stage associations and photo processing consistency.
 */
class PhotoPropertyTest : FunSpec({
    
    test("Property 5: Photo-Stage Association Integrity - Feature: recipe-manager, Property 5: For any photo and recipe stage, associating the photo with the stage should make it retrievable when viewing that specific stage") {
        checkAll(100, photoArb(), Arb.string(1..50), Arb.string(1..50)) { photo, ingredientId, stepId ->
            // Create a fresh database for each test iteration
            val driverFactory = DatabaseDriverFactory()
            val database = RecipeDatabase(driverFactory.createDriver())
            val photoStorage = MockPhotoStorage()
            val photoRepository = PhotoRepositoryImpl(database, photoStorage)
            val photoAssociationService = PhotoAssociationService(photoRepository)
            
            // Save the photo first
            val saveResult = photoRepository.savePhoto(photo)
            saveResult.isSuccess shouldBe true
            
            val savedPhoto = saveResult.getOrNull()
            savedPhoto.shouldNotBeNull()
            
            // Test ingredient association for appropriate stages
            if (photo.stage == PhotoStage.RAW_INGREDIENTS || photo.stage == PhotoStage.PROCESSED_INGREDIENTS) {
                // Associate photo with ingredient
                val associateResult = photoAssociationService.tagPhotoToIngredient(savedPhoto.id, ingredientId)
                associateResult.isSuccess shouldBe true
                
                // Retrieve photos for the ingredient
                val retrieveResult = photoAssociationService.getPhotosForIngredient(ingredientId)
                retrieveResult.isSuccess shouldBe true
                
                val retrievedPhotos = retrieveResult.getOrNull()
                retrievedPhotos.shouldNotBeNull()
                retrievedPhotos.map { it.id } shouldContain savedPhoto.id
                
                // Verify the retrieved photo has the correct stage
                val retrievedPhoto = retrievedPhotos.find { it.id == savedPhoto.id }
                retrievedPhoto.shouldNotBeNull()
                retrievedPhoto.stage shouldBe photo.stage
            }
            
            // Test cooking step association for appropriate stages
            if (photo.stage == PhotoStage.COOKING_STEP || photo.stage == PhotoStage.FINAL_RESULT) {
                // Associate photo with cooking step
                val associateResult = photoAssociationService.tagPhotoToCookingStep(savedPhoto.id, stepId)
                associateResult.isSuccess shouldBe true
                
                // Retrieve photos for the cooking step
                val retrieveResult = photoAssociationService.getPhotosForCookingStep(stepId)
                retrieveResult.isSuccess shouldBe true
                
                val retrievedPhotos = retrieveResult.getOrNull()
                retrievedPhotos.shouldNotBeNull()
                retrievedPhotos.map { it.id } shouldContain savedPhoto.id
                
                // Verify the retrieved photo has the correct stage
                val retrievedPhoto = retrievedPhotos.find { it.id == savedPhoto.id }
                retrievedPhoto.shouldNotBeNull()
                retrievedPhoto.stage shouldBe photo.stage
            }
            
            // Test retrieval by stage
            val recipeId = "test-recipe-${Clock.System.now().toEpochMilliseconds()}"
            val byStageResult = photoAssociationService.getPhotosByStage(recipeId, photo.stage)
            byStageResult.isSuccess shouldBe true
            
            val photosByStage = byStageResult.getOrNull()
            photosByStage.shouldNotBeNull()
            photosByStage.map { it.id } shouldContain savedPhoto.id
        }
    }
    
    test("Property 6: Photo Processing Consistency - Feature: recipe-manager, Property 6: For any captured photo, the system should automatically optimize it while preserving visual content and associating it with the correct stage") {
        checkAll(100, photoArb()) { photo ->
            // Create a fresh database for each test iteration
            val driverFactory = DatabaseDriverFactory()
            val database = RecipeDatabase(driverFactory.createDriver())
            val photoStorage = MockPhotoStorage()
            val photoRepository = PhotoRepositoryImpl(database, photoStorage)
            
            // Save the photo (this triggers optimization)
            val saveResult = photoRepository.savePhoto(photo)
            saveResult.isSuccess shouldBe true
            
            val savedPhoto = saveResult.getOrNull()
            savedPhoto.shouldNotBeNull()
            
            // Verify photo was optimized (path should be different from original)
            savedPhoto.localPath shouldNotBe photo.localPath
            savedPhoto.localPath shouldBe "optimized_${photo.localPath}"
            
            // Verify the photo storage was called to optimize
            photoStorage.savedPhotos shouldContain photo.localPath
            
            // Verify all other photo properties are preserved
            savedPhoto.id shouldBe photo.id
            savedPhoto.caption shouldBe photo.caption
            savedPhoto.stage shouldBe photo.stage
            savedPhoto.cloudUrl shouldBe photo.cloudUrl
            savedPhoto.syncStatus shouldBe photo.syncStatus
            
            // Verify photo can be retrieved with correct stage
            val retrieveResult = photoRepository.getPhoto(savedPhoto.id)
            retrieveResult.isSuccess shouldBe true
            
            val retrievedPhoto = retrieveResult.getOrNull()
            retrievedPhoto.shouldNotBeNull()
            retrievedPhoto.stage shouldBe photo.stage
            retrievedPhoto.localPath shouldBe savedPhoto.localPath
            
            // Verify photo appears in stage-specific queries
            val recipeId = "test-recipe-${Clock.System.now().toEpochMilliseconds()}"
            val byStageResult = photoRepository.getPhotosByStage(recipeId, photo.stage)
            byStageResult.isSuccess shouldBe true
            
            val photosByStage = byStageResult.getOrNull()
            photosByStage.shouldNotBeNull()
            photosByStage.map { it.id } shouldContain savedPhoto.id
            
            // Verify the photo in stage query has correct stage
            val photoInStage = photosByStage.find { it.id == savedPhoto.id }
            photoInStage.shouldNotBeNull()
            photoInStage.stage shouldBe photo.stage
        }
    }
})

/**
 * Mock implementation of PhotoStorage for testing.
 * Simulates photo optimization by prefixing paths.
 */
class MockPhotoStorage : PhotoStorage {
    val savedPhotos = mutableListOf<String>()
    val deletedPhotos = mutableListOf<String>()
    
    override suspend fun savePhoto(sourcePath: String): String {
        savedPhotos.add(sourcePath)
        // Simulate optimization by returning a modified path
        return "optimized_$sourcePath"
    }
    
    override suspend fun deletePhoto(photoPath: String) {
        deletedPhotos.add(photoPath)
    }
    
    override suspend fun photoExists(photoPath: String): Boolean {
        return savedPhotos.any { "optimized_$it" == photoPath }
    }
    
    override suspend fun getPhotoSize(photoPath: String): Long? {
        return if (photoExists(photoPath)) 1024L else null
    }
}
