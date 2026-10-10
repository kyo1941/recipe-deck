package com.example.recipe_deck.domain.shopping

sealed interface ShoppingSessionRepositoryFailure {
    data class Unexpected(val cause: Throwable) : ShoppingSessionRepositoryFailure
}
