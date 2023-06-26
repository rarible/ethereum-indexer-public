package com.rarible.protocol.order.core.repository.pool

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.randomPoolNftWithdraw
import com.rarible.protocol.order.core.data.randomPoolSpotPriceUpdate
import com.rarible.protocol.order.core.data.randomPoolTargetNftIn
import com.rarible.protocol.order.core.data.randomPoolTargetNftOut
import com.rarible.protocol.order.core.data.randomSellOnChainAmmOrder
import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import com.rarible.protocol.order.core.model.PoolCreate
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.model.PoolHistoryType
import com.rarible.protocol.order.core.model.PoolNftChange
import com.rarible.protocol.order.core.model.PoolNftWithdraw
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@IntegrationTest
internal class PoolHistoryRepositoryTest : AbstractIntegrationTest() {

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
    fun `get latest nft in change event for item withdraw`() = runBlocking<Unit> {
        val collection = randomAddress()
        val tokenIdsOut = listOf(EthUInt256.of(randomBigInt()), EthUInt256.of(randomBigInt()))
        val tokenIds = (1..10).map { EthUInt256.of(randomBigInt()) } + tokenIdsOut

        save(
            history = randomSellOnChainAmmOrder().copy(collection = collection, tokenIds = tokenIds.shuffled()),
            blockNumber = 1,
            logIndex = 1,
            minorLogIndex = 0
        )
        save(
            history = randomPoolNftWithdraw().copy(collection = collection, tokenIds = tokenIdsOut.shuffled()),
            blockNumber = 2,
            logIndex = 1,
            minorLogIndex = 0
        )
        val events = poolHistoryRepository.getLatestPoolNftChange(collection, tokenIdsOut.first())
        assertThat(events).hasSize(1)
        assertThat(events.single().blockNumber).isEqualTo(2)
        assertThat((events.single().data as PoolNftWithdraw).tokenIds).containsExactlyInAnyOrderElementsOf(tokenIdsOut)
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

    @Test
    fun `get latest slot price change event for target pool`() = runBlocking<Unit> {
        val hash = Word.apply(randomWord())
        val priceUpdate1 = save(
            history = randomPoolSpotPriceUpdate().copy(hash = hash),
            blockNumber = 10,
            logIndex = 1
        )
        save(
            history = randomPoolTargetNftOut().copy(hash),
            blockNumber = 10,
            logIndex = 1
        )
        val priceUpdate2 = save(
            history = randomPoolSpotPriceUpdate().copy(hash),
            blockNumber = 9,
            logIndex = 2
        )
        val priceUpdate3 = save(
            history = randomPoolSpotPriceUpdate().copy(hash),
            blockNumber = 9,
            logIndex = 1
        )
        val priceUpdate4 = save(
            history = randomPoolSpotPriceUpdate().copy(hash),
            blockNumber = 3,
            logIndex = 1
        )
        var event = poolHistoryRepository.getLatestPoolEvent(hash, PoolHistoryType.POOL_SPOT_PRICE_UPDATE, 11, 2)
        assertThat(event?.id).isEqualTo(priceUpdate1.id)

        event = poolHistoryRepository.getLatestPoolEvent(hash, PoolHistoryType.POOL_SPOT_PRICE_UPDATE, 10, 2)
        assertThat(event?.id).isEqualTo(priceUpdate1.id)

        event = poolHistoryRepository.getLatestPoolEvent(hash, PoolHistoryType.POOL_SPOT_PRICE_UPDATE, 10, 1)
        assertThat(event?.id).isEqualTo(priceUpdate2.id)

        event = poolHistoryRepository.getLatestPoolEvent(hash, PoolHistoryType.POOL_SPOT_PRICE_UPDATE, 9, 10)
        assertThat(event?.id).isEqualTo(priceUpdate2.id)

        event = poolHistoryRepository.getLatestPoolEvent(hash, PoolHistoryType.POOL_SPOT_PRICE_UPDATE, 9, 2)
        assertThat(event?.id).isEqualTo(priceUpdate3.id)

        event = poolHistoryRepository.getLatestPoolEvent(hash, PoolHistoryType.POOL_SPOT_PRICE_UPDATE, 9, 0)
        assertThat(event?.id).isEqualTo(priceUpdate4.id)

        event = poolHistoryRepository.getLatestPoolEvent(hash, PoolHistoryType.POOL_SPOT_PRICE_UPDATE, 3, 2)
        assertThat(event?.id).isEqualTo(priceUpdate4.id)

        event = poolHistoryRepository.getLatestPoolEvent(hash, PoolHistoryType.POOL_SPOT_PRICE_UPDATE, 3, 0)
        assertThat(event).isNull()
    }

    @Test
    fun `get pool creat event`() = runBlocking<Unit> {
        val hash = Word.apply(randomWord())
        save(
            history = randomSellOnChainAmmOrder().copy(hash = hash),
            blockNumber = 1,
            logIndex = 1,
            minorLogIndex = 0
        )
        save(
            history = randomSellOnChainAmmOrder(),
            blockNumber = 2,
            logIndex = 1,
            minorLogIndex = 0
        )
        save(
            history = randomPoolTargetNftOut().copy(hash = hash),
            blockNumber = 2,
            logIndex = 1,
            minorLogIndex = 0
        )
        save(
            history = randomPoolTargetNftOut().copy(hash = hash),
            blockNumber = 3,
            logIndex = 1,
            minorLogIndex = 0
        )
        val event = poolHistoryRepository.getPoolCreateEvent(hash)
        assertThat(event).isNotNull
        assertThat(event?.data).isInstanceOf(PoolCreate::class.java)
        assertThat((event?.data as PoolCreate).hash).isEqualTo(hash)
    }

    @Test
    fun `should get all distinct hashes from pool history`() = runBlocking<Unit> {
        val hashes = listOf(Word.apply(randomWord()), Word.apply(randomWord()), Word.apply(randomWord()))
        (1..10).forEach { index ->
            for (hash in hashes) {
                save(
                    history = randomPoolTargetNftOut().copy(hash = hash),
                    blockNumber = 3,
                    logIndex = index,
                    minorLogIndex = 1
                )
            }
        }
        val distinct = poolHistoryRepository.findDistinctHashes().toList()
        assertThat(distinct).containsExactlyInAnyOrderElementsOf(hashes)
    }

    @Test
    fun `should get all distinct hashes from target hash`() = runBlocking<Unit> {
        val hash1 = Word.apply("0x0000000000000000000000000000000000000000000000000000000000000001")
        val hash2 = Word.apply("0x0000000000000000000000000000000000000000000000000000000000000002")
        val hashes = listOf(hash1, hash2)
        (1..10).forEach { index ->
            for (hash in hashes) {
                save(
                    history = randomPoolTargetNftOut().copy(hash = hash),
                    blockNumber = 3,
                    logIndex = index,
                    minorLogIndex = 1
                )
            }
        }
        val distinct = poolHistoryRepository.findDistinctHashes(from = hash1).toList()
        assertThat(distinct).containsExactlyInAnyOrder(hash2)
    }

    private suspend fun save(
        history: PoolHistory,
        blockNumber: Long = 0,
        logIndex: Int = 0,
        minorLogIndex: Int = 0
    ): ReversedEthereumLogRecord {
        return poolHistoryRepository.save(
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
                index = 0
            )
        ).awaitFirst()
    }
}
