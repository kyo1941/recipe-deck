@file:OptIn(ExperimentalUuidApi::class)

package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.item.ItemReference
import com.example.recipe_deck.domain.recipe.RecipeIngredient
import com.example.recipe_deck.domain.recipe.RecipeIngredientId
import com.example.recipe_deck.usecase.ingredient.IngredientInput
import com.example.recipe_deck.usecase.item.linkToExistingItem
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
    val existingItemsById = existingItems.values.associateBy { it.id }
    val newItems = mutableMapOf<String, Item>()
    val matchedExistingItems = mutableSetOf<Item>()
    val recipeIngredients = ingredients
        .filterNot { it.isIgnorable }
        .map { input ->
            val itemId = when (val item = input.item) {
                is ItemReference.Registered -> item.itemId
                is ItemReference.Unregistered -> when (val linked = item.linkToExistingItem(existingItems)) {
                    is ItemReference.Registered -> {
                        existingItemsById[linked.itemId]?.let { matchedExistingItems += it }
                        linked.itemId
                    }
                    is ItemReference.Unregistered -> newItems
                        .getOrPut(linked.displayName) { Item(ItemId(Uuid.random().toString()), linked.displayName) }
                        .id
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
