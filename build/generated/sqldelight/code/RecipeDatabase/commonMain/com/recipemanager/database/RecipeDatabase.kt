package com.recipemanager.database

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.recipemanager.database.RecipeManager.newInstance
import com.recipemanager.database.RecipeManager.schema
import kotlin.Unit

public interface RecipeDatabase : Transacter {
  public val recipeQueries: RecipeQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = RecipeDatabase::class.schema

    public operator fun invoke(driver: SqlDriver): RecipeDatabase =
        RecipeDatabase::class.newInstance(driver)
  }
}
