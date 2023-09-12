package com.rarible.protocol.nft.core.repository.history

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.nft.core.data.randomReversedLogRecord
import com.rarible.protocol.nft.core.data.randomRoyaltiesHistory
import com.rarible.protocol.nft.core.model.RoyaltiesEventType
import com.rarible.protocol.nft.core.test.AbstractIntegrationTest
import com.rarible.protocol.nft.core.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class RoyaltiesHistoryRepositoryTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var royaltiesHistoryRepository: RoyaltiesHistoryRepository

    @BeforeEach
    fun setupIndexes() = runBlocking<Unit> {
        royaltiesHistoryRepository.createIndexes()
    }

    @Test
    fun `save and get latest`() = runBlocking<Unit> {
        val token = randomAddress()
        val history1 = randomRoyaltiesHistory(token)
        val history2 = randomRoyaltiesHistory(token)

        val log1 = randomReversedLogRecord(history1).copy(blockNumber = 1)
        val log2 = randomReversedLogRecord(history2).copy(blockNumber = 2)

        royaltiesHistoryRepository.save(log1)
        royaltiesHistoryRepository.save(log2)

        val lastHistory = royaltiesHistoryRepository.findLastByCollection(token, RoyaltiesEventType.SET_CONTRACT_ROYALTIES)
        assertThat(lastHistory?.data).isEqualTo(history2)
    }
}
