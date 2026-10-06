package com.example.recipe_deck.domain.recipe

sealed interface RecipeRepositoryFailure {
    data class Unexpected(val cause: Throwable) : RecipeRepositoryFailure
}
