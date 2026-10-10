@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)

package com.example.recipe_deck.usecase.shopping

import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.amount.Multiplier
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemReference
import com.example.recipe_deck.domain.item.ItemRepository
import com.example.recipe_deck.domain.item.ItemRepositoryFailure
import com.example.recipe_deck.domain.recipe.RecipeId
import com.example.recipe_deck.domain.recipe.RecipeRepository
import com.example.recipe_deck.domain.recipe.RecipeRepositoryFailure
import com.example.recipe_deck.domain.recipe.RecipeStatus
import com.example.recipe_deck.domain.shopping.SessionItem
import com.example.recipe_deck.domain.shopping.SessionItemId
import com.example.recipe_deck.domain.shopping.SessionRecipe
import com.example.recipe_deck.domain.shopping.SessionRecipeId
import com.example.recipe_deck.domain.shopping.SessionRecipeItem
import com.example.recipe_deck.domain.shopping.SessionRecipeItemId
import com.example.recipe_deck.domain.shopping.SessionState
import com.example.recipe_deck.domain.shopping.ShoppingSession
import com.example.recipe_deck.domain.shopping.ShoppingSessionId
import com.example.recipe_deck.domain.shopping.ShoppingSessionRepository
import com.example.recipe_deck.domain.shopping.ShoppingSessionRepositoryFailure
import com.example.recipe_deck.usecase.item.linkToExistingItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AddRecipeToShoppingSessionUseCase(
    private val recipeRepository: RecipeRepository,
    private val itemRepository: ItemRepository,
    private val shoppingSessionRepository: ShoppingSessionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        recipeId: RecipeId,
        multiplier: Multiplier,
        ingredients: List<SessionIngredientInput>,
    ): AddRecipeToShoppingSessionResult {
        ingredients.indices
            .filter { ingredients[it].ingredient.isInvalid }
            .takeIf { it.isNotEmpty() }
            ?.let { return AddRecipeToShoppingSessionResult.IngredientDisplayNameRequired(it.toImmutableList()) }

        val recipe = when (val found = recipeRepository.findById(recipeId)) {
            is Outcome.Success -> found.value ?: return AddRecipeToShoppingSessionResult.RecipeNoLongerExists
            is Outcome.Failure -> return found.error.toAddRecipeToShoppingSessionResult()
        }
        if (recipe.status == RecipeStatus.DRAFT) return AddRecipeToShoppingSessionResult.RecipeIsDraft

        val existingItems = when (val found = itemRepository.findAll()) {
            is Outcome.Success -> found.value.associateBy { it.displayName }
            is Outcome.Failure -> return found.error.toAddRecipeToShoppingSessionResult()
        }
        val existingItemsById = existingItems.values.associateBy { it.id }

        val session = when (val found = shoppingSessionRepository.findActive()) {
            is Outcome.Success -> found.value ?: newSession()
            is Outcome.Failure -> return found.error.toAddRecipeToShoppingSessionResult()
        }

        val sessionItems = session.items.associateByTo(mutableMapOf()) { it.item }
        val matchedExistingItems = mutableSetOf<Item>()
        val recipeItems = ingredients
            .filterNot { it.ingredient.isIgnorable }
            .map { input ->
                val item = when (val inputItem = input.ingredient.item) {
                    is ItemReference.Registered -> inputItem
                    is ItemReference.Unregistered -> inputItem.linkToExistingItem(existingItems).also { linked ->
                        if (linked is ItemReference.Registered) existingItemsById[linked.itemId]?.let { matchedExistingItems += it }
                    }
                }
                val sessionItem = sessionItems.getOrPut(item) {
                    SessionItem(SessionItemId(Uuid.random().toString()), item, manualAddition = null)
                }
                SessionRecipeItem(
                    id = SessionRecipeItemId(Uuid.random().toString()),
                    sessionItemId = sessionItem.id,
                    amount = input.ingredient.amount,
                    isExcluded = input.isExcluded,
                )
            }
        val sessionRecipe = SessionRecipe(
            id = SessionRecipeId(Uuid.random().toString()),
            sourceRecipeId = recipe.id,
            recipeName = recipe.name,
            multiplier = multiplier,
            items = recipeItems.toImmutableList(),
        )
        val updatedSession = session.copy(
            recipes = (session.recipes + sessionRecipe).toImmutableList(),
            items = sessionItems.values.toImmutableList(),
        )

        return when (val saved = shoppingSessionRepository.save(updatedSession)) {
            is Outcome.Success -> AddRecipeToShoppingSessionResult.Added(matchedExistingItems.toImmutableList())
            is Outcome.Failure -> saved.error.toAddRecipeToShoppingSessionResult()
        }
    }

    private fun newSession() = ShoppingSession(
        id = ShoppingSessionId(Uuid.random().toString()),
        state = SessionState.Active,
        createdAt = clock.now(),
        recipes = persistentListOf(),
        items = persistentListOf(),
    )

    private fun RecipeRepositoryFailure.toAddRecipeToShoppingSessionResult(): AddRecipeToShoppingSessionResult =
        when (this) {
            is RecipeRepositoryFailure.Unexpected -> AddRecipeToShoppingSessionResult.Unexpected(cause)
        }

    private fun ItemRepositoryFailure.toAddRecipeToShoppingSessionResult(): AddRecipeToShoppingSessionResult =
        when (this) {
            is ItemRepositoryFailure.Unexpected -> AddRecipeToShoppingSessionResult.Unexpected(cause)
        }

    private fun ShoppingSessionRepositoryFailure.toAddRecipeToShoppingSessionResult(): AddRecipeToShoppingSessionResult =
        when (this) {
            is ShoppingSessionRepositoryFailure.Unexpected -> AddRecipeToShoppingSessionResult.Unexpected(cause)
        }
}

sealed interface AddRecipeToShoppingSessionResult {
    data class Added(val matchedExistingItems: ImmutableList<Item>) : AddRecipeToShoppingSessionResult
    data object RecipeIsDraft : AddRecipeToShoppingSessionResult
    data object RecipeNoLongerExists : AddRecipeToShoppingSessionResult
    data class IngredientDisplayNameRequired(val ingredientIndexes: ImmutableList<Int>) : AddRecipeToShoppingSessionResult
    data class Unexpected(val cause: Throwable) : AddRecipeToShoppingSessionResult
}
