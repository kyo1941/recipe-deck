package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.item.ItemId

data class SessionItem(
    val id: SessionItemId,
    val itemId: ItemId,
    val manualAddition: ManualAddition?,
)

data class ManualAddition(
    val amount: Amount?,
)
