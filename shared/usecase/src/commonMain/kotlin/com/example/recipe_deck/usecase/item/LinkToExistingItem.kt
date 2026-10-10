package com.example.recipe_deck.usecase.item

import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemReference

internal fun ItemReference.Unregistered.linkToExistingItem(existingItems: Map<String, Item>): ItemReference {
    val trimmedDisplayName = displayName.trim()
    val existingItem = existingItems[trimmedDisplayName]
    return if (existingItem != null) {
        ItemReference.Registered(existingItem.id)
    } else {
        ItemReference.Unregistered(trimmedDisplayName)
    }
}
