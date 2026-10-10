package com.example.recipe_deck.usecase.item

import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemReference

internal fun ItemReference.linkToExistingItem(existingItems: Map<String, Item>): ItemReference = when (this) {
    is ItemReference.Registered -> this
    is ItemReference.Unregistered -> {
        val trimmedDisplayName = displayName.trim()
        val existingItem = existingItems[trimmedDisplayName]
        if (existingItem != null) {
            ItemReference.Registered(existingItem.id)
        } else {
            ItemReference.Unregistered(trimmedDisplayName)
        }
    }
}
