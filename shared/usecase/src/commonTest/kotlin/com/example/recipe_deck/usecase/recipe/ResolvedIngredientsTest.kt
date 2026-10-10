package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.amount.AmountUnit
import com.example.recipe_deck.domain.amount.Quantity
import com.example.recipe_deck.domain.amount.QuantityNotation
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.item.ItemReference
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolvedIngredientsTest {
    private val groundMeat = Item(ItemId("item-1"), "ひき肉")
    private val tofu = Item(ItemId("item-2"), "豆腐")
    private val existingItems = listOf(groundMeat, tofu).associateBy { it.displayName }

    private fun ingredient(
        item: ItemReference,
        quantity: Quantity? = null,
        unit: AmountUnit? = null,
    ) = IngredientInput(item, quantity, QuantityNotation.FRACTION, unit)

    @Test
    fun 選んだ既存のItemと分量を入力の順に材料にする() {
        val resolved = resolveIngredients(
            listOf(
                ingredient(ItemReference.Registered(tofu.id), Quantity.of(1), AmountUnit.PIECE),
                ingredient(ItemReference.Registered(groundMeat.id), Quantity.of(200), AmountUnit.GRAM),
            ),
            existingItems,
        )

        assertEquals(listOf(tofu.id, groundMeat.id), resolved.recipeIngredients.map { it.itemId })
        assertEquals(
            listOf(
                Amount(Quantity.of(1), QuantityNotation.FRACTION, AmountUnit.PIECE),
                Amount(Quantity.of(200), QuantityNotation.FRACTION, AmountUnit.GRAM),
            ),
            resolved.recipeIngredients.map { it.amount },
        )
        assertEquals(emptyList(), resolved.newItems)
    }

    @Test
    fun 新しい表示名の材料は前後の空白を除いた表示名でItemを作って参照する() {
        val resolved = resolveIngredients(listOf(ingredient(ItemReference.Unregistered("　長ネギ \n"))), existingItems)

        val created = resolved.newItems.single()
        assertEquals("長ネギ", created.displayName)
        assertEquals(created.id, resolved.recipeIngredients.single().itemId)
    }

    @Test
    fun 新しい表示名が既存のItemと完全に一致したら作らずに既存のItemを参照して寄せた分として返す() {
        val resolved = resolveIngredients(listOf(ingredient(ItemReference.Unregistered(" ひき肉 "))), existingItems)

        assertEquals(groundMeat.id, resolved.recipeIngredients.single().itemId)
        assertEquals(emptyList(), resolved.newItems)
        assertEquals(listOf(groundMeat), resolved.matchedExistingItems)
    }

    @Test
    fun 同じ新しい表示名の材料が複数あればItemを1つだけ作って両方が参照する() {
        val resolved = resolveIngredients(
            listOf(ingredient(ItemReference.Unregistered("長ネギ")), ingredient(ItemReference.Unregistered("長ネギ "))),
            existingItems,
        )

        val created = resolved.newItems.single()
        assertEquals(listOf(created.id, created.id), resolved.recipeIngredients.map { it.itemId })
    }

    @Test
    fun 同じ既存のItemに複数の材料が寄っても寄せた分には1つだけ含める() {
        val resolved = resolveIngredients(
            listOf(ingredient(ItemReference.Unregistered("ひき肉")), ingredient(ItemReference.Unregistered(" ひき肉"))),
            existingItems,
        )

        assertEquals(listOf(groundMeat), resolved.matchedExistingItems)
    }

    @Test
    fun 無視してよい材料は除く() {
        val resolved = resolveIngredients(
            listOf(ingredient(ItemReference.Unregistered(" ")), ingredient(ItemReference.Registered(tofu.id))),
            existingItems,
        )

        assertEquals(listOf(tofu.id), resolved.recipeIngredients.map { it.itemId })
    }
}
