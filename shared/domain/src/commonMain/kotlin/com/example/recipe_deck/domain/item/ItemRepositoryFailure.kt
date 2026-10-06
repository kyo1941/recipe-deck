package com.example.recipe_deck.domain.item

sealed interface ItemRepositoryFailure {
    data class Unexpected(val cause: Throwable) : ItemRepositoryFailure
}
