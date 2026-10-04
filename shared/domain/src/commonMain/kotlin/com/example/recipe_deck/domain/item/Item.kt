package com.example.recipe_deck.domain.item

data class Item(
    val id: ItemId,
    val displayName: String,
) {
    init {
        require(displayName.isNotBlank()) { "Item の表示名は空白だけにできない" }
    }
}
