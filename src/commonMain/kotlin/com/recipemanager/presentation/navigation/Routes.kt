package com.recipemanager.presentation.navigation

/**
 * Navigation routes for the Recipe Manager application.
 * Supports deep linking and state preservation.
 */
object Routes {
    const val RECIPE_LIST = "recipe_list"
    const val RECIPE_DETAIL = "recipe_detail/{recipeId}"
    const val RECIPE_FORM = "recipe_form?recipeId={recipeId}"
    const val COLLECTION_LIST = "collection_list"
    const val COLLECTION_DETAIL = "collection_detail/{collectionId}"
    const val COOKING_MODE = "cooking_mode/{recipeId}"
    const val PHOTO_MANAGEMENT = "photo_management/{recipeId}"
    const val IMPORT_RECIPE = "import_recipe"
    const val SHARE_RECIPE = "share_recipe/{recipeId}"
    
    // Deep link patterns for shared recipes
    const val SHARED_RECIPE_DEEP_LINK = "recipemanager://recipe/{recipeId}"
    const val SHARED_COLLECTION_DEEP_LINK = "recipemanager://collection/{collectionId}"
    
    /**
     * Helper functions to create routes with parameters
     */
    fun recipeDetail(recipeId: String) = "recipe_detail/$recipeId"
    fun recipeForm(recipeId: String? = null) = if (recipeId != null) {
        "recipe_form?recipeId=$recipeId"
    } else {
        "recipe_form"
    }
    fun collectionDetail(collectionId: String) = "collection_detail/$collectionId"
    fun cookingMode(recipeId: String) = "cooking_mode/$recipeId"
    fun photoManagement(recipeId: String) = "photo_management/$recipeId"
    fun shareRecipe(recipeId: String) = "share_recipe/$recipeId"
}