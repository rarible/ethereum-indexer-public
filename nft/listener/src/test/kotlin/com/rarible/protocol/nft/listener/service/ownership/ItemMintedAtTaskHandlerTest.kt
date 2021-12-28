package com.rarible.protocol.nft.listener.service.ownership

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import com.rarible.protocol.nft.listener.service.item.ItemMintedAtTaskHandler
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import scalether.domain.AddressFactory

@IntegrationTest
@FlowPreview
class ItemMintedAtTaskHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var handler: ItemMintedAtTaskHandler

    @Autowired
    private lateinit var historyService: ItemReduceService

    @Test
    fun `should update mintedAt`() = runBlocking<Unit> {
        val token = AddressFactory.create()
        val tokenId = EthUInt256.ONE
        val itemId = ItemId(token, tokenId)
        val transfer = ItemTransfer(
            owner = AddressFactory.create(),
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.ONE
        )
        saveItemHistory(transfer)
        itemRepository.save(Item.empty(token, tokenId)).awaitFirstOrNull()
        val item = itemRepository.findById(itemId).awaitSingle()
        assertThat(item.mintedAt).isNull()

        handler.runLongTask(null, "").collect()
        val updated = itemRepository.findById(itemId).awaitSingle()
        assertThat(updated.mintedAt).isEqualTo(transfer.date)
    }

    suspend fun <T> saveItemHistory(
        data: T,
        token: Address = AddressFactory.create(),
        transactionHash: Word = WordFactory.create(),
        logIndex: Int? = null,
        status: LogEventStatus = LogEventStatus.CONFIRMED
    ): T {
        return nftItemHistoryRepository.save(
            LogEvent(
                data = data as EventData,
                address = token,
                topic = WordFactory.create(),
                transactionHash = transactionHash,
                status = status,
                index = 0,
                logIndex = logIndex,
                blockNumber = 1,
                minorLogIndex = 0
            )
        ).awaitFirst().data as T
    }
}

