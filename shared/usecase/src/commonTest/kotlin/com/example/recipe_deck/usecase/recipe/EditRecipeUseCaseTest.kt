package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.recipe.Recipe
import com.example.recipe_deck.domain.recipe.RecipeId
import com.example.recipe_deck.domain.recipe.RecipeIngredient
import com.example.recipe_deck.domain.recipe.RecipeIngredientId
import com.example.recipe_deck.domain.recipe.RecipeRepositoryFailure
import com.example.recipe_deck.domain.recipe.RecipeStatus
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EditRecipeUseCaseTest {
    private val recipeRepository = FakeRecipeRepository()
    private val editRecipe = EditRecipeUseCase(recipeRepository)

    private val recipe = Recipe(
        id = RecipeId("recipe-1"),
        name = "麻婆豆腐",
        status = RecipeStatus.DRAFT,
        photoRef = "photo-1",
        memo = null,
        ingredients = persistentListOf(
            RecipeIngredient(id = RecipeIngredientId("ingredient-1"), itemId = ItemId("item-1"), amount = null),
        ),
    )

    @Test
    fun 指定された名前と状態とメモでRecipeを更新する() = runTest {
        recipeRepository.savedRecipes += recipe

        val result = editRecipe(
            recipeId = recipe.id,
            name = "麻婆豆腐（辛口）",
            status = RecipeStatus.READY,
            memo = "花椒を多めに",
        )

        assertEquals(EditRecipeResult.Saved, result)
        val saved = recipeRepository.savedRecipes.last()
        assertEquals("麻婆豆腐（辛口）", saved.name)
        assertEquals(RecipeStatus.READY, saved.status)
        assertEquals("花椒を多めに", saved.memo)
    }

    @Test
    fun 写真と材料は変えずに保存する() = runTest {
        recipeRepository.savedRecipes += recipe

        editRecipe(recipeId = recipe.id, name = "麻婆豆腐（辛口）", status = RecipeStatus.READY, memo = "")

        val saved = recipeRepository.savedRecipes.last()
        assertEquals(recipe.id, saved.id)
        assertEquals(recipe.photoRef, saved.photoRef)
        assertEquals(recipe.ingredients, saved.ingredients)
    }

    @Test
    fun 名前とメモの前後の空白を除いて保存する() = runTest {
        recipeRepository.savedRecipes += recipe

        editRecipe(recipeId = recipe.id, name = "　麻婆豆腐 \n", status = RecipeStatus.DRAFT, memo = " 花椒を多めに\n")

        val saved = recipeRepository.savedRecipes.last()
        assertEquals("麻婆豆腐", saved.name)
        assertEquals("花椒を多めに", saved.memo)
    }

    @Test
    fun メモが空白だけならメモなしとして保存する() = runTest {
        recipeRepository.savedRecipes += recipe.copy(memo = "花椒を多めに")

        editRecipe(recipeId = recipe.id, name = "麻婆豆腐", status = RecipeStatus.DRAFT, memo = " 　\n")

        assertNull(recipeRepository.savedRecipes.last().memo)
    }

    @Test
    fun 名前が空白だけなら保存せずに失敗を返す() = runTest {
        recipeRepository.savedRecipes += recipe

        val result = editRecipe(recipeId = recipe.id, name = " 　\n", status = RecipeStatus.DRAFT, memo = "")

        assertEquals(EditRecipeResult.NameRequired, result)
        assertEquals(listOf(recipe), recipeRepository.savedRecipes)
    }

    @Test
    fun Recipeが無ければ保存せずに失敗を返す() = runTest {
        val result = editRecipe(recipeId = recipe.id, name = "麻婆豆腐", status = RecipeStatus.DRAFT, memo = "")

        assertEquals(EditRecipeResult.RecipeNoLongerExists, result)
        assertEquals(emptyList(), recipeRepository.savedRecipes)
    }

    @Test
    fun 読み込みに失敗したら原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先から読み込めない")
        recipeRepository.findFailure = RecipeRepositoryFailure.Unexpected(cause)

        val result = editRecipe(recipeId = recipe.id, name = "麻婆豆腐", status = RecipeStatus.DRAFT, memo = "")

        assertEquals(EditRecipeResult.Unexpected(cause), result)
    }

    @Test
    fun 保存に失敗したら原因を持った予期しない失敗を返す() = runTest {
        recipeRepository.savedRecipes += recipe
        val cause = IllegalStateException("保存先に書き込めない")
        recipeRepository.saveFailure = RecipeRepositoryFailure.Unexpected(cause)

        val result = editRecipe(recipeId = recipe.id, name = "麻婆豆腐", status = RecipeStatus.DRAFT, memo = "")

        assertEquals(EditRecipeResult.Unexpected(cause), result)
    }
}
