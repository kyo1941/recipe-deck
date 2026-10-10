package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.amount.AmountUnit
import com.example.recipe_deck.domain.amount.Quantity
import com.example.recipe_deck.domain.amount.QuantityNotation
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.item.ItemReference
import com.example.recipe_deck.domain.item.ItemRepositoryFailure
import com.example.recipe_deck.domain.recipe.RecipeRepositoryFailure
import com.example.recipe_deck.domain.recipe.RecipeStatus
import com.example.recipe_deck.usecase.FakeTransactionRunner
import com.example.recipe_deck.usecase.ingredient.IngredientInput
import com.example.recipe_deck.usecase.item.FakeItemRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateRecipeUseCaseTest {
    private val recipeRepository = FakeRecipeRepository()
    private val itemRepository = FakeItemRepository()
    private val transactionRunner = FakeTransactionRunner()
    private val createRecipe = CreateRecipeUseCase(recipeRepository, itemRepository, transactionRunner)

    private val groundMeat = Item(ItemId("item-1"), "ひき肉")
    private val tofu = Item(ItemId("item-2"), "豆腐")

    private fun ingredient(
        item: ItemReference,
        quantity: Quantity? = null,
        unit: AmountUnit? = null,
    ) = IngredientInput(item, quantity, QuantityNotation.FRACTION, unit)

    @Test
    fun 指定された名前と状態でRecipeを保存する() = runTest {
        val result = createRecipe(name = "麻婆豆腐", status = RecipeStatus.READY, ingredients = emptyList())

        val saved = recipeRepository.savedRecipes.single()
        assertEquals(CreateRecipeResult.Created(saved.id, persistentListOf()), result)
        assertEquals("麻婆豆腐", saved.name)
        assertEquals(RecipeStatus.READY, saved.status)
        assertEquals(emptyList(), saved.ingredients)
    }

    @Test
    fun 名前の前後の空白を除いて保存する() = runTest {
        createRecipe(name = "　麻婆豆腐 \n", status = RecipeStatus.DRAFT, ingredients = emptyList())

        assertEquals("麻婆豆腐", recipeRepository.savedRecipes.single().name)
    }

    @Test
    fun 名前が空白だけなら保存せずに失敗を返す() = runTest {
        val result = createRecipe(name = " 　\n", status = RecipeStatus.DRAFT, ingredients = emptyList())

        assertEquals(CreateRecipeResult.NameRequired, result)
        assertEquals(emptyList(), recipeRepository.savedRecipes)
    }

    @Test
    fun 材料を持ったRecipeと新しいItemを保存して既存に寄せたItemを返す() = runTest {
        itemRepository.savedItems += listOf(groundMeat, tofu)

        val result = createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(
                ingredient(ItemReference.Registered(tofu.id), Quantity.of(1), AmountUnit.PIECE),
                ingredient(ItemReference.Unregistered("長ネギ")),
                ingredient(ItemReference.Unregistered(" ひき肉")),
            ),
        )

        val saved = recipeRepository.savedRecipes.single()
        val created = itemRepository.savedItems.single { it.displayName == "長ネギ" }
        assertEquals(CreateRecipeResult.Created(saved.id, persistentListOf(groundMeat)), result)
        assertEquals(listOf(tofu.id, created.id, groundMeat.id), saved.ingredients.map { it.itemId })
        assertEquals(
            listOf(Amount(Quantity.of(1), QuantityNotation.FRACTION, AmountUnit.PIECE), null, null),
            saved.ingredients.map { it.amount },
        )
    }

    @Test
    fun 不正な材料の位置を返して何も保存しない() = runTest {
        val result = createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(
                ingredient(ItemReference.Unregistered(" 　"), Quantity.of(1), AmountUnit.PIECE),
                ingredient(ItemReference.Unregistered("長ネギ")),
                ingredient(ItemReference.Unregistered(""), Quantity.of(200), AmountUnit.GRAM),
            ),
        )

        assertEquals(CreateRecipeResult.IngredientDisplayNameRequired(persistentListOf(0, 2)), result)
        assertEquals(emptyList(), itemRepository.savedItems)
        assertEquals(emptyList(), recipeRepository.savedRecipes)
    }

    @Test
    fun Itemの読み込みに失敗したら取り消して原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先を読めない")
        itemRepository.findFailure = ItemRepositoryFailure.Unexpected(cause)

        val result = createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(ingredient(ItemReference.Unregistered("長ネギ"))),
        )

        assertEquals(CreateRecipeResult.Unexpected(cause), result)
        assertTrue(transactionRunner.rolledBack)
        assertEquals(emptyList(), recipeRepository.savedRecipes)
    }

    @Test
    fun Itemの保存に失敗したら取り消して原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先に書き込めない")
        itemRepository.saveFailure = ItemRepositoryFailure.Unexpected(cause)

        val result = createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(ingredient(ItemReference.Unregistered("長ネギ"))),
        )

        assertEquals(CreateRecipeResult.Unexpected(cause), result)
        assertTrue(transactionRunner.rolledBack)
        assertEquals(emptyList(), recipeRepository.savedRecipes)
    }

    @Test
    fun Recipeの保存に失敗したら取り消して原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先に書き込めない")
        recipeRepository.saveFailure = RecipeRepositoryFailure.Unexpected(cause)

        val result = createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(ingredient(ItemReference.Unregistered("長ネギ"))),
        )

        assertEquals(CreateRecipeResult.Unexpected(cause), result)
        assertTrue(transactionRunner.rolledBack)
    }
}
