package com.example.recipe_deck.domain.item

import com.example.recipe_deck.domain.Outcome
import kotlinx.collections.immutable.ImmutableList

interface ItemRepository {
    suspend fun findAll(): Outcome<ImmutableList<Item>, ItemRepositoryFailure>
}
