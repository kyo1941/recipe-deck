package com.example.recipe_deck.domain.amount

internal tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)

internal fun Long.timesExact(other: Long): Long {
    val result = this * other
    if (this != 0L && result / this != other) throw ArithmeticException("分数の計算が Long の範囲を超えた")
    return result
}

internal fun Long.plusExact(other: Long): Long {
    val overflows = if (other > 0) this > Long.MAX_VALUE - other else this < Long.MIN_VALUE - other
    if (overflows) throw ArithmeticException("分数の計算が Long の範囲を超えた")
    return this + other
}
