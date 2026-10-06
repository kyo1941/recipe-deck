@file:OptIn(ExperimentalUuidApi::class)

package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.recipe.Recipe
import com.example.recipe_deck.domain.recipe.RecipeId
import com.example.recipe_deck.domain.recipe.RecipeRepository
import com.example.recipe_deck.domain.recipe.RecipeRepositoryFailure
import com.example.recipe_deck.domain.recipe.RecipeStatus
import kotlinx.collections.immutable.persistentListOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CreateRecipeUseCase(
    private val recipeRepository: RecipeRepository,
) {
    suspend operator fun invoke(name: String, status: RecipeStatus): CreateRecipeResult {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return CreateRecipeResult.NameRequired

        val recipe = Recipe(
            id = RecipeId(Uuid.random().toString()),
            name = trimmedName,
            status = status,
            photoRef = null,
            memo = null,
            ingredients = persistentListOf(),
        )
        return when (val saved = recipeRepository.save(recipe)) {
            is Outcome.Success -> CreateRecipeResult.Created(recipe.id)
            is Outcome.Failure -> saved.error.toCreateRecipeResult()
        }
    }

    private fun RecipeRepositoryFailure.toCreateRecipeResult(): CreateRecipeResult = when (this) {
        is RecipeRepositoryFailure.Unexpected -> CreateRecipeResult.Unexpected(cause)
    }
}

sealed interface CreateRecipeResult {
    data class Created(val recipeId: RecipeId) : CreateRecipeResult
    data object NameRequired : CreateRecipeResult
    data class Unexpected(val cause: Throwable) : CreateRecipeResult
}
