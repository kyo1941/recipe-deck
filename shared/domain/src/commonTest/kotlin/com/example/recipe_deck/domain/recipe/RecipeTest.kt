package com.example.recipe_deck.domain.recipe

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.amount.AmountUnit
import com.example.recipe_deck.domain.amount.Quantity
import com.example.recipe_deck.domain.amount.QuantityNotation
import com.example.recipe_deck.domain.item.ItemId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecipeTest {
    private val greenOnion = ItemId("green-onion")

    private fun recipe(
        name: String = "麻婆豆腐",
        memo: String? = null,
        ingredients: ImmutableList<RecipeIngredient> = persistentListOf(),
    ) = Recipe(
        id = RecipeId("mapo-tofu"),
        name = name,
        status = RecipeStatus.DRAFT,
        photoRef = null,
        memo = memo,
        ingredients = ingredients,
    )

    @Test
    fun 名前だけで材料0件のRecipeを作れる() {
        assertEquals(emptyList(), recipe().ingredients)
    }

    @Test
    fun 名前が空白だけのRecipeは作れない() {
        assertFailsWith<IllegalArgumentException> { recipe(name = "  ") }
    }

    @Test
    fun メモが空白だけのRecipeは作れない() {
        assertFailsWith<IllegalArgumentException> { recipe(memo = " \n") }
    }

    @Test
    fun 同じItemを複数の材料行に持てる() {
        val ingredients = persistentListOf(
            RecipeIngredient(RecipeIngredientId("row-1"), greenOnion, Amount(Quantity.of(1, 2), QuantityNotation.FRACTION, AmountUnit.PIECE)),
            RecipeIngredient(RecipeIngredientId("row-2"), greenOnion, null),
        )

        assertEquals(ingredients, recipe(ingredients = ingredients).ingredients)
    }

    @Test
    fun 同じ材料行IDを重複して持てない() {
        val ingredients = persistentListOf(
            RecipeIngredient(RecipeIngredientId("row-1"), greenOnion, null),
            RecipeIngredient(RecipeIngredientId("row-1"), ItemId("tofu"), null),
        )

        assertFailsWith<IllegalArgumentException> { recipe(ingredients = ingredients) }
    }
}
