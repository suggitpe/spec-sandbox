package com.recipemanager.domain

import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.database.DatabaseManager
import com.recipemanager.data.repository.CollectionRepositoryImpl
import com.recipemanager.data.repository.RecipeRepositoryImpl
import com.recipemanager.domain.model.CookingStep
import com.recipemanager.domain.model.Ingredient
import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.model.RecipeCollection
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.Clock
import java.util.UUID

class CollectionManagementTest : FunSpec({
    
    lateinit var collectionRepository: CollectionRepositoryImpl
    lateinit var recipeRepository: RecipeRepositoryImpl
    
    beforeEach {
        val databaseManager = DatabaseManager(DatabaseDriverFactory())
        val database = databaseManager.initialize()
        collectionRepository = CollectionRepositoryImpl(database)
        recipeRepository = RecipeRepositoryImpl(database)
    }
    
    test("should create a collection") {
        val now = Clock.System.now()
        val collection = RecipeCollection(
            id = UUID.randomUUID().toString(),
            name = "Italian Recipes",
            description = "My favorite Italian dishes",
            recipeIds = emptyList(),
            createdAt = now,
            updatedAt = now
        )
        
        val result = collectionRepository.createCollection(collection)
        
        result.isSuccess shouldBe true
        val created = result.getOrThrow()
        created.name shouldBe "Italian Recipes"
        created.description shouldBe "My favorite Italian dishes"
    }
    
    test("should retrieve a collection by id") {
        val now = Clock.System.now()
        val collection = RecipeCollection(
            id = UUID.randomUUID().toString(),
            name = "Desserts",
            description = "Sweet treats",
            recipeIds = emptyList(),
            createdAt = now,
            updatedAt = now
        )
        
        collectionRepository.createCollection(collection)
        val result = collectionRepository.getCollection(collection.id)
        
        result.isSuccess shouldBe true
        val retrieved = result.getOrThrow()
        retrieved shouldNotBe null
        retrieved?.name shouldBe "Desserts"
        retrieved?.description shouldBe "Sweet treats"
    }
    
    test("should update a collection") {
        val now = Clock.System.now()
        val collection = RecipeCollection(
            id = UUID.randomUUID().toString(),
            name = "Breakfast",
            description = "Morning meals",
            recipeIds = emptyList(),
            createdAt = now,
            updatedAt = now
        )
        
        collectionRepository.createCollection(collection)
        
        val updated = collection.copy(
            name = "Breakfast & Brunch",
            description = "Morning and midday meals",
            updatedAt = Clock.System.now()
        )
        
        val result = collectionRepository.updateCollection(updated)
        
        result.isSuccess shouldBe true
        val retrieved = collectionRepository.getCollection(collection.id).getOrThrow()
        retrieved?.name shouldBe "Breakfast & Brunch"
        retrieved?.description shouldBe "Morning and midday meals"
    }
    
    test("should delete a collection") {
        val now = Clock.System.now()
        val collection = RecipeCollection(
            id = UUID.randomUUID().toString(),
            name = "Temporary",
            description = "To be deleted",
            recipeIds = emptyList(),
            createdAt = now,
            updatedAt = now
        )
        
        collectionRepository.createCollection(collection)
        val deleteResult = collectionRepository.deleteCollection(collection.id)
        
        deleteResult.isSuccess shouldBe true
        
        val retrieved = collectionRepository.getCollection(collection.id).getOrThrow()
        retrieved shouldBe null
    }
    
    test("should add recipe to collection") {
        val now = Clock.System.now()
        
        // Create a recipe
        val recipe = Recipe(
            id = UUID.randomUUID().toString(),
            title = "Pasta Carbonara",
            description = "Classic Italian pasta",
            ingredients = listOf(
                Ingredient(
                    id = UUID.randomUUID().toString(),
                    name = "Spaghetti",
                    quantity = 400.0,
                    unit = "g"
                )
            ),
            steps = listOf(
                CookingStep(
                    id = UUID.randomUUID().toString(),
                    stepNumber = 1,
                    instruction = "Boil pasta"
                )
            ),
            preparationTime = 10,
            cookingTime = 15,
            servings = 4,
            tags = listOf("pasta", "italian"),
            createdAt = now,
            updatedAt = now
        )
        
        recipeRepository.createRecipe(recipe)
        
        // Create a collection
        val collection = RecipeCollection(
            id = UUID.randomUUID().toString(),
            name = "Italian Recipes",
            description = "Italian dishes",
            recipeIds = emptyList(),
            createdAt = now,
            updatedAt = now
        )
        
        collectionRepository.createCollection(collection)
        
        // Add recipe to collection
        val addResult = collectionRepository.addRecipeToCollection(recipe.id, collection.id)
        
        addResult.isSuccess shouldBe true
        
        // Verify recipe is in collection
        val recipesInCollection = collectionRepository.getRecipesInCollection(collection.id).getOrThrow()
        recipesInCollection shouldHaveSize 1
        recipesInCollection[0].id shouldBe recipe.id
    }
    
    test("should remove recipe from collection") {
        val now = Clock.System.now()
        
        // Create a recipe
        val recipe = Recipe(
            id = UUID.randomUUID().toString(),
            title = "Tiramisu",
            description = "Italian dessert",
            ingredients = listOf(
                Ingredient(
                    id = UUID.randomUUID().toString(),
                    name = "Mascarpone",
                    quantity = 250.0,
                    unit = "g"
                )
            ),
            steps = listOf(
                CookingStep(
                    id = UUID.randomUUID().toString(),
                    stepNumber = 1,
                    instruction = "Mix ingredients"
                )
            ),
            preparationTime = 20,
            cookingTime = 0,
            servings = 6,
            tags = listOf("dessert", "italian"),
            createdAt = now,
            updatedAt = now
        )
        
        recipeRepository.createRecipe(recipe)
        
        // Create a collection with the recipe
        val collection = RecipeCollection(
            id = UUID.randomUUID().toString(),
            name = "Desserts",
            description = "Sweet treats",
            recipeIds = listOf(recipe.id),
            createdAt = now,
            updatedAt = now
        )
        
        collectionRepository.createCollection(collection)
        
        // Remove recipe from collection
        val removeResult = collectionRepository.removeRecipeFromCollection(recipe.id, collection.id)
        
        removeResult.isSuccess shouldBe true
        
        // Verify recipe is not in collection
        val recipesInCollection = collectionRepository.getRecipesInCollection(collection.id).getOrThrow()
        recipesInCollection shouldHaveSize 0
    }
    
    test("should get collections for a recipe") {
        val now = Clock.System.now()
        
        // Create a recipe
        val recipe = Recipe(
            id = UUID.randomUUID().toString(),
            title = "Pizza Margherita",
            description = "Classic pizza",
            ingredients = listOf(
                Ingredient(
                    id = UUID.randomUUID().toString(),
                    name = "Flour",
                    quantity = 500.0,
                    unit = "g"
                )
            ),
            steps = listOf(
                CookingStep(
                    id = UUID.randomUUID().toString(),
                    stepNumber = 1,
                    instruction = "Make dough"
                )
            ),
            preparationTime = 30,
            cookingTime = 15,
            servings = 4,
            tags = listOf("pizza", "italian"),
            createdAt = now,
            updatedAt = now
        )
        
        recipeRepository.createRecipe(recipe)
        
        // Create two collections
        val collection1 = RecipeCollection(
            id = UUID.randomUUID().toString(),
            name = "Italian Recipes",
            description = "Italian dishes",
            recipeIds = listOf(recipe.id),
            createdAt = now,
            updatedAt = now
        )
        
        val collection2 = RecipeCollection(
            id = UUID.randomUUID().toString(),
            name = "Quick Meals",
            description = "Fast to prepare",
            recipeIds = listOf(recipe.id),
            createdAt = now,
            updatedAt = now
        )
        
        collectionRepository.createCollection(collection1)
        collectionRepository.createCollection(collection2)
        
        // Get collections for recipe
        val collections = collectionRepository.getCollectionsForRecipe(recipe.id).getOrThrow()
        
        collections shouldHaveSize 2
        collections.map { it.name } shouldContain "Italian Recipes"
        collections.map { it.name } shouldContain "Quick Meals"
    }
    
    test("should allow recipe to belong to multiple collections") {
        val now = Clock.System.now()
        
        // Create a recipe
        val recipe = Recipe(
            id = UUID.randomUUID().toString(),
            title = "Chicken Salad",
            description = "Healthy salad",
            ingredients = listOf(
                Ingredient(
                    id = UUID.randomUUID().toString(),
                    name = "Chicken",
                    quantity = 200.0,
                    unit = "g"
                )
            ),
            steps = listOf(
                CookingStep(
                    id = UUID.randomUUID().toString(),
                    stepNumber = 1,
                    instruction = "Cook chicken"
                )
            ),
            preparationTime = 15,
            cookingTime = 20,
            servings = 2,
            tags = listOf("salad", "healthy"),
            createdAt = now,
            updatedAt = now
        )
        
        recipeRepository.createRecipe(recipe)
        
        // Create three collections
        val collection1 = RecipeCollection(
            id = UUID.randomUUID().toString(),
            name = "Healthy Meals",
            description = "Nutritious dishes",
            recipeIds = emptyList(),
            createdAt = now,
            updatedAt = now
        )
        
        val collection2 = RecipeCollection(
            id = UUID.randomUUID().toString(),
            name = "Lunch Ideas",
            description = "Midday meals",
            recipeIds = emptyList(),
            createdAt = now,
            updatedAt = now
        )
        
        val collection3 = RecipeCollection(
            id = UUID.randomUUID().toString(),
            name = "Chicken Recipes",
            description = "Dishes with chicken",
            recipeIds = emptyList(),
            createdAt = now,
            updatedAt = now
        )
        
        collectionRepository.createCollection(collection1)
        collectionRepository.createCollection(collection2)
        collectionRepository.createCollection(collection3)
        
        // Add recipe to all three collections
        collectionRepository.addRecipeToCollection(recipe.id, collection1.id)
        collectionRepository.addRecipeToCollection(recipe.id, collection2.id)
        collectionRepository.addRecipeToCollection(recipe.id, collection3.id)
        
        // Verify recipe is in all collections
        val collections = collectionRepository.getCollectionsForRecipe(recipe.id).getOrThrow()
        
        collections shouldHaveSize 3
        collections.map { it.name } shouldContain "Healthy Meals"
        collections.map { it.name } shouldContain "Lunch Ideas"
        collections.map { it.name } shouldContain "Chicken Recipes"
    }
    
    test("should keep recipe in main library when removed from collection") {
        val now = Clock.System.now()
        
        // Create a recipe
        val recipe = Recipe(
            id = UUID.randomUUID().toString(),
            title = "Beef Stew",
            description = "Hearty stew",
            ingredients = listOf(
                Ingredient(
                    id = UUID.randomUUID().toString(),
                    name = "Beef",
                    quantity = 500.0,
                    unit = "g"
                )
            ),
            steps = listOf(
                CookingStep(
                    id = UUID.randomUUID().toString(),
                    stepNumber = 1,
                    instruction = "Brown beef"
                )
            ),
            preparationTime = 20,
            cookingTime = 120,
            servings = 6,
            tags = listOf("stew", "beef"),
            createdAt = now,
            updatedAt = now
        )
        
        recipeRepository.createRecipe(recipe)
        
        // Create a collection with the recipe
        val collection = RecipeCollection(
            id = UUID.randomUUID().toString(),
            name = "Winter Meals",
            description = "Warm dishes",
            recipeIds = listOf(recipe.id),
            createdAt = now,
            updatedAt = now
        )
        
        collectionRepository.createCollection(collection)
        
        // Remove recipe from collection
        collectionRepository.removeRecipeFromCollection(recipe.id, collection.id)
        
        // Verify recipe still exists in main library
        val retrievedRecipe = recipeRepository.getRecipe(recipe.id).getOrThrow()
        retrievedRecipe shouldNotBe null
        retrievedRecipe?.title shouldBe "Beef Stew"
    }
})
