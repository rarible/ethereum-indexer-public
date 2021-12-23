package com.rarible.protocol.nft.core.repository.history

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemTransfer
import io.daonomic.rpc.domain.Word
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import scalether.domain.Address

@IntegrationTest
class HistoryRepositoryIt : AbstractIntegrationTest() {

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

    private fun word(): Word = Word.apply(RandomUtils.nextBytes(32))
}
