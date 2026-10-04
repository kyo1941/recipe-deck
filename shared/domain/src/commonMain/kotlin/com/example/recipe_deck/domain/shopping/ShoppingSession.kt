@file:OptIn(ExperimentalTime::class)

package com.example.recipe_deck.domain.shopping

import kotlinx.collections.immutable.ImmutableList
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class ShoppingSession(
    val id: ShoppingSessionId,
    val state: SessionState,
    val createdAt: Instant,
    val recipes: ImmutableList<SessionRecipe>,
    val items: ImmutableList<SessionItem>,
) {
    init {
        if (state is SessionState.Completed) {
            require(state.completedAt >= createdAt) { "完了日時が作成日時より前になっている" }
        }
        require(recipes.distinctBy { it.id }.size == recipes.size) { "同じ SessionRecipe が重複している" }
        require(items.distinctBy { it.id }.size == items.size) { "同じ SessionItem が重複している" }
        require(items.distinctBy { it.itemId }.size == items.size) { "同じ Item の SessionItem が複数ある" }

        val recipeItems = recipes.flatMap { it.items }
        require(recipeItems.distinctBy { it.id }.size == recipeItems.size) { "同じ材料行が Session に重複している" }

        val sessionItemIds = items.map { it.id }.toSet()
        val referencedSessionItemIds = recipeItems.map { it.sessionItemId }.toSet()
        require(sessionItemIds.containsAll(referencedSessionItemIds)) { "Session に無い SessionItem を参照している" }
        require(items.all { it.manualAddition != null || it.id in referencedSessionItemIds }) {
            "Recipe 由来でも手動追加でもない SessionItem がある"
        }
    }
}
