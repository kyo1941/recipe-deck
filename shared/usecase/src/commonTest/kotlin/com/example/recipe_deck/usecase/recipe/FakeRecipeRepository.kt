package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.recipe.Recipe
import com.example.recipe_deck.domain.recipe.RecipeRepository

class FakeRecipeRepository : RecipeRepository {
    val savedRecipes = mutableListOf<Recipe>()
    var saveFailure: Throwable? = null

    override suspend fun save(recipe: Recipe): Result<Unit> {
        saveFailure?.let { return Result.failure(it) }
        savedRecipes += recipe
        return Result.success(Unit)
    }
}
