package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.amount.AmountUnit
import com.example.recipe_deck.domain.amount.Multiplier
import com.example.recipe_deck.domain.amount.Quantity
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.recipe.RecipeId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ShoppingSessionTest {
    private val greenOnion = SessionItem(SessionItemId("session-green-onion"), ItemId("green-onion"), manualAddition = null)

    private fun mapoTofu(vararg items: SessionRecipeItem) = SessionRecipe(
        id = SessionRecipeId("session-mapo-tofu"),
        sourceRecipeId = RecipeId("mapo-tofu"),
        recipeName = "麻婆豆腐",
        multiplier = Multiplier.of(2),
        items = items.toList(),
    )

    private fun requires(sessionItem: SessionItem, rowId: String = "row-1") = SessionRecipeItem(
        id = SessionRecipeItemId(rowId),
        sessionItemId = sessionItem.id,
        amount = Amount(Quantity.of(1), AmountUnit.PIECE),
        isExcluded = false,
    )

    private fun session(recipes: List<SessionRecipe> = emptyList(), items: List<SessionItem> = emptyList()) =
        ShoppingSession(ShoppingSessionId("session-1"), SessionState.ACTIVE, recipes, items)

    @Test
    fun Recipe由来の材料と同じItemを手動でも追加できる() {
        val greenOnionAlsoManual = greenOnion.copy(manualAddition = ManualAddition(amount = null))

        val session = session(recipes = listOf(mapoTofu(requires(greenOnionAlsoManual))), items = listOf(greenOnionAlsoManual))

        assertEquals(ManualAddition(amount = null), session.items.single().manualAddition)
    }

    @Test
    fun 数量なしの手動追加だけでSessionItemを持てる() {
        val manualOnly = greenOnion.copy(manualAddition = ManualAddition(amount = null))

        assertEquals(listOf(manualOnly), session(items = listOf(manualOnly)).items)
    }

    @Test
    fun 同じItemのSessionItemを複数持てない() {
        val duplicated = SessionItem(SessionItemId("session-green-onion-2"), greenOnion.itemId, ManualAddition(amount = null))

        assertFailsWith<IllegalArgumentException> {
            session(recipes = listOf(mapoTofu(requires(greenOnion))), items = listOf(greenOnion, duplicated))
        }
    }

    @Test
    fun 同じSessionItemのIDを重複して持てない() {
        val tofu = SessionItem(greenOnion.id, ItemId("tofu"), ManualAddition(amount = null))
        val manualGreenOnion = greenOnion.copy(manualAddition = ManualAddition(amount = null))

        assertFailsWith<IllegalArgumentException> { session(items = listOf(manualGreenOnion, tofu)) }
    }

    @Test
    fun 同じSessionRecipeのIDを重複して持てない() {
        assertFailsWith<IllegalArgumentException> { session(recipes = listOf(mapoTofu(), mapoTofu())) }
    }

    @Test
    fun Sessionに無いSessionItemを材料行から参照できない() {
        assertFailsWith<IllegalArgumentException> {
            session(recipes = listOf(mapoTofu(requires(greenOnion))), items = emptyList())
        }
    }

    @Test
    fun Recipe由来でも手動追加でもないSessionItemは持てない() {
        assertFailsWith<IllegalArgumentException> { session(items = listOf(greenOnion)) }
    }

    @Test
    fun 除外した材料行も参照としてSessionItemを支える() {
        val excluded = requires(greenOnion).copy(isExcluded = true)

        assertEquals(listOf(greenOnion), session(recipes = listOf(mapoTofu(excluded)), items = listOf(greenOnion)).items)
    }

    @Test
    fun Recipe名が空白だけのSessionRecipeは作れない() {
        assertFailsWith<IllegalArgumentException> { mapoTofu().copy(recipeName = " ") }
    }

    @Test
    fun 同じ材料行IDをSessionRecipeに重複して持てない() {
        assertFailsWith<IllegalArgumentException> { mapoTofu(requires(greenOnion, "row-1"), requires(greenOnion, "row-1")) }
    }
}
