package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.recipe.RecipeId
import com.example.recipe_deck.domain.recipe.RecipeRepository
import com.example.recipe_deck.domain.recipe.RecipeRepositoryFailure
import com.example.recipe_deck.domain.recipe.RecipeStatus

class EditRecipeUseCase(
    private val recipeRepository: RecipeRepository,
) {
    suspend operator fun invoke(
        recipeId: RecipeId,
        name: String,
        status: RecipeStatus,
        memo: String,
    ): EditRecipeResult {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return EditRecipeResult.NameRequired

        val recipe = when (val found = recipeRepository.findById(recipeId)) {
            is Outcome.Success -> found.value ?: return EditRecipeResult.RecipeNoLongerExists
            is Outcome.Failure -> return found.error.toEditRecipeResult()
        }

        val editedRecipe = recipe.copy(
            name = trimmedName,
            status = status,
            memo = memo.trim().ifEmpty { null },
        )
        return when (val saved = recipeRepository.save(editedRecipe)) {
            is Outcome.Success -> EditRecipeResult.Saved
            is Outcome.Failure -> saved.error.toEditRecipeResult()
        }
    }

    private fun RecipeRepositoryFailure.toEditRecipeResult(): EditRecipeResult = when (this) {
        is RecipeRepositoryFailure.Unexpected -> EditRecipeResult.Unexpected(cause)
    }
}

sealed interface EditRecipeResult {
    data object Saved : EditRecipeResult
    data object NameRequired : EditRecipeResult
    data object RecipeNoLongerExists : EditRecipeResult
    data class Unexpected(val cause: Throwable) : EditRecipeResult
}
