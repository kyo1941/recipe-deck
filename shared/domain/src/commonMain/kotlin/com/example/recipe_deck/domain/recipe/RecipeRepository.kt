package com.example.recipe_deck.domain.recipe

interface RecipeRepository {
    suspend fun save(recipe: Recipe): Result<Unit>
}
