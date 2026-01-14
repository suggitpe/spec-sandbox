package com.recipemanager.di

import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.database.DatabaseManager
import com.recipemanager.data.repository.RecipeRepositoryImpl
import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.domain.usecase.RecipeUseCases
import com.recipemanager.domain.validation.RecipeValidator

class AppModule(private val databaseDriverFactory: DatabaseDriverFactory) {
    
    private val databaseManager: DatabaseManager by lazy {
        DatabaseManager(databaseDriverFactory)
    }
    
    private val database: RecipeDatabase by lazy {
        databaseManager.initialize()
    }
    
    private val recipeValidator: RecipeValidator by lazy {
        RecipeValidator()
    }
    
    val recipeRepository: RecipeRepository by lazy {
        RecipeRepositoryImpl(database)
    }
    
    val recipeUseCases: RecipeUseCases by lazy {
        RecipeUseCases(recipeRepository, recipeValidator)
    }
}
