@file:OptIn(ExperimentalTime::class)

package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.amount.AmountUnit
import com.example.recipe_deck.domain.amount.Multiplier
import com.example.recipe_deck.domain.amount.Quantity
import com.example.recipe_deck.domain.amount.QuantityNotation
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.recipe.RecipeId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class ShoppingSessionTest {
    private val greenOnion = SessionItem(SessionItemId("session-green-onion"), ItemId("green-onion"), manualAddition = null)

    private fun mapoTofu(vararg items: SessionRecipeItem) = SessionRecipe(
        id = SessionRecipeId("session-mapo-tofu"),
        sourceRecipeId = RecipeId("mapo-tofu"),
        recipeName = "麻婆豆腐",
        multiplier = Multiplier.of(2),
        items = persistentListOf(*items),
    )

    private fun requires(sessionItem: SessionItem, rowId: String = "row-1") = SessionRecipeItem(
        id = SessionRecipeItemId(rowId),
        sessionItemId = sessionItem.id,
        amount = Amount(Quantity.of(1), QuantityNotation.FRACTION, AmountUnit.PIECE),
        isExcluded = false,
    )

    private val createdAt = Instant.parse("2026-10-04T10:00:00Z")

    private fun session(
        state: SessionState = SessionState.Active,
        recipes: ImmutableList<SessionRecipe> = persistentListOf(),
        items: ImmutableList<SessionItem> = persistentListOf(),
    ) =
        ShoppingSession(ShoppingSessionId("session-1"), state, createdAt, recipes, items)

    @Test
    fun 作成日時より後に完了したSessionを作れる() {
        val completed = SessionState.Completed(completedAt = Instant.parse("2026-10-04T11:00:00Z"))

        assertEquals(completed, session(state = completed).state)
    }

    @Test
    fun 完了日時が作成日時より前のSessionは作れない() {
        val completedBeforeCreation = SessionState.Completed(completedAt = Instant.parse("2026-10-04T09:00:00Z"))

        assertFailsWith<IllegalArgumentException> { session(state = completedBeforeCreation) }
    }

    @Test
    fun Recipe由来の材料と同じItemを手動でも追加できる() {
        val greenOnionAlsoManual = greenOnion.copy(manualAddition = ManualAddition(amount = null))

        val session = session(recipes = persistentListOf(mapoTofu(requires(greenOnionAlsoManual))), items = persistentListOf(greenOnionAlsoManual))

        assertEquals(ManualAddition(amount = null), session.items.single().manualAddition)
    }

    @Test
    fun 数量なしの手動追加だけでSessionItemを持てる() {
        val manualOnly = greenOnion.copy(manualAddition = ManualAddition(amount = null))

        assertEquals(persistentListOf(manualOnly), session(items = persistentListOf(manualOnly)).items)
    }

    @Test
    fun 同じItemのSessionItemを複数持てない() {
        val duplicated = SessionItem(SessionItemId("session-green-onion-2"), greenOnion.itemId, ManualAddition(amount = null))

        assertFailsWith<IllegalArgumentException> {
            session(recipes = persistentListOf(mapoTofu(requires(greenOnion))), items = persistentListOf(greenOnion, duplicated))
        }
    }

    @Test
    fun 同じSessionItemのIDを重複して持てない() {
        val tofu = SessionItem(greenOnion.id, ItemId("tofu"), ManualAddition(amount = null))
        val manualGreenOnion = greenOnion.copy(manualAddition = ManualAddition(amount = null))

        assertFailsWith<IllegalArgumentException> { session(items = persistentListOf(manualGreenOnion, tofu)) }
    }

    @Test
    fun 同じSessionRecipeのIDを重複して持てない() {
        assertFailsWith<IllegalArgumentException> { session(recipes = persistentListOf(mapoTofu(), mapoTofu())) }
    }

    @Test
    fun Sessionに無いSessionItemを材料行から参照できない() {
        assertFailsWith<IllegalArgumentException> {
            session(recipes = persistentListOf(mapoTofu(requires(greenOnion))), items = persistentListOf())
        }
    }

    @Test
    fun Recipe由来でも手動追加でもないSessionItemは持てない() {
        assertFailsWith<IllegalArgumentException> { session(items = persistentListOf(greenOnion)) }
    }

    @Test
    fun 除外した材料行も参照としてSessionItemを支える() {
        val excluded = requires(greenOnion).copy(isExcluded = true)

        assertEquals(persistentListOf(greenOnion), session(recipes = persistentListOf(mapoTofu(excluded)), items = persistentListOf(greenOnion)).items)
    }

    @Test
    fun Recipe名が空白だけのSessionRecipeは作れない() {
        assertFailsWith<IllegalArgumentException> { mapoTofu().copy(recipeName = " ") }
    }

    @Test
    fun 別々のSessionRecipeにまたがっても同じ材料行IDを重複して持てない() {
        val friedRice = mapoTofu(requires(greenOnion, "row-1")).copy(id = SessionRecipeId("session-fried-rice"))

        assertFailsWith<IllegalArgumentException> {
            session(recipes = persistentListOf(mapoTofu(requires(greenOnion, "row-1")), friedRice), items = persistentListOf(greenOnion))
        }
    }
}
