package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.amount.AmountUnit
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
import com.example.recipe_deck.usecase.FakeTransactionRunner
import com.example.recipe_deck.usecase.item.FakeItemRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditRecipeUseCaseTest {
    private val recipeRepository = FakeRecipeRepository()
    private val itemRepository = FakeItemRepository()
    private val transactionRunner = FakeTransactionRunner()
    private val editRecipe = EditRecipeUseCase(recipeRepository, itemRepository, transactionRunner)

    private val groundMeat = Item(ItemId("item-1"), "ひき肉")
    private val tofu = Item(ItemId("item-2"), "豆腐")

    private val recipe = Recipe(
        id = RecipeId("recipe-1"),
        name = "麻婆豆腐",
        status = RecipeStatus.DRAFT,
        photoRef = "photo-1",
        memo = null,
        ingredients = persistentListOf(
            RecipeIngredient(id = RecipeIngredientId("ingredient-1"), itemId = groundMeat.id, amount = null),
        ),
    )

    private fun ingredient(
        item: ItemReference,
        quantity: Quantity? = null,
        unit: AmountUnit? = null,
    ) = IngredientInput(item, quantity, QuantityNotation.FRACTION, unit)

    @Test
    fun 指定された名前と状態とメモでRecipeを更新する() = runTest {
        recipeRepository.savedRecipes += recipe

        val result = editRecipe(
            recipeId = recipe.id,
            name = "麻婆豆腐（辛口）",
            status = RecipeStatus.READY,
            memo = "花椒を多めに",
            ingredients = emptyList(),
        )

        assertEquals(EditRecipeResult.Saved(persistentListOf()), result)
        val saved = recipeRepository.savedRecipes.last()
        assertEquals("麻婆豆腐（辛口）", saved.name)
        assertEquals(RecipeStatus.READY, saved.status)
        assertEquals("花椒を多めに", saved.memo)
    }

    @Test
    fun 写真は変えずに保存する() = runTest {
        recipeRepository.savedRecipes += recipe

        editRecipe(
            recipeId = recipe.id,
            name = "麻婆豆腐（辛口）",
            status = RecipeStatus.READY,
            memo = "",
            ingredients = emptyList(),
        )

        val saved = recipeRepository.savedRecipes.last()
        assertEquals(recipe.id, saved.id)
        assertEquals(recipe.photoRef, saved.photoRef)
    }

    @Test
    fun 材料を入力の内容で置き換える() = runTest {
        recipeRepository.savedRecipes += recipe
        itemRepository.savedItems += listOf(groundMeat, tofu)

        val result = editRecipe(
            recipeId = recipe.id,
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            memo = "",
            ingredients = listOf(
                ingredient(ItemReference.Registered(tofu.id), Quantity.of(1), AmountUnit.PIECE),
                ingredient(ItemReference.Unregistered("長ネギ")),
                ingredient(ItemReference.Unregistered(" ひき肉")),
            ),
        )

        val created = itemRepository.savedItems.single { it.displayName == "長ネギ" }
        val ingredients = recipeRepository.savedRecipes.last().ingredients
        assertEquals(EditRecipeResult.Saved(persistentListOf(groundMeat)), result)
        assertEquals(listOf(tofu.id, created.id, groundMeat.id), ingredients.map { it.itemId })
        assertEquals(
            listOf(Amount(Quantity.of(1), QuantityNotation.FRACTION, AmountUnit.PIECE), null, null),
            ingredients.map { it.amount },
        )
    }

    @Test
    fun 名前とメモの前後の空白を除いて保存する() = runTest {
        recipeRepository.savedRecipes += recipe

        editRecipe(
            recipeId = recipe.id,
            name = "　麻婆豆腐 \n",
            status = RecipeStatus.DRAFT,
            memo = " 花椒を多めに\n",
            ingredients = emptyList(),
        )

        val saved = recipeRepository.savedRecipes.last()
        assertEquals("麻婆豆腐", saved.name)
        assertEquals("花椒を多めに", saved.memo)
    }

    @Test
    fun メモが空白だけならメモなしとして保存する() = runTest {
        recipeRepository.savedRecipes += recipe.copy(memo = "花椒を多めに")

        editRecipe(
            recipeId = recipe.id,
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            memo = " 　\n",
            ingredients = emptyList(),
        )

        assertNull(recipeRepository.savedRecipes.last().memo)
    }

    @Test
    fun 名前が空白だけなら保存せずに失敗を返す() = runTest {
        recipeRepository.savedRecipes += recipe

        val result = editRecipe(
            recipeId = recipe.id,
            name = " 　\n",
            status = RecipeStatus.DRAFT,
            memo = "",
            ingredients = emptyList(),
        )

        assertEquals(EditRecipeResult.NameRequired, result)
        assertEquals(listOf(recipe), recipeRepository.savedRecipes)
    }

    @Test
    fun 不正な材料の位置を返して何も保存しない() = runTest {
        recipeRepository.savedRecipes += recipe

        val result = editRecipe(
            recipeId = recipe.id,
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            memo = "",
            ingredients = listOf(
                ingredient(ItemReference.Unregistered("長ネギ")),
                ingredient(ItemReference.Unregistered(" "), Quantity.of(1), AmountUnit.PIECE),
            ),
        )

        assertEquals(EditRecipeResult.IngredientDisplayNameRequired(persistentListOf(1)), result)
        assertEquals(emptyList(), itemRepository.savedItems)
        assertEquals(listOf(recipe), recipeRepository.savedRecipes)
    }

    @Test
    fun Recipeが無ければ保存せずに失敗を返す() = runTest {
        val result = editRecipe(
            recipeId = recipe.id,
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            memo = "",
            ingredients = emptyList(),
        )

        assertEquals(EditRecipeResult.RecipeNoLongerExists, result)
        assertEquals(emptyList(), recipeRepository.savedRecipes)
    }

    @Test
    fun Recipeの読み込みに失敗したら取り消して原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先から読み込めない")
        recipeRepository.findFailure = RecipeRepositoryFailure.Unexpected(cause)

        val result = editRecipe(
            recipeId = recipe.id,
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            memo = "",
            ingredients = emptyList(),
        )

        assertEquals(EditRecipeResult.Unexpected(cause), result)
        assertTrue(transactionRunner.rolledBack)
    }

    @Test
    fun Itemの読み込みに失敗したら取り消して原因を持った予期しない失敗を返す() = runTest {
        recipeRepository.savedRecipes += recipe
        val cause = IllegalStateException("保存先から読み込めない")
        itemRepository.findFailure = ItemRepositoryFailure.Unexpected(cause)

        val result = editRecipe(
            recipeId = recipe.id,
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            memo = "",
            ingredients = listOf(ingredient(ItemReference.Unregistered("長ネギ"))),
        )

        assertEquals(EditRecipeResult.Unexpected(cause), result)
        assertTrue(transactionRunner.rolledBack)
        assertEquals(listOf(recipe), recipeRepository.savedRecipes)
    }

    @Test
    fun Itemの保存に失敗したら取り消して原因を持った予期しない失敗を返す() = runTest {
        recipeRepository.savedRecipes += recipe
        val cause = IllegalStateException("保存先に書き込めない")
        itemRepository.saveFailure = ItemRepositoryFailure.Unexpected(cause)

        val result = editRecipe(
            recipeId = recipe.id,
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            memo = "",
            ingredients = listOf(ingredient(ItemReference.Unregistered("長ネギ"))),
        )

        assertEquals(EditRecipeResult.Unexpected(cause), result)
        assertTrue(transactionRunner.rolledBack)
        assertEquals(listOf(recipe), recipeRepository.savedRecipes)
    }

    @Test
    fun Recipeの保存に失敗したら取り消して原因を持った予期しない失敗を返す() = runTest {
        recipeRepository.savedRecipes += recipe
        val cause = IllegalStateException("保存先に書き込めない")
        recipeRepository.saveFailure = RecipeRepositoryFailure.Unexpected(cause)

        val result = editRecipe(
            recipeId = recipe.id,
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            memo = "",
            ingredients = listOf(ingredient(ItemReference.Unregistered("長ネギ"))),
        )

        assertEquals(EditRecipeResult.Unexpected(cause), result)
        assertTrue(transactionRunner.rolledBack)
    }
}
