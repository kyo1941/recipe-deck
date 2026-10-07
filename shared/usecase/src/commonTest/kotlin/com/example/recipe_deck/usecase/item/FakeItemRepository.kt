package com.example.recipe_deck.usecase.item

import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemRepository
import com.example.recipe_deck.domain.item.ItemRepositoryFailure
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class FakeItemRepository : ItemRepository {
    val savedItems = mutableListOf<Item>()
    var saveFailure: ItemRepositoryFailure? = null
    var findFailure: ItemRepositoryFailure? = null

    override suspend fun save(item: Item): Outcome<Unit, ItemRepositoryFailure> {
        saveFailure?.let { return Outcome.Failure(it) }
        savedItems += item
        return Outcome.Success(Unit)
    }

    override suspend fun findAll(): Outcome<ImmutableList<Item>, ItemRepositoryFailure> {
        findFailure?.let { return Outcome.Failure(it) }
        return Outcome.Success(savedItems.toImmutableList())
    }
}
