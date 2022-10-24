package com.rarible.protocol.nft.core.repository

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomInconsistentItem
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.InconsistentItem
import com.rarible.protocol.nft.core.model.InconsistentItemStatus
import com.rarible.protocol.nft.core.model.ItemProblemType
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger

@IntegrationTest
class InconsistentItemRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var inconsistentItemRepository: InconsistentItemRepository

    @Test
    fun `should save and return if was inserted`() = runBlocking<Unit> {
        // given
        val inconsistentItem = createRandomInconsistentItem()

        // when
        val inserted1 = inconsistentItemRepository.save(inconsistentItem)
        val inserted2 = inconsistentItemRepository.save(inconsistentItem)

        // then
        assertThat(inserted1).isTrue()
        assertThat(inserted2).isFalse()
    }

    @Test
    fun `should save and get by id`() = runBlocking<Unit> {
        // given
        val inconsistentItem = createRandomInconsistentItem()
        inconsistentItemRepository.save(inconsistentItem)

        // when
        val actual = inconsistentItemRepository.get(inconsistentItem.id)

        // then
        assertThat(actual).isEqualTo(inconsistentItem)
    }

    @Test
    fun `should find by ids`() = runBlocking<Unit> {
        // given
        val item1 = createRandomInconsistentItem()
        val item2 = createRandomInconsistentItem()
        inconsistentItemRepository.save(item1)
        inconsistentItemRepository.save(item2)

        // when
        val actual = inconsistentItemRepository.searchByIds(setOf(item1.id, item2.id))

        // then
        assertThat(actual).containsExactlyInAnyOrder(item1, item2)
    }
}