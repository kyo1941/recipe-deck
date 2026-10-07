package com.example.recipe_deck.usecase.item

import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.item.ItemRepositoryFailure
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchItemsUseCaseTest {
    private val itemRepository = FakeItemRepository()
    private val searchItems = SearchItemsUseCase(itemRepository)

    private val groundMeat = Item(ItemId("item-1"), "ひき肉")
    private val porkBelly = Item(ItemId("item-2"), "豚バラ肉")
    private val tofu = Item(ItemId("item-3"), "豆腐")

    @Test
    fun 表示名に入力した文字を含むItemを返す() = runTest {
        itemRepository.savedItems += listOf(groundMeat, porkBelly, tofu)

        val result = searchItems("肉")

        assertEquals(SearchItemsResult.Found(persistentListOf(groundMeat, porkBelly)), result)
    }

    @Test
    fun 入力した文字の前後の空白を除いて探す() = runTest {
        itemRepository.savedItems += listOf(groundMeat, tofu)

        val result = searchItems("　豆腐 \n")

        assertEquals(SearchItemsResult.Found(persistentListOf(tofu)), result)
    }

    @Test
    fun 入力が空白だけなら何も返さない() = runTest {
        itemRepository.savedItems += listOf(groundMeat, tofu)

        val result = searchItems(" 　\n")

        assertEquals(SearchItemsResult.Found(persistentListOf()), result)
    }

    @Test
    fun 読み込みに失敗したら原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先から読み込めない")
        itemRepository.findFailure = ItemRepositoryFailure.Unexpected(cause)

        val result = searchItems("肉")

        assertEquals(SearchItemsResult.Unexpected(cause), result)
    }
}
