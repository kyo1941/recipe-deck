package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.recipe.Recipe
import com.example.recipe_deck.domain.recipe.RecipeId
import com.example.recipe_deck.domain.recipe.RecipeRepository
import com.example.recipe_deck.domain.recipe.RecipeRepositoryFailure

class FakeRecipeRepository : RecipeRepository {
    val savedRecipes = mutableListOf<Recipe>()
    var saveFailure: RecipeRepositoryFailure? = null
    var findFailure: RecipeRepositoryFailure? = null

    override suspend fun save(recipe: Recipe): Outcome<Unit, RecipeRepositoryFailure> {
        saveFailure?.let { return Outcome.Failure(it) }
        savedRecipes += recipe
        return Outcome.Success(Unit)
    }

    override suspend fun findById(id: RecipeId): Outcome<Recipe?, RecipeRepositoryFailure> {
        findFailure?.let { return Outcome.Failure(it) }
        return Outcome.Success(savedRecipes.lastOrNull { it.id == id })
    }
}
