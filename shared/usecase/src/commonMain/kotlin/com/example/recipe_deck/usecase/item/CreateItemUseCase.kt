@file:OptIn(ExperimentalUuidApi::class)

package com.example.recipe_deck.usecase.item

import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.item.ItemRepository
import com.example.recipe_deck.domain.item.ItemRepositoryFailure
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CreateItemUseCase(
    private val itemRepository: ItemRepository,
) {
    suspend operator fun invoke(displayName: String): CreateItemResult {
        val trimmedDisplayName = displayName.trim()
        if (trimmedDisplayName.isEmpty()) return CreateItemResult.DisplayNameRequired

        val existingItems = when (val found = itemRepository.findAll()) {
            is Outcome.Success -> found.value
            is Outcome.Failure -> return found.error.toCreateItemResult()
        }
        existingItems.firstOrNull { it.displayName == trimmedDisplayName }?.let {
            return CreateItemResult.AlreadyExists(it)
        }

        val item = Item(
            id = ItemId(Uuid.random().toString()),
            displayName = trimmedDisplayName,
        )
        return when (val saved = itemRepository.save(item)) {
            is Outcome.Success -> CreateItemResult.Created(item.id)
            is Outcome.Failure -> saved.error.toCreateItemResult()
        }
    }

    private fun ItemRepositoryFailure.toCreateItemResult(): CreateItemResult = when (this) {
        is ItemRepositoryFailure.Unexpected -> CreateItemResult.Unexpected(cause)
    }
}

sealed interface CreateItemResult {
    data class Created(val itemId: ItemId) : CreateItemResult
    data object DisplayNameRequired : CreateItemResult
    data class AlreadyExists(val existingItem: Item) : CreateItemResult
    data class Unexpected(val cause: Throwable) : CreateItemResult
}
