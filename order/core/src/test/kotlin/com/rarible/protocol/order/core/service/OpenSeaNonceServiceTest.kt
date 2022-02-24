package com.rarible.protocol.order.core.service

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

@IntegrationTest
internal class OpenSeaNonceServiceTest : AbstractIntegrationTest() {
    @Autowired
    protected lateinit var openSeaNonceService: OpenSeaNonceService

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
            date = nowMillis() - Duration.ofMinutes(1)
        )
        val nonce2 = ChangeNonceHistory(
            maker = maker,
            newNonce = EthUInt256.of(2),
            date = nowMillis()
        )
        saveLog(nonce1, blockNumber = 10, logIndex = 2, minorLogIndex = 2)
        val logEvent2 = saveLog(nonce2, blockNumber = 11, logIndex = 2, minorLogIndex = 1)

        Wait.waitAssert {
            val makerNonce = openSeaNonceService.getLatestMakerNonce(maker)
            assertThat(makerNonce.nonce).isEqualTo(nonce2.newNonce)
            assertThat(makerNonce.timestamp).isEqualTo(nonce2.date)
            assertThat(makerNonce.historyId).isEqualTo(logEvent2.id.toHexString())
        }
    }

    private suspend fun saveLog(
        history: ChangeNonceHistory,
        blockNumber: Long,
        logIndex: Int,
        minorLogIndex: Int
    ) : LogEvent {
        return nonceHistoryRepository.save(
            LogEvent(
                data = history,
                address = randomAddress(),
                topic = Word.apply(ByteArray(32)),
                transactionHash = Word.apply(randomWord()),
                status = LogEventStatus.CONFIRMED,
                blockNumber = blockNumber,
                logIndex = logIndex,
                minorLogIndex = minorLogIndex,
                index = 0,
            )
        )
    }
}
