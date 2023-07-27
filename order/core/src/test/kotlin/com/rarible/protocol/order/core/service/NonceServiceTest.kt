package com.rarible.protocol.order.core.service

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.model.HistorySource
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

@IntegrationTest
internal class NonceServiceTest : AbstractIntegrationTest() {
    @Autowired
    protected lateinit var nonceService: NonceService

    @BeforeEach
    fun setup() = runBlocking {
        nonceHistoryRepository.createIndexes()
    }

    @Test
    fun `should get latest nonce history by user`() = runBlocking<Unit> {
        val maker = randomAddress()

        val nonce1 = ChangeNonceHistory(
            maker = maker,
            newNonce = EthUInt256.ONE,
            date = nowMillis() - Duration.ofMinutes(1),
            source = HistorySource.OPEN_SEA
        )
        val nonce2 = ChangeNonceHistory(
            maker = maker,
            newNonce = EthUInt256.of(2),
            date = nowMillis(),
            source = HistorySource.OPEN_SEA
        )
        saveLog(nonce1, blockNumber = 10, logIndex = 2, minorLogIndex = 2)
        val logEvent2 = saveLog(nonce2, blockNumber = 11, logIndex = 2, minorLogIndex = 1)

        Wait.waitAssert {
            val makerNonce = nonceService.getLatestMakerNonce(maker, logEvent2.address)
            assertThat(makerNonce.nonce).isEqualTo(nonce2.newNonce)
            assertThat(makerNonce.timestamp).isEqualTo(nonce2.date)
            assertThat(makerNonce.historyId).isEqualTo(logEvent2.id)
        }
    }

    private suspend fun saveLog(
        history: ChangeNonceHistory,
        blockNumber: Long,
        logIndex: Int,
        minorLogIndex: Int
    ): ReversedEthereumLogRecord {
        return nonceHistoryRepository.save(
            ReversedEthereumLogRecord(
                id = ObjectId().toHexString(),
                data = history,
                address = randomAddress(),
                topic = Word.apply(ByteArray(32)),
                transactionHash = randomWord(),
                status = EthereumBlockStatus.CONFIRMED,
                blockNumber = blockNumber,
                logIndex = logIndex,
                minorLogIndex = minorLogIndex,
                index = 0,
            )
        )
    }
}
