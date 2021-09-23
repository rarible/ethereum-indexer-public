package com.rarible.protocol.nft.core.service.token

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.contracts.collection.CreateEvent
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.CollectionEvent
import com.rarible.protocol.nft.core.model.CollectionOwnershipTransferred
import com.rarible.protocol.nft.core.model.CreateCollection
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import scalether.domain.AddressFactory

@IntegrationTest
class TokenReduceServiceTest : AbstractIntegrationTest() {
    @Test
    fun `should not change lastEventId`() = runBlocking<Unit> {
        val collectionId = randomAddress()
        prepareStorage(
            CreateCollection(
                collectionId,
                nowMillis(),
                randomAddress(),
                "Test",
                "TEST"
            )
        )
        val token = tokenReduceService.updateToken(collectionId)
        assertNotNull(token)
        assertNotNull(token!!.lastEventId)

        val sameToken = tokenReduceService.updateToken(collectionId)
        assertEquals(token.copy(version = 0), sameToken?.copy(version = 0))
    }

    @Test
    fun `should change lastEventId is a new event is added`() = runBlocking<Unit> {
        val collectionId = randomAddress()
        val previousOwner = randomAddress()
        prepareStorage(
            CreateCollection(
                collectionId,
                nowMillis(),
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
                nowMillis(),
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
                    createdAt = event.date,
                    updatedAt = event.date
                )
            ).awaitFirst()
        }
    }

}
