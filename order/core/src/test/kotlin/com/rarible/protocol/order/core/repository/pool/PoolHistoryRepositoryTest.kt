package com.rarible.protocol.order.core.repository.pool

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.TestPropertiesConfiguration
import com.rarible.protocol.order.core.configuration.RepositoryConfiguration
import com.rarible.protocol.order.core.data.randomPoolTargetNftIn
import com.rarible.protocol.order.core.data.randomPoolTargetNftOut
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.model.PoolNftChange
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@MongoTest
@DataMongoTest
@MongoCleanup
@EnableAutoConfiguration
@ContextConfiguration(classes = [RepositoryConfiguration::class, TestPropertiesConfiguration::class])
@ActiveProfiles("integration")
internal class PoolHistoryRepositoryTest {

    @Autowired
    private lateinit var poolHistoryRepository: PoolHistoryRepository

    @BeforeEach
    fun setup() = runBlocking {
        poolHistoryRepository.createIndexes()
    }

    @Test
    fun `get latest nft in change event for target item`() = runBlocking<Unit> {
        val collection = randomAddress()
        val tokenId = EthUInt256.of(randomBigInt())
        val tokenIds = (1..10).map { EthUInt256.of(randomBigInt()) } + tokenId

        save(
            history = randomPoolTargetNftIn().copy(collection = collection, tokenIds = tokenIds.shuffled()),
            blockNumber = 1,
            logIndex = 1,
            minorLogIndex = 0
        )
        save(
            history = randomPoolTargetNftOut().copy(collection = collection, tokenIds = tokenIds.shuffled()),
            blockNumber = 2,
            logIndex = 1,
            minorLogIndex = 0
        )
        save(
            history = randomPoolTargetNftIn().copy(collection = collection, tokenIds = tokenIds.shuffled()),
            blockNumber = 3,
            logIndex = 1,
            minorLogIndex = 0
        )
        save(
            history = randomPoolTargetNftIn().copy(collection = collection),
            blockNumber = 3,
            logIndex = 1,
            minorLogIndex = 0
        )
        val events = poolHistoryRepository.getLatestPoolNftChange(collection, tokenId)
        assertThat(events).hasSize(1)
        assertThat(events.single().blockNumber).isEqualTo(3)
        assertThat((events.single().data as PoolNftChange).tokenIds).contains(tokenId)
    }

    @Test
    fun `get latest nft out change event for target item`() = runBlocking<Unit> {
        val collection = randomAddress()
        val tokenId = EthUInt256.of(randomBigInt())
        val tokenIds = (1..10).map { EthUInt256.of(randomBigInt()) } + tokenId

        save(
            history = randomPoolTargetNftIn().copy(collection = collection, tokenIds = tokenIds.shuffled()),
            blockNumber = 1,
            logIndex = 1,
            minorLogIndex = 0
        )
        save(
            history = randomPoolTargetNftOut().copy(collection = collection, tokenIds = tokenIds.shuffled()),
            blockNumber = 2,
            logIndex = 1,
            minorLogIndex = 0
        )
        save(
            history = randomPoolTargetNftIn().copy(collection = collection, tokenIds = tokenIds.shuffled()),
            blockNumber = 3,
            logIndex = 1,
            minorLogIndex = 0
        )
        save(
            history = randomPoolTargetNftOut().copy(collection = collection, tokenIds = tokenIds.shuffled()),
            blockNumber = 3,
            logIndex = 1,
            minorLogIndex = 1
        )
        val events = poolHistoryRepository.getLatestPoolNftChange(collection, tokenId)
        assertThat(events).hasSize(1)
        assertThat(events.single().blockNumber).isEqualTo(3)
        assertThat(events.single().logIndex).isEqualTo(1)
        assertThat(events.single().minorLogIndex).isEqualTo(1)
        assertThat((events.single().data as PoolNftChange).tokenIds).contains(tokenId)
    }

    private suspend fun save(
        history: PoolHistory,
        blockNumber: Long = 0,
        logIndex: Int = 0,
        minorLogIndex: Int = 0
    ) {
        poolHistoryRepository.save(
            LogEvent(
                data = history,
                address = randomAddress(),
                topic = Word.apply(ByteArray(32)),
                transactionHash = Word.apply(randomWord()),
                status = LogEventStatus.CONFIRMED,
                blockNumber = blockNumber,
                logIndex = logIndex,
                minorLogIndex = minorLogIndex,
                index = 0
            )
        ).awaitFirst()
    }
}
