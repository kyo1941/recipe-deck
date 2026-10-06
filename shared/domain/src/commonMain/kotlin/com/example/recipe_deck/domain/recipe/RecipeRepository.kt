package com.example.recipe_deck.domain.recipe

import com.example.recipe_deck.domain.Outcome

interface RecipeRepository {
    suspend fun save(recipe: Recipe): Outcome<Unit, RecipeRepositoryFailure>
}
