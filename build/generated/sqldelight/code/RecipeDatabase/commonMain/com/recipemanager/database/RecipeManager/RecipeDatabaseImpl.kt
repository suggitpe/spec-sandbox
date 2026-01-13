package com.recipemanager.database.RecipeManager

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.recipemanager.database.RecipeDatabase
import com.recipemanager.database.RecipeQueries
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<RecipeDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = RecipeDatabaseImpl.Schema

internal fun KClass<RecipeDatabase>.newInstance(driver: SqlDriver): RecipeDatabase =
    RecipeDatabaseImpl(driver)

private class RecipeDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver), RecipeDatabase {
  override val recipeQueries: RecipeQueries = RecipeQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE Recipe (
          |    id TEXT PRIMARY KEY NOT NULL,
          |    title TEXT NOT NULL,
          |    description TEXT,
          |    preparationTime INTEGER NOT NULL,
          |    cookingTime INTEGER NOT NULL,
          |    servings INTEGER NOT NULL,
          |    tags TEXT NOT NULL, -- JSON array
          |    createdAt INTEGER NOT NULL, -- Unix timestamp
          |    updatedAt INTEGER NOT NULL, -- Unix timestamp
          |    version INTEGER NOT NULL DEFAULT 1,
          |    parentRecipeId TEXT
          |)
          """.trimMargin(), 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
