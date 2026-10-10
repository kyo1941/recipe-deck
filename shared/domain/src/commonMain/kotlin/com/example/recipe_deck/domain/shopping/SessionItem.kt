package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.item.ItemReference

data class SessionItem(
    val id: SessionItemId,
    val item: ItemReference,
    val manualAddition: ManualAddition?,
) {
    init {
        if (item is ItemReference.Unregistered) {
            require(item.displayName.isNotBlank()) { "Session 限定の品目の表示名は空白だけにできない" }
        }
    }
}

data class ManualAddition(
    val amount: Amount?,
)
