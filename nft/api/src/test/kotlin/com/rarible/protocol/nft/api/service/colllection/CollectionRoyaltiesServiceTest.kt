package com.rarible.protocol.nft.api.service.colllection

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.nft.core.data.randomReversedLogRecord
import com.rarible.protocol.nft.core.data.randomRoyaltiesHistory
import com.rarible.protocol.nft.core.model.RoyaltiesEventType
import com.rarible.protocol.nft.core.repository.history.RoyaltiesHistoryRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CollectionRoyaltiesServiceTest {
    private val repository = mockk<RoyaltiesHistoryRepository>()
    private val service = CollectionRoyaltiesService(repository)

    @Test
    fun `get latest`() = runBlocking<Unit> {
        val token = randomAddress()
        val history = randomRoyaltiesHistory(token)
        val log = randomReversedLogRecord(history)

        coEvery { repository.findLastByCollection(token, RoyaltiesEventType.SET_CONTRACT_ROYALTIES) } returns log

        val parts = service.getRoyaltiesHistory(token)
        assertThat(parts).isEqualTo(history.parts)
    }
}
