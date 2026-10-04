package com.example.recipe_deck.domain.recipe

data class Recipe(
    val id: RecipeId,
    val name: String,
    val status: RecipeStatus,
    val photoRef: String?,
    val memo: String?,
    val ingredients: List<RecipeIngredient>,
) {
    init {
        require(name.isNotBlank()) { "Recipe 名は空白だけにできない" }
        require(ingredients.distinctBy { it.id }.size == ingredients.size) { "同じ材料行が Recipe に重複している" }
    }
}
