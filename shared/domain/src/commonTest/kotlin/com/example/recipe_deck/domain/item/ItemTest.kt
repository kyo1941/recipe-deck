package com.example.recipe_deck.domain.item

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ItemTest {
    @Test
    fun 表示名が空白だけのItemは作れない() {
        assertFailsWith<IllegalArgumentException> { Item(ItemId("item-1"), " ") }
    }
}
