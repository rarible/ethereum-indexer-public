package com.rarible.protocol.nft.core.repository

import com.rarible.protocol.nft.core.data.createRandomInconsistentItem
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.InconsistentItem
import com.rarible.protocol.nft.core.model.InconsistentItemContinuation
import com.rarible.protocol.nft.core.model.InconsistentItemContinuation.Companion.fromInconsistentItem
import com.rarible.protocol.nft.core.model.InconsistentItemFilterAll
import com.rarible.protocol.nft.core.model.InconsistentItemStatus
import com.rarible.protocol.nft.core.repository.inconsistentitem.InconsistentItemFilterCriteria.toCriteria
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.time.Instant

@IntegrationTest
class InconsistentItemRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var inconsistentItemRepository: InconsistentItemRepository

    @BeforeEach
    fun setupDbIndexes() = runBlocking {
        inconsistentItemRepository.createIndexes()
    }

    @Test
    fun `should save and return if was inserted`() = runBlocking<Unit> {
        // given
        val inconsistentItem = createRandomInconsistentItem()

        // when
        val inserted1 = inconsistentItemRepository.insert(inconsistentItem)
        val inserted2 = inconsistentItemRepository.insert(inconsistentItem)

        // then
        assertThat(inserted1).isTrue()
        assertThat(inserted2).isFalse()
    }

    @Test
    fun `should save and get by id`() = runBlocking<Unit> {
        // given
        val inconsistentItem = createRandomInconsistentItem()
        inconsistentItemRepository.insert(inconsistentItem)

        // when
        val actual = inconsistentItemRepository.get(inconsistentItem.id)

        // then
        assertThat(actual).isEqualTo(inconsistentItem)
    }

    @Test
    fun `should save if inconsistent item was fixed previously`() = runBlocking<Unit> {
        // given
        val before = createRandomInconsistentItem().copy(status = InconsistentItemStatus.FIXED)
        inconsistentItemRepository.insert(before)
        val after = before.copy(status = InconsistentItemStatus.UNFIXED)

        // when
        val saved = inconsistentItemRepository.insert(after)
        val actual = inconsistentItemRepository.get(after.id)

        // then
        assertThat(saved).isTrue()
        assertThat(actual).usingRecursiveComparison().ignoringFields("status", "relapseCount").isEqualTo(after)
        assertThat(actual!!.status).isEqualTo(InconsistentItemStatus.RELAPSED)
        assertThat(actual.relapseCount).isEqualTo(1)
    }

    @Test
    fun `should find by ids`() = runBlocking<Unit> {
        // given
        val item1 = createRandomInconsistentItem()
        val item2 = createRandomInconsistentItem()
        inconsistentItemRepository.insert(item1)
        inconsistentItemRepository.insert(item2)

        // when
        val actual = inconsistentItemRepository.searchByIds(setOf(item1.id, item2.id))

        // then
        assertThat(actual).containsExactlyInAnyOrder(item1, item2)
    }

    @Test
    fun `should search all with continuation`() = runBlocking<Unit> {
        // given
        val n1 = createRandomInconsistentItem().copy(
            lastUpdatedAt = Instant.ofEpochMilli(100),
            token = Address.TWO()
        )
        val n2 = createRandomInconsistentItem().copy(
            lastUpdatedAt = Instant.ofEpochMilli(100),
            token = Address.THREE()
        )
        val n3 = createRandomInconsistentItem().copy(
            lastUpdatedAt = Instant.ofEpochMilli(200),
            token = Address.ONE()
        )
        val n4 = createRandomInconsistentItem().copy(
            lastUpdatedAt = Instant.ofEpochMilli(200),
            token = Address.TWO()
        )

        listOf(n1, n2, n3, n4).shuffled().forEach { inconsistentItemRepository.insert(it) }

        // when
        var continuation: InconsistentItemContinuation? = null
        val totalResult = mutableListOf<InconsistentItem>()
        do {
            val query = InconsistentItemFilterAll().toCriteria(continuation = continuation, limit = 1)
            val result: List<InconsistentItem> = inconsistentItemRepository.search(query)
            totalResult.addAll(result)
            continuation = result.lastOrNull()?.fromInconsistentItem()
        } while (result.isNotEmpty())

        // then
        assertThat(totalResult).containsExactly(n1, n2, n3, n4)
    }
}
