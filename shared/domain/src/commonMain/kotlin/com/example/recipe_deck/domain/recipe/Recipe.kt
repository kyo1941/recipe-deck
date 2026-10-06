package com.example.recipe_deck.domain.recipe

import kotlinx.collections.immutable.ImmutableList

data class Recipe(
    val id: RecipeId,
    val name: String,
    val status: RecipeStatus,
    val photoRef: String?,
    val memo: String?,
    val ingredients: ImmutableList<RecipeIngredient>,
) {
    init {
        require(name.isNotBlank()) { "Recipe 名は空白だけにできない" }
        require(memo == null || memo.isNotBlank()) { "メモなしは null で表し、空白だけのメモは持てない" }
        require(ingredients.distinctBy { it.id }.size == ingredients.size) { "同じ材料行が Recipe に重複している" }
    }
}
