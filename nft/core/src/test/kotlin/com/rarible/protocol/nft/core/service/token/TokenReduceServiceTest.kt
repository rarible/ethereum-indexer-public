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
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.model.TokenStandard
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono
import scalether.domain.AddressFactory

@IntegrationTest
class TokenReduceServiceTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var tokenRegistrationService: TokenRegistrationService

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `change owner for a token registered via service`(version: ReduceVersion) = withReducer(version) {
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
                blockNumber = 1,
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
            Token::version.name,
            Token::revertableEvents.name,
        )
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `return token registered with via service having no log events`(version: ReduceVersion) = withReducer(version) {
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

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `should not change lastEventId`(version: ReduceVersion) = withReducer(version) {
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

        when (version) {
            ReduceVersion.V1 -> assertNotNull(token!!.lastEventId)
            ReduceVersion.V2 -> {
                assertThat(token!!.revertableEvents).hasSize(1)
                assertThat(token.revertableEvents.single()).isInstanceOf(TokenEvent.TokenCreateEvent::class.java)
            }
        }
        val sameToken = tokenReduceService.updateToken(collectionId)
        assertEquals(token.copy(version = 0), sameToken?.copy(version = 0))
    }

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun `should change lastEventId if a new event is added`(version: ReduceVersion) = withReducer(version) {
        val collectionId = randomAddress()
        val previousOwner = randomAddress()
        prepareStorage(
            CreateCollection(
                collectionId,
                previousOwner,
                "Test",
                "TEST"
            ),
            blockNumber = 1
        )
        val token = tokenReduceService.updateToken(collectionId)
        assertNotNull(token)
        when (version) {
            ReduceVersion.V1 -> assertNotNull(token!!.lastEventId)
            ReduceVersion.V2 -> {
                assertThat(token!!.revertableEvents).hasSize(1)
                assertThat(token.revertableEvents.single()).isInstanceOf(TokenEvent.TokenCreateEvent::class.java)
            }
        }

        val newOwner = randomAddress()
        prepareStorage(
            CollectionOwnershipTransferred(
                collectionId,
                previousOwner,
                newOwner
            ),
            blockNumber = 2
        )

        val withUpdatedOwner = tokenReduceService.updateToken(collectionId)
        println("NEW $withUpdatedOwner")
        println("OWN $newOwner")

        assertNotNull(withUpdatedOwner)
        assertEquals(newOwner, withUpdatedOwner!!.owner)
        when (version) {
            ReduceVersion.V1 -> assertNotEquals(token.lastEventId, withUpdatedOwner.lastEventId)
            ReduceVersion.V2 -> {
                assertThat(withUpdatedOwner.revertableEvents).hasSize(2)
                assertThat(withUpdatedOwner.revertableEvents[0]).isInstanceOf(TokenEvent.TokenCreateEvent::class.java)
                assertThat(withUpdatedOwner.revertableEvents[1]).isInstanceOf(TokenEvent.TokenChangeOwnershipEvent::class.java)
            }
        }
    }

    private suspend fun prepareStorage(
        vararg events: CollectionEvent,
        blockNumber: Long = System.currentTimeMillis()
    ) {
        events.forEach { event ->
            tokenHistoryRepository.save(
                LogEvent(
                    data = event,
                    address = AddressFactory.create(),
                    topic = if (event is CreateCollection) CreateEvent.id() else Word.apply(randomWord()),
                    transactionHash = Word.apply(randomWord()),
                    status = LogEventStatus.CONFIRMED,
                    blockNumber = blockNumber,
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
