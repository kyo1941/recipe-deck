package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.Outcome

interface ShoppingSessionRepository {
    suspend fun save(session: ShoppingSession): Outcome<Unit, ShoppingSessionRepositoryFailure>
    suspend fun findActive(): Outcome<ShoppingSession?, ShoppingSessionRepositoryFailure>
}
