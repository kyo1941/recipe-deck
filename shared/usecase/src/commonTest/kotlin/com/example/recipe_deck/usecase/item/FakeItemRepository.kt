package com.example.recipe_deck.usecase.item

import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemRepository
import com.example.recipe_deck.domain.item.ItemRepositoryFailure
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class FakeItemRepository : ItemRepository {
    val savedItems = mutableListOf<Item>()
    var findFailure: ItemRepositoryFailure? = null

    override suspend fun findAll(): Outcome<ImmutableList<Item>, ItemRepositoryFailure> {
        findFailure?.let { return Outcome.Failure(it) }
        return Outcome.Success(savedItems.toImmutableList())
    }
}
