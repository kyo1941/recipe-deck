package com.example.recipe_deck.domain.item

sealed interface ItemReference {
    data class Registered(val itemId: ItemId) : ItemReference
    data class Unregistered(val displayName: String) : ItemReference
}
