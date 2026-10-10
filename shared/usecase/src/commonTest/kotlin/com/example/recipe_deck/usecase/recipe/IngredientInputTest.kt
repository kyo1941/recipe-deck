package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.amount.AmountUnit
import com.example.recipe_deck.domain.amount.Quantity
import com.example.recipe_deck.domain.amount.QuantityNotation
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.item.ItemReference
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IngredientInputTest {
    private fun ingredient(
        item: ItemReference,
        quantity: Quantity? = null,
        unit: AmountUnit? = null,
    ) = IngredientInput(item, quantity, QuantityNotation.FRACTION, unit)

    @Test
    fun 表示名も分量もなければ無視してよい材料になる() {
        assertTrue(ingredient(ItemReference.Unregistered(" 　")).isIgnorable)
    }

    @Test
    fun 表示名がなく単位だけなら無視してよい材料になる() {
        assertTrue(ingredient(ItemReference.Unregistered(""), unit = AmountUnit.GRAM).isIgnorable)
    }

    @Test
    fun 表示名がなく数量があれば不正な材料になる() {
        val input = ingredient(ItemReference.Unregistered(" "), Quantity.of(1), AmountUnit.PIECE)

        assertTrue(input.isInvalid)
        assertFalse(input.isIgnorable)
    }

    @Test
    fun 表示名があれば分量がなくても無視してよい材料でも不正な材料でもない() {
        val input = ingredient(ItemReference.Unregistered("長ネギ"))

        assertFalse(input.isIgnorable)
        assertFalse(input.isInvalid)
    }

    @Test
    fun 既存のItemを選んでいれば分量がなくても無視してよい材料にならない() {
        assertFalse(ingredient(ItemReference.Registered(ItemId("item-1"))).isIgnorable)
    }
}
