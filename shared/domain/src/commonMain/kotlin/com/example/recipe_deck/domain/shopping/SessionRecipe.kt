package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.amount.Multiplier
import com.example.recipe_deck.domain.recipe.RecipeId
import kotlinx.collections.immutable.ImmutableList

data class SessionRecipe(
    val id: SessionRecipeId,
    val sourceRecipeId: RecipeId,
    val recipeName: String,
    val multiplier: Multiplier,
    val items: ImmutableList<SessionRecipeItem>,
) {
    init {
        require(recipeName.isNotBlank()) { "Recipe 名は空白だけにできない" }
    }
}
