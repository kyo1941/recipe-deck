package com.example.recipe_deck.domain

interface TransactionRunner {
    suspend fun <T> run(block: suspend TransactionScope<T>.() -> T): T
}

interface TransactionScope<T> {
    suspend fun rollback(result: T): Nothing
}
