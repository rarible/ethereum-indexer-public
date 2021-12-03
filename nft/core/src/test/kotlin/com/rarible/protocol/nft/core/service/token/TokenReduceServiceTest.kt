package com.rarible.protocol.nft.core.service.token

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.contracts.collection.CreateEvent
import com.rarible.protocol.contracts.erc721.OwnershipTransferredEvent
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.CollectionEvent
import com.rarible.protocol.nft.core.model.CollectionOwnershipTransferred
import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono
import scalether.domain.AddressFactory

@IntegrationTest
class TokenReduceServiceTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var tokenRegistrationService: TokenRegistrationService

    @Test
    fun `change owner for a token registered via service`() = runBlocking<Unit> {
        val id = randomAddress()
        val previousOwner = randomAddress()
        val newOwner = randomAddress()
        tokenRegistrationService.getOrSaveToken(id) {
            Token(
                id = id,
                owner = previousOwner,
                name = "Name",
                symbol = "Symbol",
                standard = TokenStandard.ERC721
            ).toMono()
        }.awaitFirst()

        tokenHistoryRepository.save(
            LogEvent(
                CollectionOwnershipTransferred(
                    id = id,
                    previousOwner = previousOwner,
                    newOwner = newOwner
                ),
                address = id,
                topic = OwnershipTransferredEvent.id(),
                transactionHash = Word.apply(randomWord()),
                status = LogEventStatus.CONFIRMED,
                logIndex = 0,
                minorLogIndex = 0,
                index = 0
            )
        ).awaitFirst()

        val updated = tokenReduceService.updateToken(id)
        assertThat(updated).isEqualToIgnoringGivenFields(
            Token(
                id = id,
                owner = newOwner,
                name = "Name",
                symbol = "Symbol",
                standard = TokenStandard.ERC721
            ),
            Token::lastEventId.name,
            Token::version.name
        )
    }

    @Test
    fun `return token registered with via service having no log events`() = runBlocking<Unit> {
        val id = randomAddress()
        val token = Token(
            id = id,
            owner = randomAddress(),
            name = "Name",
            symbol = "Symbol",
            standard = TokenStandard.ERC721
        )
        tokenRegistrationService.getOrSaveToken(id) { token.toMono() }.awaitFirst()
        val updated = tokenReduceService.updateToken(id)
        assertThat(updated).isEqualTo(updated)
    }

    @Test
    fun `should not change lastEventId`() = runBlocking<Unit> {
        val collectionId = randomAddress()
        prepareStorage(
            CreateCollection(
                id = collectionId,
                owner = randomAddress(),
                name = "Test",
                symbol = "TEST"
            )
        )
        val token = tokenReduceService.updateToken(collectionId)
        assertNotNull(token)
        assertNotNull(token!!.lastEventId)

        val sameToken = tokenReduceService.updateToken(collectionId)
        assertEquals(token.copy(version = 0), sameToken?.copy(version = 0))
    }

    @Test
    fun `should change lastEventId if a new event is added`() = runBlocking<Unit> {
        val collectionId = randomAddress()
        val previousOwner = randomAddress()
        prepareStorage(
            CreateCollection(
                collectionId,
                previousOwner,
                "Test",
                "TEST"
            )
        )
        val token = tokenReduceService.updateToken(collectionId)
        assertNotNull(token)
        assertNotNull(token!!.lastEventId)

        val newOwner = randomAddress()
        prepareStorage(
            CollectionOwnershipTransferred(
                collectionId,
                previousOwner,
                newOwner
            )
        )

        val withUpdatedOwner = tokenReduceService.updateToken(collectionId)
        println("NEW $withUpdatedOwner")
        println("OWN $newOwner")

        assertNotNull(withUpdatedOwner)
        assertEquals(newOwner, withUpdatedOwner!!.owner)
        assertNotEquals(token.lastEventId, withUpdatedOwner.lastEventId)
    }

    private suspend fun prepareStorage(vararg events: CollectionEvent) {
        events.forEach { event ->
            tokenHistoryRepository.save(
                LogEvent(
                    data = event,
                    address = AddressFactory.create(),
                    topic = if (event is CreateCollection) CreateEvent.id() else Word.apply(randomWord()),
                    transactionHash = Word.apply(randomWord()),
                    status = LogEventStatus.CONFIRMED,
                    blockNumber = System.currentTimeMillis(),
                    logIndex = 0,
                    minorLogIndex = 0,
                    index = 0,
                    createdAt = nowMillis(),
                    updatedAt = nowMillis()
                )
            ).awaitFirst()
        }
    }

}
