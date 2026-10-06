package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.recipe.RecipeRepositoryFailure
import com.example.recipe_deck.domain.recipe.RecipeStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateRecipeUseCaseTest {
    private val recipeRepository = FakeRecipeRepository()
    private val createRecipe = CreateRecipeUseCase(recipeRepository)

    @Test
    fun 指定された名前と状態でRecipeを保存する() = runTest {
        val result = createRecipe(name = "麻婆豆腐", status = RecipeStatus.READY)

        val saved = recipeRepository.savedRecipes.single()
        assertEquals(CreateRecipeResult.Created(saved.id), result)
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

        assertEquals(CreateRecipeResult.NameRequired, result)
        assertEquals(emptyList(), recipeRepository.savedRecipes)
    }

    @Test
    fun 保存に失敗したら原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先に書き込めない")
        recipeRepository.saveFailure = RecipeRepositoryFailure.Unexpected(cause)

        val result = createRecipe(name = "麻婆豆腐", status = RecipeStatus.DRAFT)

        assertEquals(CreateRecipeResult.Unexpected(cause), result)
    }
}
