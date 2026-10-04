package com.example.recipe_deck.domain.amount

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MultiplierTest {
    @Test
    fun 倍率0は作れない() {
        assertFailsWith<IllegalArgumentException> { Multiplier.of(0) }
    }

    @Test
    fun 負の倍率は作れない() {
        assertFailsWith<IllegalArgumentException> { Multiplier.of(-1, 2) }
    }

    @Test
    fun 分数の倍率を数量に誤差なく掛けられる() {
        assertEquals(Quantity.of(1), Quantity.of(3) * Multiplier.of(1, 3))
    }

    @Test
    fun 倍率も分母を省略すると整数になる() {
        assertEquals(Multiplier.of(2, 1), Multiplier.of(2))
    }

    @Test
    fun 倍率も約分された値として扱われる() {
        val multiplier = Multiplier.of(2, 4)

        assertEquals(1, multiplier.numerator)
        assertEquals(2, multiplier.denominator)
        assertEquals(Multiplier.of(1, 2), multiplier)
    }

    @Test
    fun 倍率も分母0は作れない() {
        assertFailsWith<IllegalArgumentException> { Multiplier.of(1, 0) }
    }

    @Test
    fun 倍率を掛けた積がLongの範囲を超えると範囲超過として失敗する() {
        assertFailsWith<ArithmeticException> { Quantity.of(1, Long.MAX_VALUE) * Multiplier.of(1, 2) }
    }
}
