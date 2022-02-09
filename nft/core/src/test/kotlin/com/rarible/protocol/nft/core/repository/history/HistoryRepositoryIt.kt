package com.rarible.protocol.nft.core.repository.history

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.*
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemTransfer
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import scalether.domain.Address

@IntegrationTest
class HistoryRepositoryIt : AbstractIntegrationTest() {

    @BeforeEach
    fun setupIndexes() = runBlocking<Unit> {
        nftItemHistoryRepository.createIndexes()
    }

    @Test
    fun getHistory() {
        val owner = Address.apply(RandomUtils.nextBytes(20))
        val token = Address.ZERO()
        val token2 = Address.ONE()

        val txHash = Word.apply(RandomUtils.nextBytes(32))
        val transfer1 = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = EthUInt256.ONE,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.of(1)
        )
        val transfer2 = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = EthUInt256.of(2),
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.of(1)
        )
        val transfer3 = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = EthUInt256.TEN,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.of(1)
        )
        val transfer4 = ItemTransfer(
            owner = owner,
            token = token2,
            tokenId = EthUInt256.TEN,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.of(1)
        )
        nftItemHistoryRepository.save(
            LogEvent(
                data = transfer1,
                address = token,
                topic = word(),
                transactionHash = word(),
                status = LogEventStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 0,
                index = 0,
                minorLogIndex = 0
            )
        ).block()
        nftItemHistoryRepository.save(
            LogEvent(
                data = transfer2,
                address = token,
                topic = word(),
                transactionHash = txHash,
                status = LogEventStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 1,
                index = 0,
                minorLogIndex = 0
            )
        ).block()
        nftItemHistoryRepository.save(
            LogEvent(
                data = transfer3,
                address = token,
                topic = word(),
                transactionHash = word(),
                status = LogEventStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 3,
                index = 0,
                minorLogIndex = 0
            )
        ).block()
        nftItemHistoryRepository.save(
            LogEvent(
                data = transfer4,
                address = token,
                topic = word(),
                transactionHash = word(),
                status = LogEventStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 0,
                index = 0,
                minorLogIndex = 0
            )
        ).block()

        val logs = nftItemHistoryRepository.findItemsHistory(token).collectList().block()!!
        assertEquals(logs.size, 3)
        assertEquals(logs[0].item, transfer1)
        assertEquals(logs[1].item, transfer2)
        assertEquals(logs[2].item, transfer3)

        val logsWithFrom =
            nftItemHistoryRepository.findItemsHistory(token, null, ItemId(token, EthUInt256.of(2))).collectList()
                .block()!!
        assertEquals(logsWithFrom.size, 1)
        assertEquals(logsWithFrom[0].item, transfer3)

        val logsWithFromForAll =
            nftItemHistoryRepository.findItemsHistory(from = ItemId(token, EthUInt256.of(2))).collectList().block()!!
        assertEquals(logsWithFromForAll.size, 2)
        assertEquals(logsWithFromForAll[0].item, transfer3)
        assertEquals(logsWithFromForAll[1].item, transfer4)

    }

    @Test
    fun `should get history from to to`() = runBlocking<Unit> {
        val minTokenId = EthUInt256.ZERO
        val maxTokenId = EthUInt256.of("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")

        val token0Min = Address.apply("0x0000000000000000000000000000000000000000")
        val token0Max = Address.apply("0x0fffffffffffffffffffffffffffffffffffffff")

        val token1Min = Address.apply("0x1000000000000000000000000000000000000000")
        val token11 = Address.apply("0x1000000000000000000000000000000000010000")
        val token12 = Address.apply("0x1f00000000000000000000000000000000000000")
        val token1Max = Address.apply("0x1fffffffffffffffffffffffffffffffffffffff")

        val token2Min = Address.apply("0x2000000000000000000000000000000000000000")
        val token7Max = Address.apply("0x2fffffffffffffffffffffffffffffffffffffff")

        listOf(
            token0Min,
            token0Max,
            token1Min,
            token11,
            token12,
            token1Max,
            token2Min,
            token7Max
        ).map { createLogEvent(token = it) }.forEach { nftItemHistoryRepository.save(it).awaitFirst() }

        Wait.waitAssert {
            val logs = nftItemHistoryRepository
                .findItemsHistory(from = ItemId(token0Max, maxTokenId), to = ItemId(token2Min, minTokenId))
                .collectList().awaitFirst()
            assertThat(logs.map { it.item.token }).containsExactly(token1Min, token11, token12, token1Max)
        }
    }

    @Test
    fun `should get history from to to for tokenId`() = runBlocking<Unit> {
        val minTokenId = EthUInt256.ZERO
        val maxTokenId = EthUInt256.of("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")

        val token0Max = Address.apply("0x0fffffffffffffffffffffffffffffffffffffff")
        val token1Min = Address.apply("0x1000000000000000000000000000000000000000")
        val token1Max = Address.apply("0x1fffffffffffffffffffffffffffffffffffffff")
        val token2Min = Address.apply("0x2000000000000000000000000000000000000000")

        val item01 = ItemId(token0Max, minTokenId)
        val item02 = ItemId(token0Max, maxTokenId)
        val item10 = ItemId(token1Min, minTokenId)
        val item11 = ItemId(token1Min, maxTokenId)
        val item12 = ItemId(token1Max, minTokenId)
        val item13 = ItemId(token1Max, maxTokenId)
        val item21 = ItemId(token2Min, minTokenId)
        val item22 = ItemId(token2Min, maxTokenId)

        listOf(
            item01,
            item02,
            item10,
            item11,
            item12,
            item13,
            item21,
            item22
        ).map { createLogEvent(token = it.token, tokenId = it.tokenId) }.forEach { nftItemHistoryRepository.save(it).awaitFirst() }

        Wait.waitAssert {
            val logs = nftItemHistoryRepository
                .findItemsHistory(from = ItemId(token0Max, maxTokenId), to = ItemId(token2Min, minTokenId))
                .collectList().awaitFirst()
            assertThat(logs.map { ItemId(it.item.token, it.item.tokenId) }).containsExactly(item10, item11, item12, item13)
        }
    }

    private fun createLogEvent(token: Address, tokenId: EthUInt256 = EthUInt256.of(randomBigInt())): LogEvent {
        val transfer1 = ItemTransfer(
            owner = randomAddress(),
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.of(1)
        )
        return LogEvent(
            data = transfer1,
            address = token,
            topic = word(),
            transactionHash = Word.apply(randomWord()),
            status = LogEventStatus.CONFIRMED,
            blockNumber = randomLong(),
            logIndex = randomInt(),
            index = 0,
            minorLogIndex = 0
        )
    }

    private fun word(): Word = Word.apply(RandomUtils.nextBytes(32))
}
