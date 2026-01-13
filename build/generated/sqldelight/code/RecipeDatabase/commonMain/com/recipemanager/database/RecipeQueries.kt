package com.recipemanager.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class RecipeQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAllRecipes(mapper: (
    id: String,
    title: String,
    description: String?,
    preparationTime: Long,
    cookingTime: Long,
    servings: Long,
    tags: String,
    createdAt: Long,
    updatedAt: Long,
    version: Long,
    parentRecipeId: String?,
  ) -> T): Query<T> = Query(80_223_583, arrayOf("Recipe"), driver, "Recipe.sq", "selectAllRecipes",
      "SELECT * FROM Recipe ORDER BY updatedAt DESC") { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getString(10)
    )
  }

  public fun selectAllRecipes(): Query<Recipe> = selectAllRecipes { id, title, description,
      preparationTime, cookingTime, servings, tags, createdAt, updatedAt, version, parentRecipeId ->
    Recipe(
      id,
      title,
      description,
      preparationTime,
      cookingTime,
      servings,
      tags,
      createdAt,
      updatedAt,
      version,
      parentRecipeId
    )
  }

  public fun <T : Any> selectRecipeById(id: String, mapper: (
    id: String,
    title: String,
    description: String?,
    preparationTime: Long,
    cookingTime: Long,
    servings: Long,
    tags: String,
    createdAt: Long,
    updatedAt: Long,
    version: Long,
    parentRecipeId: String?,
  ) -> T): Query<T> = SelectRecipeByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getString(10)
    )
  }

  public fun selectRecipeById(id: String): Query<Recipe> = selectRecipeById(id) { id_, title,
      description, preparationTime, cookingTime, servings, tags, createdAt, updatedAt, version,
      parentRecipeId ->
    Recipe(
      id_,
      title,
      description,
      preparationTime,
      cookingTime,
      servings,
      tags,
      createdAt,
      updatedAt,
      version,
      parentRecipeId
    )
  }

  public fun <T : Any> searchRecipes(
    `value`: String,
    value_: String,
    value__: String,
    mapper: (
      id: String,
      title: String,
      description: String?,
      preparationTime: Long,
      cookingTime: Long,
      servings: Long,
      tags: String,
      createdAt: Long,
      updatedAt: Long,
      version: Long,
      parentRecipeId: String?,
    ) -> T,
  ): Query<T> = SearchRecipesQuery(value, value_, value__) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getString(10)
    )
  }

  public fun searchRecipes(
    value_: String,
    value__: String,
    value___: String,
  ): Query<Recipe> = searchRecipes(value_, value__, value___) { id, title, description,
      preparationTime, cookingTime, servings, tags, createdAt, updatedAt, version, parentRecipeId ->
    Recipe(
      id,
      title,
      description,
      preparationTime,
      cookingTime,
      servings,
      tags,
      createdAt,
      updatedAt,
      version,
      parentRecipeId
    )
  }

  public fun insertRecipe(
    id: String,
    title: String,
    description: String?,
    preparationTime: Long,
    cookingTime: Long,
    servings: Long,
    tags: String,
    createdAt: Long,
    updatedAt: Long,
    version: Long,
    parentRecipeId: String?,
  ) {
    driver.execute(-900_697_946, """
        |INSERT INTO Recipe (id, title, description, preparationTime, cookingTime, servings, tags, createdAt, updatedAt, version, parentRecipeId)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 11) {
          bindString(0, id)
          bindString(1, title)
          bindString(2, description)
          bindLong(3, preparationTime)
          bindLong(4, cookingTime)
          bindLong(5, servings)
          bindString(6, tags)
          bindLong(7, createdAt)
          bindLong(8, updatedAt)
          bindLong(9, version)
          bindString(10, parentRecipeId)
        }
    notifyQueries(-900_697_946) { emit ->
      emit("Recipe")
    }
  }

  public fun updateRecipe(
    title: String,
    description: String?,
    preparationTime: Long,
    cookingTime: Long,
    servings: Long,
    tags: String,
    updatedAt: Long,
    version: Long,
    parentRecipeId: String?,
    id: String,
  ) {
    driver.execute(1_699_945_142, """
        |UPDATE Recipe SET 
        |    title = ?, 
        |    description = ?, 
        |    preparationTime = ?, 
        |    cookingTime = ?, 
        |    servings = ?, 
        |    tags = ?, 
        |    updatedAt = ?, 
        |    version = ?, 
        |    parentRecipeId = ?
        |WHERE id = ?
        """.trimMargin(), 10) {
          bindString(0, title)
          bindString(1, description)
          bindLong(2, preparationTime)
          bindLong(3, cookingTime)
          bindLong(4, servings)
          bindString(5, tags)
          bindLong(6, updatedAt)
          bindLong(7, version)
          bindString(8, parentRecipeId)
          bindString(9, id)
        }
    notifyQueries(1_699_945_142) { emit ->
      emit("Recipe")
    }
  }

  public fun deleteRecipe(id: String) {
    driver.execute(1_880_143_384, """DELETE FROM Recipe WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(1_880_143_384) { emit ->
      emit("Recipe")
    }
  }

  private inner class SelectRecipeByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Recipe", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Recipe", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_549_678_619, """SELECT * FROM Recipe WHERE id = ?""", mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "Recipe.sq:selectRecipeById"
  }

  private inner class SearchRecipesQuery<out T : Any>(
    public val `value`: String,
    public val value_: String,
    public val value__: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Recipe", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Recipe", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-323_836_226, """
    |SELECT * FROM Recipe 
    |WHERE title LIKE '%' || ? || '%' 
    |   OR description LIKE '%' || ? || '%' 
    |   OR tags LIKE '%' || ? || '%'
    |ORDER BY updatedAt DESC
    """.trimMargin(), mapper, 3) {
      bindString(0, value)
      bindString(1, value_)
      bindString(2, value__)
    }

    override fun toString(): String = "Recipe.sq:searchRecipes"
  }
}
