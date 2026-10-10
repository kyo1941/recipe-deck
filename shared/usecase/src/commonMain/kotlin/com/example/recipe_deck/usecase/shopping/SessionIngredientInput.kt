package com.example.recipe_deck.usecase.shopping

import com.example.recipe_deck.usecase.recipe.IngredientInput

data class SessionIngredientInput(
    val ingredient: IngredientInput,
    val isExcluded: Boolean,
)
