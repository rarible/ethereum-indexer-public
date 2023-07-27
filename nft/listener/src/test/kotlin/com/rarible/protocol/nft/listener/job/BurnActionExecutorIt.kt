package com.rarible.protocol.nft.listener.job

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.EnsDomainService
import com.rarible.protocol.nft.core.service.action.ActionJobHandler
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import com.rarible.protocol.nft.listener.test.data.createRandomItemProperties
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.time.Instant

@IntegrationTest
internal class BurnActionExecutorIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var endDoomainService: EnsDomainService

    @Autowired
    private lateinit var reduceService: ItemReduceService

    @Autowired
    private lateinit var handler: ActionJobHandler

    @Test
    @Disabled
    fun `should execute burn action for ens domain`() = runBlocking<Unit> {
        val expirationDate = "1970-04-01T17:26:15Z"

        val owner = AddressFactory.create()
        val token = AddressFactory.create()
        val tokenId = EthUInt256.ONE
        val itemId = ItemId(token, tokenId)

        saveToken(
            Token(token, name = "TEST", standard = TokenStandard.ERC721)
        )
        val transfer = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
            value = EthUInt256.ONE
        )
        saveItemHistory(transfer)
        reduceService.update(token = token, tokenId = tokenId).awaitFirst()

        val savedItem = itemRepository.findById(itemId).awaitFirst()
        assertThat(savedItem.deleted).isFalse()

        val properties = createRandomItemProperties().copy(
            attributes = listOf(
                ItemAttribute(EnsDomainService.EXPIRATION_DATE_PROPERTY, expirationDate)
            )
        )
        endDoomainService.onGetProperties(itemId, properties)
        handler.handle()

        Wait.waitAssert {
            val updatedItem = itemRepository.findById(itemId).awaitFirst()
            assertThat(updatedItem.deleted).isTrue()
        }
    }

    suspend fun saveItemHistory(data: ItemHistory): ItemHistory {
        return nftItemHistoryRepository.save(
            LogEvent(
                data = data as EventData,
                address = data.token,
                topic = WordFactory.create(),
                transactionHash = Word.apply(randomWord()),
                status = LogEventStatus.CONFIRMED,
                from = randomAddress(),
                index = 0,
                logIndex = 0,
                blockNumber = 1,
                minorLogIndex = 0,
                blockTimestamp = Instant.now().epochSecond
            )
        ).awaitFirst().data as ItemHistory
    }

    private suspend fun saveToken(token: Token) {
        tokenRepository.save(token).awaitFirst()
    }
}
