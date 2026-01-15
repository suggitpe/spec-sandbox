package com.recipemanager.domain

import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.database.DatabaseManager
import com.recipemanager.data.repository.RecipeRepositoryImpl
import com.recipemanager.data.repository.RecipeSnapshotRepositoryImpl
import com.recipemanager.data.repository.RecipeVersionRepositoryImpl
import com.recipemanager.domain.model.CookingStep
import com.recipemanager.domain.model.Ingredient
import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.service.RecipeVersionManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.datetime.Clock

/**
 * Tests for recipe version reversion functionality.
 * Validates Requirement 4.5: Complete data rollback for recipe versions.
 */
class RecipeVersionReversionTest : FunSpec({
    
    lateinit var versionManager: RecipeVersionManager
    lateinit var recipeRepository: RecipeRepositoryImpl
    
    beforeEach {
        val databaseDriverFactory = DatabaseDriverFactory()
        val databaseManager = DatabaseManager(databaseDriverFactory)
        val database = databaseManager.initialize()
        
        recipeRepository = RecipeRepositoryImpl(database)
        val versionRepository = RecipeVersionRepositoryImpl(database)
        val snapshotRepository = RecipeSnapshotRepositoryImpl(database)
        
        versionManager = RecipeVersionManager(
            recipeRepository,
            versionRepository,
            snapshotRepository
        )
    }
    
    test("should restore complete recipe data when reverting to previous version") {
        // Create initial recipe (version 1)
        val now = Clock.System.now()
        val originalRecipe = Recipe(
            id = "recipe-1",
            title = "Original Pasta",
            description = "Original description",
            ingredients = listOf(
                Ingredient(
                    id = "ing-1",
                    name = "Pasta",
                    quantity = 400.0,
                    unit = "g",
                    notes = "Original pasta"
                )
            ),
            steps = listOf(
                CookingStep(
                    id = "step-1",
                    stepNumber = 1,
                    instruction = "Boil water",
                    duration = 10
                )
            ),
            preparationTime = 10,
            cookingTime = 20,
            servings = 4,
            tags = listOf("pasta", "italian"),
            createdAt = now,
            updatedAt = now,
            version = 1
        )
        
        val createResult = recipeRepository.createRecipe(originalRecipe)
        createResult.isSuccess shouldBe true
        
        // Create initial version and snapshot
        val initialVersionResult = versionManager.createInitialVersion(originalRecipe)
        initialVersionResult.isSuccess shouldBe true
        
        // Create upgraded version (version 2) with modifications
        val upgradeResult = versionManager.upgradeRecipe(
            parentRecipe = originalRecipe,
            upgradeNotes = "Added more ingredients"
        ) { recipe ->
            recipe.copy(
                title = "Upgraded Pasta",
                description = "Upgraded description",
                ingredients = recipe.ingredients + Ingredient(
                    id = "ing-2",
                    name = "Tomato Sauce",
                    quantity = 200.0,
                    unit = "ml",
                    notes = "Fresh tomato sauce"
                ),
                steps = recipe.steps + CookingStep(
                    id = "step-2",
                    stepNumber = 2,
                    instruction = "Add sauce",
                    duration = 5
                ),
                servings = 6
            )
        }
        
        upgradeResult.isSuccess shouldBe true
        val upgradedRecipe = upgradeResult.getOrThrow()
        
        // Verify upgraded recipe has changes
        upgradedRecipe.title shouldBe "Upgraded Pasta"
        upgradedRecipe.description shouldBe "Upgraded description"
        upgradedRecipe.ingredients.size shouldBe 2
        upgradedRecipe.steps.size shouldBe 2
        upgradedRecipe.servings shouldBe 6
        upgradedRecipe.version shouldBe 2
        
        // Revert to version 1
        val revertResult = versionManager.revertToVersion(
            currentRecipeId = upgradedRecipe.id,
            targetVersion = 1
        )
        
        revertResult.isSuccess shouldBe true
        val revertedRecipe = revertResult.getOrThrow()
        
        // Verify reverted recipe has complete data from version 1
        revertedRecipe.id shouldNotBe originalRecipe.id // New ID
        revertedRecipe.id shouldNotBe upgradedRecipe.id // Different from upgraded
        revertedRecipe.title shouldBe "Original Pasta"
        revertedRecipe.description shouldBe "Original description"
        revertedRecipe.preparationTime shouldBe 10
        revertedRecipe.cookingTime shouldBe 20
        revertedRecipe.servings shouldBe 4
        revertedRecipe.tags shouldContainExactly listOf("pasta", "italian")
        
        // Verify ingredients are restored
        revertedRecipe.ingredients.size shouldBe 1
        revertedRecipe.ingredients[0].name shouldBe "Pasta"
        revertedRecipe.ingredients[0].quantity shouldBe 400.0
        revertedRecipe.ingredients[0].unit shouldBe "g"
        revertedRecipe.ingredients[0].notes shouldBe "Original pasta"
        
        // Verify steps are restored
        revertedRecipe.steps.size shouldBe 1
        revertedRecipe.steps[0].instruction shouldBe "Boil water"
        revertedRecipe.steps[0].duration shouldBe 10
        
        // Verify version chain is maintained
        revertedRecipe.parentRecipeId shouldBe upgradedRecipe.id
        revertedRecipe.version shouldBe 3 // New version in the chain
    }
    
    test("should fail when reverting to non-existent version") {
        val now = Clock.System.now()
        val recipe = Recipe(
            id = "recipe-2",
            title = "Test Recipe",
            description = null,
            ingredients = listOf(
                Ingredient("ing-1", "Flour", 200.0, "g", null)
            ),
            steps = listOf(
                CookingStep("step-1", 1, "Mix ingredients")
            ),
            preparationTime = 5,
            cookingTime = 10,
            servings = 2,
            tags = emptyList(),
            createdAt = now,
            updatedAt = now,
            version = 1
        )
        
        recipeRepository.createRecipe(recipe)
        
        // Try to revert to non-existent version
        val revertResult = versionManager.revertToVersion(
            currentRecipeId = recipe.id,
            targetVersion = 5
        )
        
        revertResult.isFailure shouldBe true
    }
    
    test("should fail when reverting to current version") {
        val now = Clock.System.now()
        val recipe = Recipe(
            id = "recipe-3",
            title = "Test Recipe",
            description = null,
            ingredients = listOf(
                Ingredient("ing-1", "Flour", 200.0, "g", null)
            ),
            steps = listOf(
                CookingStep("step-1", 1, "Mix ingredients")
            ),
            preparationTime = 5,
            cookingTime = 10,
            servings = 2,
            tags = emptyList(),
            createdAt = now,
            updatedAt = now,
            version = 1
        )
        
        recipeRepository.createRecipe(recipe)
        
        // Try to revert to current version
        val revertResult = versionManager.revertToVersion(
            currentRecipeId = recipe.id,
            targetVersion = 1
        )
        
        revertResult.isFailure shouldBe true
    }
    
    test("should maintain version history after reversion") {
        val now = Clock.System.now()
        val originalRecipe = Recipe(
            id = "recipe-4",
            title = "Version 1",
            description = null,
            ingredients = listOf(
                Ingredient("ing-1", "Ingredient 1", 100.0, "g", null)
            ),
            steps = listOf(
                CookingStep("step-1", 1, "Step 1")
            ),
            preparationTime = 5,
            cookingTime = 10,
            servings = 2,
            tags = emptyList(),
            createdAt = now,
            updatedAt = now,
            version = 1
        )
        
        recipeRepository.createRecipe(originalRecipe)
        versionManager.createInitialVersion(originalRecipe)
        
        // Create version 2
        val v2Result = versionManager.upgradeRecipe(
            parentRecipe = originalRecipe,
            upgradeNotes = "Upgrade to v2"
        ) { it.copy(title = "Version 2") }
        
        val v2Recipe = v2Result.getOrThrow()
        
        // Revert to version 1
        val revertResult = versionManager.revertToVersion(
            currentRecipeId = v2Recipe.id,
            targetVersion = 1
        )
        
        val revertedRecipe = revertResult.getOrThrow()
        
        // Check version history
        val historyResult = versionManager.getVersionHistory(revertedRecipe.id)
        historyResult.isSuccess shouldBe true
        
        val history = historyResult.getOrThrow()
        history.size shouldBe 1
        history[0].version shouldBe 3
        history[0].upgradeNotes shouldBe "Reverted to version 1"
    }
})
