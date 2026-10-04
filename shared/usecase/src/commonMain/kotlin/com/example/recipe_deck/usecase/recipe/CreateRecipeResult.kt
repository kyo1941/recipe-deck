package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.recipe.RecipeId

sealed interface CreateRecipeResult {
    data class Success(val recipeId: RecipeId) : CreateRecipeResult

    sealed interface Failure : CreateRecipeResult {
        data object BlankName : Failure
        data class Unexpected(val cause: Throwable) : Failure
    }
}
