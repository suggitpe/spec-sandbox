package com.recipemanager.data.database

import app.cash.sqldelight.db.SqlDriver
import com.recipemanager.database.RecipeDatabase

/**
 * Manages database initialization, migrations, and lifecycle.
 */
class DatabaseManager(private val driverFactory: DatabaseDriverFactory) {
    
    private var driver: SqlDriver? = null
    private var database: RecipeDatabase? = null
    
    /**
     * Initialize the database with schema creation and migrations.
     */
    fun initialize(): RecipeDatabase {
        if (database != null) {
            return database!!
        }
        
        val sqlDriver = driverFactory.createDriver()
        driver = sqlDriver
        
        // Create schema if needed (handled by platform-specific drivers)
        val db = RecipeDatabase(sqlDriver)
        database = db
        
        return db
    }
    
    /**
     * Get the initialized database instance.
     * @throws IllegalStateException if database is not initialized
     */
    fun getDatabase(): RecipeDatabase {
        return database ?: throw IllegalStateException("Database not initialized. Call initialize() first.")
    }
    
    /**
     * Close the database connection and cleanup resources.
     */
    fun close() {
        driver?.close()
        driver = null
        database = null
    }
}
