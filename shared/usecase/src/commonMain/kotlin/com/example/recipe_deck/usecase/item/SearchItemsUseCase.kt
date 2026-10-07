package com.example.recipe_deck.usecase.item

import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemRepository
import com.example.recipe_deck.domain.item.ItemRepositoryFailure
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class SearchItemsUseCase(
    private val itemRepository: ItemRepository,
) {
    suspend operator fun invoke(keyword: String): SearchItemsResult {
        val trimmedKeyword = keyword.trim()
        if (trimmedKeyword.isEmpty()) return SearchItemsResult.Found(persistentListOf())

        return when (val found = itemRepository.findAll()) {
            is Outcome.Success -> SearchItemsResult.Found(
                found.value.filter { it.displayName.contains(trimmedKeyword) }.toImmutableList(),
            )
            is Outcome.Failure -> found.error.toSearchItemsResult()
        }
    }

    private fun ItemRepositoryFailure.toSearchItemsResult(): SearchItemsResult = when (this) {
        is ItemRepositoryFailure.Unexpected -> SearchItemsResult.Unexpected(cause)
    }
}

sealed interface SearchItemsResult {
    data class Found(val items: ImmutableList<Item>) : SearchItemsResult
    data class Unexpected(val cause: Throwable) : SearchItemsResult
}
