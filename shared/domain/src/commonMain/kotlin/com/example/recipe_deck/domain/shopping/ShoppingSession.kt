package com.example.recipe_deck.domain.shopping

data class ShoppingSession(
    val id: ShoppingSessionId,
    val state: SessionState,
    val recipes: List<SessionRecipe>,
    val items: List<SessionItem>,
) {
    init {
        require(recipes.distinctBy { it.id }.size == recipes.size) { "同じ SessionRecipe が重複している" }
        require(items.distinctBy { it.id }.size == items.size) { "同じ SessionItem が重複している" }
        require(items.distinctBy { it.itemId }.size == items.size) { "同じ Item の SessionItem が複数ある" }

        val sessionItemIds = items.map { it.id }.toSet()
        val referencedSessionItemIds = recipes.flatMap { recipe -> recipe.items.map { it.sessionItemId } }.toSet()
        require(sessionItemIds.containsAll(referencedSessionItemIds)) { "Session に無い SessionItem を参照している" }
        require(items.all { it.manualAddition != null || it.id in referencedSessionItemIds }) {
            "Recipe 由来でも手動追加でもない SessionItem がある"
        }
    }
}
