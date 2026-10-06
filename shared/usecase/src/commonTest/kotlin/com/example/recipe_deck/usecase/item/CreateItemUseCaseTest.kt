package com.example.recipe_deck.usecase.item

import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.item.ItemRepositoryFailure
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateItemUseCaseTest {
    private val itemRepository = FakeItemRepository()
    private val createItem = CreateItemUseCase(itemRepository)

    private val groundMeat = Item(ItemId("item-1"), "ひき肉")

    @Test
    fun 指定された表示名でItemを保存する() = runTest {
        val result = createItem("豆腐")

        val saved = itemRepository.savedItems.single()
        assertEquals(CreateItemResult.Created(saved.id), result)
        assertEquals("豆腐", saved.displayName)
    }

    @Test
    fun 表示名の前後の空白を除いて保存する() = runTest {
        createItem("　豆腐 \n")

        assertEquals("豆腐", itemRepository.savedItems.single().displayName)
    }

    @Test
    fun 表示名が空白だけなら保存せずに失敗を返す() = runTest {
        val result = createItem(" 　\n")

        assertEquals(CreateItemResult.DisplayNameRequired, result)
        assertEquals(emptyList(), itemRepository.savedItems)
    }

    @Test
    fun 前後の空白を除いた表示名が同じItemがあれば保存せずに既存のItemを返す() = runTest {
        itemRepository.savedItems += groundMeat

        val result = createItem(" ひき肉　")

        assertEquals(CreateItemResult.AlreadyExists(groundMeat), result)
        assertEquals(listOf(groundMeat), itemRepository.savedItems)
    }

    @Test
    fun 表示名の一部だけが同じItemしかなければ保存する() = runTest {
        itemRepository.savedItems += groundMeat

        val result = createItem("肉")

        val saved = itemRepository.savedItems.last()
        assertEquals(CreateItemResult.Created(saved.id), result)
        assertEquals("肉", saved.displayName)
    }

    @Test
    fun 読み込みに失敗したら保存せずに原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先から読み込めない")
        itemRepository.findFailure = ItemRepositoryFailure.Unexpected(cause)

        val result = createItem("豆腐")

        assertEquals(CreateItemResult.Unexpected(cause), result)
        assertEquals(emptyList(), itemRepository.savedItems)
    }

    @Test
    fun 保存に失敗したら原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先に書き込めない")
        itemRepository.saveFailure = ItemRepositoryFailure.Unexpected(cause)

        val result = createItem("豆腐")

        assertEquals(CreateItemResult.Unexpected(cause), result)
    }
}
