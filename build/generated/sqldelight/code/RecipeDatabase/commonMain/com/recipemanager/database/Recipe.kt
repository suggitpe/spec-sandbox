package com.recipemanager.database

import kotlin.Long
import kotlin.String

public data class Recipe(
  public val id: String,
  public val title: String,
  public val description: String?,
  public val preparationTime: Long,
  public val cookingTime: Long,
  public val servings: Long,
  public val tags: String,
  public val createdAt: Long,
  public val updatedAt: Long,
  public val version: Long,
  public val parentRecipeId: String?,
)
