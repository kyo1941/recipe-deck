@file:OptIn(ExperimentalTime::class)

package com.example.recipe_deck.domain.shopping

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

sealed interface SessionState {
    data object Active : SessionState

    data class Completed(val completedAt: Instant) : SessionState
}
