package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.recipe.Recipe
import com.example.recipe_deck.domain.recipe.RecipeRepository
import com.example.recipe_deck.domain.recipe.RecipeRepositoryFailure

class FakeRecipeRepository : RecipeRepository {
    val savedRecipes = mutableListOf<Recipe>()
    var saveFailure: RecipeRepositoryFailure? = null

    override suspend fun save(recipe: Recipe): Outcome<Unit, RecipeRepositoryFailure> {
        saveFailure?.let { return Outcome.Failure(it) }
        savedRecipes += recipe
        return Outcome.Success(Unit)
    }
}
