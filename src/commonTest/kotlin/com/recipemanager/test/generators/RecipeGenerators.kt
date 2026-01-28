package com.recipemanager.test.generators

import com.recipemanager.domain.model.*
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

fun recipeArb(): Arb<Recipe> = arbitrary { rs ->
    val now = Clock.System.now()
    Recipe(
        id = Arb.string(1..50).bind(),
        title = Arb.string(1..100).filter { it.isNotBlank() }.bind(),
        description = Arb.string(0..500).orNull(0.3).bind(),
        ingredients = Arb.list(ingredientArb(), 1..10).bind(),
        steps = Arb.list(cookingStepArb(), 1..15).bind(),
        preparationTime = Arb.int(1..480).bind(),
        cookingTime = Arb.int(1..480).bind(),
        servings = Arb.int(1..20).bind(),
        tags = Arb.list(Arb.string(1..30).filter { it.isNotBlank() }, 0..5).bind(),
        createdAt = now,
        updatedAt = now,
        version = Arb.int(1..10).bind(),
        parentRecipeId = Arb.string(1..50).orNull(0.8).bind()
    )
}

fun ingredientArb(): Arb<Ingredient> = arbitrary { rs ->
    Ingredient(
        id = Arb.string(1..50).bind(),
        name = Arb.string(1..50).filter { it.isNotBlank() }.bind(),
        quantity = Arb.double(0.1..1000.0).bind(),
        unit = Arb.element("cup", "tbsp", "tsp", "oz", "lb", "g", "kg", "ml", "l").bind(),
        notes = Arb.string(0..200).orNull(0.5).bind(),
        photos = Arb.list(photoArb(), 0..3).bind()
    )
}

fun cookingStepArb(): Arb<CookingStep> = arbitrary { rs ->
    CookingStep(
        id = Arb.string(1..50).bind(),
        stepNumber = Arb.int(1..20).bind(),
        instruction = Arb.string(10..200).filter { it.isNotBlank() }.bind(),
        duration = Arb.int(1..120).orNull(0.6).bind(),
        temperature = Arb.int(100..500).orNull(0.4).bind(),
        photos = Arb.list(photoArb(), 0..2).bind(),
        timerRequired = Arb.boolean().bind()
    )
}

fun photoArb(): Arb<Photo> = arbitrary { rs ->
    Photo(
        id = Arb.string(1..50).bind(),
        localPath = Arb.string(10..100).map { "/path/to/photo/$it.jpg" }.bind(),
        cloudUrl = Arb.string(20..100).map { "https://example.com/photos/$it.jpg" }.orNull(0.7).bind(),
        caption = Arb.string(0..100).orNull(0.6).bind(),
        stage = Arb.enum<PhotoStage>().bind(),
        timestamp = Clock.System.now(),
        syncStatus = Arb.enum<SyncStatus>().bind()
    )
}

fun cookingTimerArb(): Arb<CookingTimer> = arbitrary { rs ->
    val duration = Arb.int(30..3600).bind() // 30 seconds to 1 hour
    CookingTimer(
        id = Arb.string(1..50).bind(),
        recipeId = Arb.string(1..50).bind(),
        stepId = Arb.string(1..50).bind(),
        duration = duration,
        remainingTime = Arb.int(0..duration).bind(),
        status = Arb.enum<TimerStatus>().bind(),
        createdAt = Clock.System.now()
    )
}

fun recipeCollectionArb(): Arb<RecipeCollection> = arbitrary { rs ->
    val now = Clock.System.now()
    RecipeCollection(
        id = Arb.string(1..50).bind(),
        name = Arb.string(1..100).filter { it.isNotBlank() }.bind(),
        description = Arb.string(0..300).orNull(0.4).bind(),
        recipeIds = Arb.list(Arb.string(1..50), 0..20).bind(),
        createdAt = now,
        updatedAt = now
    )
}

// Navigation and State generators for property testing

fun navigationStateArb(): Arb<com.recipemanager.presentation.navigation.NavigationState> = arbitrary { rs ->
    com.recipemanager.presentation.navigation.NavigationState(
        currentRoute = Arb.element(
            "recipe_list",
            "recipe_detail/recipe123",
            "recipe_form",
            "recipe_form?recipeId=recipe456",
            "collection_list",
            "collection_detail/collection789",
            "cooking_mode/recipe123",
            "photo_management/recipe456",
            "share_recipe/recipe789",
            "import_recipe"
        ).bind(),
        backStack = Arb.list(
            Arb.element(
                "recipe_list",
                "recipe_detail/recipe123",
                "collection_list",
                "cooking_mode/recipe456"
            ),
            0..5
        ).bind(),
        timestamp = Arb.long(1000000000L..2000000000L).bind()
    )
}

fun appStateMapArb(): Arb<Map<String, String>> = arbitrary { rs ->
    val keys = listOf("lastViewedRecipe", "currentCollection", "cookingMode", "searchQuery", "filterTags")
    val selectedKeys = Arb.list(Arb.element(keys), 0..keys.size).bind().distinct()
    
    selectedKeys.associateWith { key ->
        when (key) {
            "lastViewedRecipe" -> Arb.string(1..50).bind()
            "currentCollection" -> Arb.string(1..50).bind()
            "cookingMode" -> Arb.element("true", "false").bind()
            "searchQuery" -> Arb.string(0..100).bind()
            "filterTags" -> Arb.list(Arb.string(1..20), 0..5).bind().joinToString(",")
            else -> Arb.string(0..100).bind()
        }
    }
}