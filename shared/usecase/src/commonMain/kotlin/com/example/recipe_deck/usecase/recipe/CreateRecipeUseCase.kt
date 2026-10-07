@file:OptIn(ExperimentalUuidApi::class)

package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.TransactionRunner
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemRepository
import com.example.recipe_deck.domain.item.ItemRepositoryFailure
import com.example.recipe_deck.domain.recipe.Recipe
import com.example.recipe_deck.domain.recipe.RecipeId
import com.example.recipe_deck.domain.recipe.RecipeRepository
import com.example.recipe_deck.domain.recipe.RecipeRepositoryFailure
import com.example.recipe_deck.domain.recipe.RecipeStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CreateRecipeUseCase(
    private val recipeRepository: RecipeRepository,
    private val itemRepository: ItemRepository,
    private val transactionRunner: TransactionRunner,
) {
    suspend operator fun invoke(
        name: String,
        status: RecipeStatus,
        ingredients: List<IngredientInput>,
    ): CreateRecipeResult {
        val trimmedName = name.trim().ifEmpty { return CreateRecipeResult.NameRequired }

        ingredients.indices
            .filter { ingredients[it].isInvalid }
            .takeIf { it.isNotEmpty() }
            ?.let { return CreateRecipeResult.IngredientDisplayNameRequired(it.toImmutableList()) }

        return transactionRunner.run {
            val existingItems = when (val found = itemRepository.findAll()) {
                is Outcome.Success -> found.value.associateBy { it.displayName }
                is Outcome.Failure -> rollback(found.error.toCreateRecipeResult())
            }

            val resolved = resolveIngredients(ingredients, existingItems)

            resolved.newItems.forEach { item ->
                when (val saved = itemRepository.save(item)) {
                    is Outcome.Success -> Unit
                    is Outcome.Failure -> rollback(saved.error.toCreateRecipeResult())
                }
            }

            val recipe = Recipe(
                id = RecipeId(Uuid.random().toString()),
                name = trimmedName,
                status = status,
                photoRef = null,
                memo = null,
                ingredients = resolved.recipeIngredients,
            )
            when (val saved = recipeRepository.save(recipe)) {
                is Outcome.Success -> CreateRecipeResult.Created(recipe.id, resolved.matchedExistingItems)
                is Outcome.Failure -> rollback(saved.error.toCreateRecipeResult())
            }
        }
    }

    private fun RecipeRepositoryFailure.toCreateRecipeResult(): CreateRecipeResult = when (this) {
        is RecipeRepositoryFailure.Unexpected -> CreateRecipeResult.Unexpected(cause)
    }

    private fun ItemRepositoryFailure.toCreateRecipeResult(): CreateRecipeResult = when (this) {
        is ItemRepositoryFailure.Unexpected -> CreateRecipeResult.Unexpected(cause)
    }
}

sealed interface CreateRecipeResult {
    data class Created(val recipeId: RecipeId, val matchedExistingItems: ImmutableList<Item>) : CreateRecipeResult
    data object NameRequired : CreateRecipeResult
    data class IngredientDisplayNameRequired(val ingredientIndexes: ImmutableList<Int>) : CreateRecipeResult
    data class Unexpected(val cause: Throwable) : CreateRecipeResult
}
