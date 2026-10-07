package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.amount.AmountUnit
import com.example.recipe_deck.domain.amount.Quantity
import com.example.recipe_deck.domain.amount.QuantityNotation
import com.example.recipe_deck.domain.item.ItemId

data class IngredientInput(
    val item: IngredientItem,
    val quantity: Quantity?,
    val notation: QuantityNotation,
    val unit: AmountUnit?,
) {
    val amount: Amount? get() = Amount.ofOrNull(quantity, notation, unit)

    val isIgnorable: Boolean get() = hasBlankDisplayName && amount == null

    val isInvalid: Boolean get() = hasBlankDisplayName && amount != null

    private val hasBlankDisplayName: Boolean
        get() = item is IngredientItem.New && item.displayName.isBlank()
}

sealed interface IngredientItem {
    data class Existing(val itemId: ItemId) : IngredientItem
    data class New(val displayName: String) : IngredientItem
}
