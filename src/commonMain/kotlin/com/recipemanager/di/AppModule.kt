package com.recipemanager.di

import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.usecase.RecipeUseCases
import com.recipemanager.domain.validation.RecipeValidator

class AppModule(private val databaseDriverFactory: DatabaseDriverFactory) {
    
    private val database: RecipeDatabase by lazy {
        RecipeDatabase(databaseDriverFactory.createDriver())
    }
    
    private val recipeValidator: RecipeValidator by lazy {
        RecipeValidator()
    }
    
    // Repository implementations will be added in later tasks
    // private val recipeRepository: RecipeRepository by lazy {
    //     RecipeRepositoryImpl(database)
    // }
    
    // val recipeUseCases: RecipeUseCases by lazy {
    //     RecipeUseCases(recipeRepository, recipeValidator)
    // }
}