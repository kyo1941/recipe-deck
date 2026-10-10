package com.example.recipe_deck.usecase.item

import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.item.ItemReference
import kotlin.test.Test
import kotlin.test.assertEquals

class LinkToExistingItemTest {
    private val groundMeat = Item(ItemId("item-1"), "ひき肉")
    private val existingItems = listOf(groundMeat).associateBy { it.displayName }

    @Test
    fun 前後の空白を除いた表示名が既存のItemと完全に一致したら既存のItemの参照にする() {
        val linked = ItemReference.Unregistered("　ひき肉 \n").linkToExistingItem(existingItems)

        assertEquals(ItemReference.Registered(groundMeat.id), linked)
    }

    @Test
    fun 一致する既存のItemが無ければ前後の空白を除いた表示名のまま返す() {
        val linked = ItemReference.Unregistered(" 豚ひき肉 ").linkToExistingItem(existingItems)

        assertEquals(ItemReference.Unregistered("豚ひき肉"), linked)
    }
}
