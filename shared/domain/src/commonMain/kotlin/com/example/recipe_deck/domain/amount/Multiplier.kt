package com.example.recipe_deck.domain.amount

@ConsistentCopyVisibility
data class Multiplier private constructor(
    val numerator: Long,
    val denominator: Long,
) {
    companion object {
        fun of(numerator: Long, denominator: Long = 1): Multiplier {
            require(numerator > 0) { "倍率は正の値でなければならない: $numerator/$denominator" }
            require(denominator > 0) { "分母は正の値でなければならない: $numerator/$denominator" }
            val divisor = gcd(numerator, denominator)
            return Multiplier(numerator / divisor, denominator / divisor)
        }
    }
}
