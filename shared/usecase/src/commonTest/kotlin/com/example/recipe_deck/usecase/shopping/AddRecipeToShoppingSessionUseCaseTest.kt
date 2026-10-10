@file:OptIn(ExperimentalTime::class)

package com.example.recipe_deck.usecase.shopping

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.amount.AmountUnit
import com.example.recipe_deck.domain.amount.Multiplier
import com.example.recipe_deck.domain.amount.Quantity
import com.example.recipe_deck.domain.amount.QuantityNotation
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.item.ItemReference
import com.example.recipe_deck.domain.item.ItemRepositoryFailure
import com.example.recipe_deck.domain.recipe.Recipe
import com.example.recipe_deck.domain.recipe.RecipeId
import com.example.recipe_deck.domain.recipe.RecipeIngredient
import com.example.recipe_deck.domain.recipe.RecipeIngredientId
import com.example.recipe_deck.domain.recipe.RecipeRepositoryFailure
import com.example.recipe_deck.domain.recipe.RecipeStatus
import com.example.recipe_deck.domain.shopping.SessionState
import com.example.recipe_deck.domain.shopping.ShoppingSessionRepositoryFailure
import com.example.recipe_deck.usecase.ingredient.IngredientInput
import com.example.recipe_deck.usecase.item.FakeItemRepository
import com.example.recipe_deck.usecase.recipe.FakeRecipeRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class AddRecipeToShoppingSessionUseCaseTest {
    private val recipeRepository = FakeRecipeRepository()
    private val itemRepository = FakeItemRepository()
    private val shoppingSessionRepository = FakeShoppingSessionRepository()
    private val now = Instant.parse("2026-10-09T10:00:00Z")
    private val addRecipe = AddRecipeToShoppingSessionUseCase(
        recipeRepository,
        itemRepository,
        shoppingSessionRepository,
        clock = object : Clock {
            override fun now() = this@AddRecipeToShoppingSessionUseCaseTest.now
        },
    )

    private val tofu = Item(ItemId("item-tofu"), "豆腐")
    private val greenOnion = Item(ItemId("item-green-onion"), "長ネギ")

    private val mapoTofu = Recipe(
        id = RecipeId("recipe-mapo-tofu"),
        name = "麻婆豆腐",
        status = RecipeStatus.READY,
        photoRef = null,
        memo = null,
        ingredients = persistentListOf(
            RecipeIngredient(RecipeIngredientId("ingredient-1"), tofu.id, amount = null),
        ),
    )

    private fun ingredient(
        item: ItemReference,
        quantity: Quantity? = null,
        unit: AmountUnit? = null,
        isExcluded: Boolean = false,
    ) = SessionIngredientInput(IngredientInput(item, quantity, QuantityNotation.FRACTION, unit), isExcluded)

    @Test
    fun 進行中のSessionが無ければ作ってRecipeを追加する() = runTest {
        recipeRepository.savedRecipes += mapoTofu

        val result = addRecipe(
            recipeId = mapoTofu.id,
            multiplier = Multiplier.of(1, 2),
            ingredients = listOf(ingredient(ItemReference.Registered(tofu.id), Quantity.of(1, 2), AmountUnit.PIECE)),
        )

        val saved = shoppingSessionRepository.savedSessions.single()
        val sessionRecipe = saved.recipes.single()
        assertEquals(AddRecipeToShoppingSessionResult.Added(persistentListOf()), result)
        assertEquals(SessionState.Active, saved.state)
        assertEquals(now, saved.createdAt)
        assertEquals(mapoTofu.id, sessionRecipe.sourceRecipeId)
        assertEquals("麻婆豆腐", sessionRecipe.recipeName)
        assertEquals(Multiplier.of(1, 2), sessionRecipe.multiplier)
        assertEquals(ItemReference.Registered(tofu.id), saved.items.single().item)
        assertEquals(
            Amount(Quantity.of(1, 2), QuantityNotation.FRACTION, AmountUnit.PIECE),
            sessionRecipe.items.single().amount,
        )
    }

    @Test
    fun 進行中のSessionがあればそこに追加する() = runTest {
        recipeRepository.savedRecipes += mapoTofu
        addRecipe(mapoTofu.id, Multiplier.of(1), listOf(ingredient(ItemReference.Registered(tofu.id))))
        val active = shoppingSessionRepository.savedSessions.single()

        addRecipe(mapoTofu.id, Multiplier.of(1), listOf(ingredient(ItemReference.Registered(greenOnion.id))))

        val saved = shoppingSessionRepository.savedSessions.last()
        assertEquals(active.id, saved.id)
        assertEquals(2, saved.recipes.size)
    }

    @Test
    fun 同じRecipeを2回追加すると別々のSessionRecipeになり同じItemのSessionItemを使い回す() = runTest {
        recipeRepository.savedRecipes += mapoTofu
        addRecipe(mapoTofu.id, Multiplier.of(1), listOf(ingredient(ItemReference.Registered(tofu.id), Quantity.of(1))))

        addRecipe(mapoTofu.id, Multiplier.of(1, 2), listOf(ingredient(ItemReference.Registered(tofu.id), Quantity.of(1, 2))))

        val saved = shoppingSessionRepository.savedSessions.last()
        val sessionItem = saved.items.single()
        assertEquals(listOf(Multiplier.of(1), Multiplier.of(1, 2)), saved.recipes.map { it.multiplier })
        assertEquals(listOf(sessionItem.id, sessionItem.id), saved.recipes.map { it.items.single().sessionItemId })
    }

    @Test
    fun 除外した材料も除外した行として追加する() = runTest {
        recipeRepository.savedRecipes += mapoTofu

        addRecipe(
            recipeId = mapoTofu.id,
            multiplier = Multiplier.of(1),
            ingredients = listOf(
                ingredient(ItemReference.Registered(tofu.id)),
                ingredient(ItemReference.Registered(greenOnion.id), isExcluded = true),
            ),
        )

        val recipeItems = shoppingSessionRepository.savedSessions.single().recipes.single().items
        assertEquals(listOf(false, true), recipeItems.map { it.isExcluded })
    }

    @Test
    fun 新しい表示名は既存のItemと一致すれば既存のItemを指し一致しなければSession限定の品目になる() = runTest {
        recipeRepository.savedRecipes += mapoTofu
        itemRepository.savedItems += greenOnion

        val result = addRecipe(
            recipeId = mapoTofu.id,
            multiplier = Multiplier.of(1),
            ingredients = listOf(
                ingredient(ItemReference.Unregistered(" 長ネギ")),
                ingredient(ItemReference.Unregistered("豆板醤 "), Quantity.of(15), AmountUnit.GRAM),
            ),
        )

        assertEquals(
            listOf(ItemReference.Registered(greenOnion.id), ItemReference.Unregistered("豆板醤")),
            shoppingSessionRepository.savedSessions.single().items.map { it.item },
        )
        assertEquals(listOf(greenOnion), itemRepository.savedItems)
        assertEquals(AddRecipeToShoppingSessionResult.Added(persistentListOf(greenOnion)), result)
    }

    @Test
    fun 同じ既存のItemに複数の材料が寄っても寄せた分には1つだけ含める() = runTest {
        recipeRepository.savedRecipes += mapoTofu
        itemRepository.savedItems += greenOnion

        val result = addRecipe(
            recipeId = mapoTofu.id,
            multiplier = Multiplier.of(1),
            ingredients = listOf(
                ingredient(ItemReference.Unregistered("長ネギ")),
                ingredient(ItemReference.Unregistered(" 長ネギ")),
                ingredient(ItemReference.Registered(greenOnion.id)),
            ),
        )

        assertEquals(AddRecipeToShoppingSessionResult.Added(persistentListOf(greenOnion)), result)
    }

    @Test
    fun 同じ表示名のSession限定の品目は1つにまとめる() = runTest {
        recipeRepository.savedRecipes += mapoTofu
        addRecipe(mapoTofu.id, Multiplier.of(1), listOf(ingredient(ItemReference.Unregistered("豆板醤"))))

        addRecipe(mapoTofu.id, Multiplier.of(1), listOf(ingredient(ItemReference.Unregistered("豆板醤"))))

        val saved = shoppingSessionRepository.savedSessions.last()
        assertEquals(ItemReference.Unregistered("豆板醤"), saved.items.single().item)
    }

    @Test
    fun 表示名も分量もない材料は追加しない() = runTest {
        recipeRepository.savedRecipes += mapoTofu

        addRecipe(
            recipeId = mapoTofu.id,
            multiplier = Multiplier.of(1),
            ingredients = listOf(ingredient(ItemReference.Unregistered(" ")), ingredient(ItemReference.Registered(tofu.id))),
        )

        assertEquals(1, shoppingSessionRepository.savedSessions.single().recipes.single().items.size)
    }

    @Test
    fun 材料が無くてもRecipeを追加できる() = runTest {
        recipeRepository.savedRecipes += mapoTofu

        val result = addRecipe(mapoTofu.id, Multiplier.of(1), emptyList())

        assertEquals(AddRecipeToShoppingSessionResult.Added(persistentListOf()), result)
        assertEquals(emptyList(), shoppingSessionRepository.savedSessions.single().recipes.single().items)
    }

    @Test
    fun 不正な材料の位置を返して何も保存しない() = runTest {
        recipeRepository.savedRecipes += mapoTofu

        val result = addRecipe(
            recipeId = mapoTofu.id,
            multiplier = Multiplier.of(1),
            ingredients = listOf(
                ingredient(ItemReference.Registered(tofu.id)),
                ingredient(ItemReference.Unregistered(" "), Quantity.of(1), AmountUnit.PIECE),
            ),
        )

        assertEquals(AddRecipeToShoppingSessionResult.IngredientDisplayNameRequired(persistentListOf(1)), result)
        assertEquals(emptyList(), shoppingSessionRepository.savedSessions)
    }

    @Test
    fun 下書きのRecipeは追加せずに失敗を返す() = runTest {
        recipeRepository.savedRecipes += mapoTofu.copy(status = RecipeStatus.DRAFT)

        val result = addRecipe(mapoTofu.id, Multiplier.of(1), emptyList())

        assertEquals(AddRecipeToShoppingSessionResult.RecipeIsDraft, result)
        assertEquals(emptyList(), shoppingSessionRepository.savedSessions)
    }

    @Test
    fun Recipeが無ければ追加せずに失敗を返す() = runTest {
        val result = addRecipe(mapoTofu.id, Multiplier.of(1), emptyList())

        assertEquals(AddRecipeToShoppingSessionResult.RecipeNoLongerExists, result)
        assertEquals(emptyList(), shoppingSessionRepository.savedSessions)
    }

    @Test
    fun Recipeの読み込みに失敗したら原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先から読み込めない")
        recipeRepository.findFailure = RecipeRepositoryFailure.Unexpected(cause)

        val result = addRecipe(mapoTofu.id, Multiplier.of(1), emptyList())

        assertEquals(AddRecipeToShoppingSessionResult.Unexpected(cause), result)
    }

    @Test
    fun Itemの読み込みに失敗したら原因を持った予期しない失敗を返す() = runTest {
        recipeRepository.savedRecipes += mapoTofu
        val cause = IllegalStateException("保存先から読み込めない")
        itemRepository.findFailure = ItemRepositoryFailure.Unexpected(cause)

        val result = addRecipe(mapoTofu.id, Multiplier.of(1), emptyList())

        assertEquals(AddRecipeToShoppingSessionResult.Unexpected(cause), result)
        assertEquals(emptyList(), shoppingSessionRepository.savedSessions)
    }

    @Test
    fun 進行中のSessionの読み込みに失敗したら原因を持った予期しない失敗を返す() = runTest {
        recipeRepository.savedRecipes += mapoTofu
        val cause = IllegalStateException("保存先から読み込めない")
        shoppingSessionRepository.findFailure = ShoppingSessionRepositoryFailure.Unexpected(cause)

        val result = addRecipe(mapoTofu.id, Multiplier.of(1), emptyList())

        assertEquals(AddRecipeToShoppingSessionResult.Unexpected(cause), result)
        assertEquals(emptyList(), shoppingSessionRepository.savedSessions)
    }

    @Test
    fun Sessionの保存に失敗したら原因を持った予期しない失敗を返す() = runTest {
        recipeRepository.savedRecipes += mapoTofu
        val cause = IllegalStateException("保存先に書き込めない")
        shoppingSessionRepository.saveFailure = ShoppingSessionRepositoryFailure.Unexpected(cause)

        val result = addRecipe(mapoTofu.id, Multiplier.of(1), emptyList())

        assertEquals(AddRecipeToShoppingSessionResult.Unexpected(cause), result)
    }
}
