package com.example.recipe_deck.domain.amount

@ConsistentCopyVisibility
data class Quantity private constructor(
    val numerator: Long,
    val denominator: Long,
) {
    operator fun plus(other: Quantity): Quantity = of(
        numerator = numerator.timesExact(other.denominator).plusExact(other.numerator.timesExact(denominator)),
        denominator = denominator.timesExact(other.denominator),
    )

    operator fun times(multiplier: Multiplier): Quantity = of(
        numerator = numerator.timesExact(multiplier.numerator),
        denominator = denominator.timesExact(multiplier.denominator),
    )

    companion object {
        fun of(numerator: Long, denominator: Long = 1): Quantity {
            require(numerator > 0) { "数量は正の値でなければならない: $numerator/$denominator" }
            require(denominator > 0) { "分母は正の値でなければならない: $numerator/$denominator" }
            val divisor = gcd(numerator, denominator)
            return Quantity(numerator / divisor, denominator / divisor)
        }
    }
}
