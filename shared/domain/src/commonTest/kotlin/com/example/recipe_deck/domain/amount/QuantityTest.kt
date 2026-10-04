package com.example.recipe_deck.domain.amount

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QuantityTest {
    @Test
    fun 約分された値として扱われる() {
        val quantity = Quantity.of(2, 4)

        assertEquals(1, quantity.numerator)
        assertEquals(2, quantity.denominator)
        assertEquals(Quantity.of(1, 2), quantity)
    }

    @Test
    fun 分母を省略すると整数になる() {
        assertEquals(Quantity.of(200, 1), Quantity.of(200))
    }

    @Test
    fun 分数同士を誤差なく足せる() {
        assertEquals(Quantity.of(5, 6), Quantity.of(1, 2) + Quantity.of(1, 3))
    }

    @Test
    fun 足した結果も約分される() {
        assertEquals(Quantity.of(1), Quantity.of(1, 2) + Quantity.of(1, 2))
    }

    @Test
    fun 数量0は作れない() {
        assertFailsWith<IllegalArgumentException> { Quantity.of(0) }
    }

    @Test
    fun 負の数量は作れない() {
        assertFailsWith<IllegalArgumentException> { Quantity.of(-1, 2) }
        assertFailsWith<IllegalArgumentException> { Quantity.of(1, -2) }
    }

    @Test
    fun 分母0は作れない() {
        assertFailsWith<IllegalArgumentException> { Quantity.of(1, 0) }
    }

    @Test
    fun 積がLongの範囲を超えると範囲超過として失敗する() {
        val huge = Quantity.of(1, Long.MAX_VALUE)

        assertFailsWith<ArithmeticException> { huge + Quantity.of(1, Long.MAX_VALUE - 1) }
    }

    @Test
    fun 和がLongの範囲を超えると範囲超過として失敗する() {
        assertFailsWith<ArithmeticException> { Quantity.of(Long.MAX_VALUE) + Quantity.of(Long.MAX_VALUE) }
    }
}
