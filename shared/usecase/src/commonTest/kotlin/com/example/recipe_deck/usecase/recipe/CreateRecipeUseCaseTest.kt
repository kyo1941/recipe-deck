package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.recipe.RecipeStatus
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateRecipeUseCaseTest {
    private val recipeRepository = FakeRecipeRepository()
    private val createRecipe = CreateRecipeUseCase(recipeRepository)

    @Test
    fun 指定された名前と状態でRecipeを保存する() = runTest {
        val result = createRecipe(name = "麻婆豆腐", status = RecipeStatus.READY)

        val saved = recipeRepository.savedRecipes.single()
        assertEquals(CreateRecipeResult.Success(saved.id), result)
        assertEquals("麻婆豆腐", saved.name)
        assertEquals(RecipeStatus.READY, saved.status)
        assertEquals(emptyList(), saved.ingredients)
    }

    @Test
    fun 名前の前後の空白を除いて保存する() = runTest {
        createRecipe(name = "　麻婆豆腐 \n", status = RecipeStatus.DRAFT)

        assertEquals("麻婆豆腐", recipeRepository.savedRecipes.single().name)
    }

    @Test
    fun 名前が空白だけなら保存せずに失敗を返す() = runTest {
        val result = createRecipe(name = " 　\n", status = RecipeStatus.DRAFT)

        assertEquals(CreateRecipeResult.Failure.BlankName, result)
        assertEquals(emptyList(), recipeRepository.savedRecipes)
    }

    @Test
    fun 保存に失敗したら原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先に書き込めない")
        recipeRepository.saveFailure = cause

        val result = createRecipe(name = "麻婆豆腐", status = RecipeStatus.DRAFT)

        assertEquals(CreateRecipeResult.Failure.Unexpected(cause), result)
    }

    @Test
    fun 保存がキャンセルされたら失敗として返さずに投げ直す() = runTest {
        recipeRepository.saveFailure = CancellationException("キャンセル")

        assertFailsWith<CancellationException> {
            createRecipe(name = "麻婆豆腐", status = RecipeStatus.DRAFT)
        }
    }
}
