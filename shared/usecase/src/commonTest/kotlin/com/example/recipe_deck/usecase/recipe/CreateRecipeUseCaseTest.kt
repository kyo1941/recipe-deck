package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.amount.AmountUnit
import com.example.recipe_deck.domain.amount.Quantity
import com.example.recipe_deck.domain.amount.QuantityNotation
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.item.ItemRepositoryFailure
import com.example.recipe_deck.domain.recipe.RecipeRepositoryFailure
import com.example.recipe_deck.domain.recipe.RecipeStatus
import com.example.recipe_deck.usecase.FakeTransactionRunner
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
        item: IngredientItem,
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
    fun 選んだ既存のItemと分量を入力の順に材料として保存する() = runTest {
        itemRepository.savedItems += listOf(groundMeat, tofu)

        createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(
                ingredient(IngredientItem.Existing(tofu.id), Quantity.of(1), AmountUnit.PIECE),
                ingredient(IngredientItem.Existing(groundMeat.id), Quantity.of(200), AmountUnit.GRAM),
            ),
        )

        val ingredients = recipeRepository.savedRecipes.single().ingredients
        assertEquals(listOf(tofu.id, groundMeat.id), ingredients.map { it.itemId })
        assertEquals(
            listOf(
                Amount(Quantity.of(1), QuantityNotation.FRACTION, AmountUnit.PIECE),
                Amount(Quantity.of(200), QuantityNotation.FRACTION, AmountUnit.GRAM),
            ),
            ingredients.map { it.amount },
        )
    }

    @Test
    fun 新しい表示名の材料は前後の空白を除いた表示名でItemを作って参照する() = runTest {
        createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(ingredient(IngredientItem.New("　長ネギ \n"))),
        )

        val created = itemRepository.savedItems.single()
        assertEquals("長ネギ", created.displayName)
        assertEquals(created.id, recipeRepository.savedRecipes.single().ingredients.single().itemId)
    }

    @Test
    fun 新しい表示名が既存のItemと完全に一致したら作らずに既存のItemを参照して返す() = runTest {
        itemRepository.savedItems += groundMeat

        val result = createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(ingredient(IngredientItem.New(" ひき肉 "))),
        )

        val saved = recipeRepository.savedRecipes.single()
        assertEquals(CreateRecipeResult.Created(saved.id, persistentListOf(groundMeat)), result)
        assertEquals(groundMeat.id, saved.ingredients.single().itemId)
        assertEquals(listOf(groundMeat), itemRepository.savedItems)
    }

    @Test
    fun 同じ新しい表示名の材料が複数あればItemを1つだけ作って両方が参照する() = runTest {
        createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(
                ingredient(IngredientItem.New("ひき肉")),
                ingredient(IngredientItem.New("ひき肉 ")),
            ),
        )

        val created = itemRepository.savedItems.single()
        assertEquals(
            listOf(created.id, created.id),
            recipeRepository.savedRecipes.single().ingredients.map { it.itemId },
        )
    }

    @Test
    fun 不正な材料の位置を返して何も保存しない() = runTest {
        val result = createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(
                ingredient(IngredientItem.New(" 　"), Quantity.of(1), AmountUnit.PIECE),
                ingredient(IngredientItem.New("長ネギ")),
                ingredient(IngredientItem.New(""), Quantity.of(200), AmountUnit.GRAM),
            ),
        )

        assertEquals(CreateRecipeResult.IngredientDisplayNameRequired(persistentListOf(0, 2)), result)
        assertEquals(emptyList(), itemRepository.savedItems)
        assertEquals(emptyList(), recipeRepository.savedRecipes)
    }

    @Test
    fun 無視してよい材料は除いて保存する() = runTest {
        createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(
                ingredient(IngredientItem.New(" ")),
                ingredient(IngredientItem.New("長ネギ")),
            ),
        )

        val created = itemRepository.savedItems.single()
        assertEquals(listOf(created.id), recipeRepository.savedRecipes.single().ingredients.map { it.itemId })
    }

    @Test
    fun Itemの読み込みに失敗したら取り消して原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先を読めない")
        itemRepository.findFailure = ItemRepositoryFailure.Unexpected(cause)

        val result = createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(ingredient(IngredientItem.New("長ネギ"))),
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
            ingredients = listOf(ingredient(IngredientItem.New("長ネギ"))),
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
            ingredients = listOf(ingredient(IngredientItem.New("長ネギ"))),
        )

        assertEquals(CreateRecipeResult.Unexpected(cause), result)
        assertTrue(transactionRunner.rolledBack)
    }
}
