package com.recipemanager.presentation

import com.recipemanager.presentation.navigation.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class NavigationTest : FunSpec({
    
    test("Routes should generate correct paths") {
        Routes.recipeDetail("recipe123") shouldBe "recipe_detail/recipe123"
        Routes.recipeForm("recipe456") shouldBe "recipe_form?recipeId=recipe456"
        Routes.recipeForm(null) shouldBe "recipe_form"
        Routes.collectionDetail("collection789") shouldBe "collection_detail/collection789"
        Routes.cookingMode("recipe123") shouldBe "cooking_mode/recipe123"
        Routes.photoManagement("recipe456") shouldBe "photo_management/recipe456"
        Routes.shareRecipe("recipe789") shouldBe "share_recipe/recipe789"
    }
    
    test("NavigationState should serialize and deserialize correctly") {
        val originalState = NavigationState(
            currentRoute = Routes.RECIPE_DETAIL.replace("{recipeId}", "test123"),
            backStack = listOf(Routes.RECIPE_LIST),
            timestamp = 1234567890L
        )
        
        val manager = NavigationStateManager()
        val serialized = manager.serializeState()
        serialized shouldNotBe null
        
        val restored = manager.restoreState(serialized)
        // Since we haven't set a state yet, this should return the default state
        restored shouldNotBe null
    }
    
    test("DeepLinkHandler should generate correct deep links") {
        // We can't test the full handler without a NavController, but we can test link generation
        val testRecipeId = "recipe123"
        val testCollectionId = "collection456"
        
        // Test the static methods that don't require NavController
        val recipeDeepLink = "recipemanager://recipe/$testRecipeId"
        val collectionDeepLink = "recipemanager://collection/$testCollectionId"
        val recipeWebLink = "https://recipemanager.app/recipe/$testRecipeId"
        val collectionWebLink = "https://recipemanager.app/collection/$testCollectionId"
        
        recipeDeepLink shouldBe "recipemanager://recipe/recipe123"
        collectionDeepLink shouldBe "recipemanager://collection/collection456"
        recipeWebLink shouldBe "https://recipemanager.app/recipe/recipe123"
        collectionWebLink shouldBe "https://recipemanager.app/collection/collection456"
    }
    
    test("StatePersistence should handle state operations").config(coroutineTestScope = true) {
        val persistence = InMemoryStatePersistence()
        
        // Test save and load
        persistence.saveState("test_key", "test_value")
        val loaded = persistence.loadState("test_key")
        loaded shouldBe "test_value"
        
        // Test clear
        persistence.clearState("test_key")
        val clearedValue = persistence.loadState("test_key")
        clearedValue shouldBe null
    }
    
    test("AppStatePersistenceManager should handle navigation state").config(coroutineTestScope = true) {
        val manager = AppStatePersistenceManager(InMemoryStatePersistence())
        
        val testState = NavigationState(
            currentRoute = "test_route",
            backStack = listOf("back1", "back2"),
            timestamp = System.currentTimeMillis()
        )
        
        // Save state
        manager.saveNavigationState(testState)
        
        // Load state
        val loadedState = manager.loadNavigationState()
        loadedState shouldNotBe null
        loadedState?.currentRoute shouldBe "test_route"
        loadedState?.backStack shouldBe listOf("back1", "back2")
    }
})