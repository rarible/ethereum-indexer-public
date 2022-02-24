package com.rarible.protocol.order.core.repository.nonce

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.ext.MongoTest
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.configuration.RepositoryConfiguration
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ContextConfiguration

@MongoTest
@DataMongoTest
@ContextConfiguration(classes = [RepositoryConfiguration::class])
@EnableAutoConfiguration
internal class NonceHistoryRepositoryTest {

    @Autowired
    private lateinit var nonceHistoryRepository: NonceHistoryRepository

    @BeforeEach
    fun setup() = runBlocking {
        nonceHistoryRepository.createIndexes()
    }

    @Test
    fun `should get latest nonce history by user`() = runBlocking<Unit> {
        val maker1 = randomAddress()
        val maker2 = randomAddress()

        val nonce0 = ChangeNonceHistory(
            maker = maker1,
            newNonce = EthUInt256.of(1000),
            date = nowMillis()
        )
        val nonce1 = ChangeNonceHistory(
            maker = maker1,
            newNonce = EthUInt256.ONE,
            date = nowMillis()
        )
        val nonce2 = ChangeNonceHistory(
            maker = maker1,
            newNonce = EthUInt256.of(2),
            date = nowMillis()
        )
        val nonce3 = ChangeNonceHistory(
            maker = maker1,
            newNonce = EthUInt256.of(3),
            date = nowMillis()
        )
        val nonce4 = ChangeNonceHistory(
            maker = maker1,
            newNonce = EthUInt256.of(4),
            date = nowMillis()
        )
        val nonce5 = ChangeNonceHistory(
            maker = maker2,
            newNonce = EthUInt256.of(10),
            date = nowMillis()
        )
        val nonce6 = ChangeNonceHistory(
            maker = maker2,
            newNonce = EthUInt256.of(11),
            date = nowMillis()
        )
        saveLog(nonce0, blockNumber = 100, logIndex = 100, minorLogIndex = 100, status = LogEventStatus.REVERTED)
        saveLog(nonce1, blockNumber = 10, logIndex = 2, minorLogIndex = 2)
        saveLog(nonce2, blockNumber = 10, logIndex = 2, minorLogIndex = 1)
        saveLog(nonce3, blockNumber = 10, logIndex = 1, minorLogIndex = 2)
        saveLog(nonce4, blockNumber = 9, logIndex = 3, minorLogIndex = 3)
        saveLog(nonce5, blockNumber = 10, logIndex = 10, minorLogIndex = 10)
        saveLog(nonce6, blockNumber = 11, logIndex = 10, minorLogIndex = 10)

        Wait.waitAssert {
            val latestMaker1NonceLog = nonceHistoryRepository.findLatestNonceHistoryByMaker(maker1)
            assertThat(latestMaker1NonceLog?.data as ChangeNonceHistory).isEqualTo(nonce1)

            val latestMaker2NonceLog = nonceHistoryRepository.findLatestNonceHistoryByMaker(maker2)
            assertThat(latestMaker2NonceLog?.data as ChangeNonceHistory).isEqualTo(nonce6)
        }
    }

    private suspend fun saveLog(
        history: ChangeNonceHistory,
        blockNumber: Long,
        logIndex: Int,
        minorLogIndex: Int,
        status: LogEventStatus = LogEventStatus.CONFIRMED
    ) {
        nonceHistoryRepository.save(
            LogEvent(
                data = history,
                address = randomAddress(),
                topic = Word.apply(ByteArray(32)),
                transactionHash = Word.apply(randomWord()),
                status = status,
                blockNumber = blockNumber,
                logIndex = logIndex,
                minorLogIndex = minorLogIndex,
                index = 0,
            )
        )
    }
}
