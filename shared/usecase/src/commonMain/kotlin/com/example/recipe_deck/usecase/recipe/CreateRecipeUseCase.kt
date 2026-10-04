@file:OptIn(ExperimentalUuidApi::class)

package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.recipe.Recipe
import com.example.recipe_deck.domain.recipe.RecipeId
import com.example.recipe_deck.domain.recipe.RecipeRepository
import com.example.recipe_deck.domain.recipe.RecipeStatus
import kotlinx.collections.immutable.persistentListOf
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CreateRecipeUseCase(
    private val recipeRepository: RecipeRepository,
) {
    suspend operator fun invoke(name: String, status: RecipeStatus): CreateRecipeResult {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return CreateRecipeResult.Failure.BlankName

        val recipe = Recipe(
            id = RecipeId(Uuid.random().toString()),
            name = trimmedName,
            status = status,
            photoRef = null,
            memo = null,
            ingredients = persistentListOf(),
        )
        return recipeRepository.save(recipe).toCreateRecipeResult(recipe.id)
    }

    private fun Result<Unit>.toCreateRecipeResult(recipeId: RecipeId): CreateRecipeResult = fold(
        onSuccess = { CreateRecipeResult.Success(recipeId) },
        onFailure = { cause ->
            // TODO: Result を変換する UseCase が増えたら、キャンセルを投げ直したうえで解体する処理を共通化する。変換のたびにキャンセルの扱いを書かずに済むようにするため。
            if (cause is CancellationException) throw cause
            CreateRecipeResult.Failure.Unexpected(cause)
        },
    )
}
