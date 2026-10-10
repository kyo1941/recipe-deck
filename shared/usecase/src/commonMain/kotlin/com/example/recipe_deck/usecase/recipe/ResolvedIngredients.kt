@file:OptIn(ExperimentalUuidApi::class)

package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.item.ItemReference
import com.example.recipe_deck.domain.recipe.RecipeIngredient
import com.example.recipe_deck.domain.recipe.RecipeIngredientId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal data class ResolvedIngredients(
    val recipeIngredients: ImmutableList<RecipeIngredient>,
    val newItems: ImmutableList<Item>,
    val matchedExistingItems: ImmutableList<Item>,
)

internal fun resolveIngredients(
    ingredients: List<IngredientInput>,
    existingItems: Map<String, Item>,
): ResolvedIngredients {
    val newItems = mutableMapOf<String, Item>()
    val matchedExistingItems = mutableSetOf<Item>()
    val recipeIngredients = ingredients
        .filterNot { it.isIgnorable }
        .map { input ->
            val itemId = when (val item = input.item) {
                is ItemReference.Registered -> item.itemId
                is ItemReference.Unregistered -> {
                    val displayName = item.displayName.trim()
                    val existingItem = existingItems[displayName]
                    if (existingItem != null) {
                        matchedExistingItems += existingItem
                        existingItem.id
                    } else {
                        newItems
                            .getOrPut(displayName) { Item(ItemId(Uuid.random().toString()), displayName) }
                            .id
                    }
                }
            }
            RecipeIngredient(
                id = RecipeIngredientId(Uuid.random().toString()),
                itemId = itemId,
                amount = input.amount,
            )
        }
    return ResolvedIngredients(
        recipeIngredients = recipeIngredients.toImmutableList(),
        newItems = newItems.values.toImmutableList(),
        matchedExistingItems = matchedExistingItems.toImmutableList(),
    )
}
