package com.rarible.protocol.nft.listener.service.ownership

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.contracts.erc721.rarible.user.CreateERC721RaribleUserEvent
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import scalether.domain.Address
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

@IntegrationTest
class RemoveIrrelevantTaskHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var handler: RemoveIrrelevantTaskHandler

    @Test
    fun `should remove ownership`() = runBlocking {
        val owner = ownership()
        ownershipRepository.save(owner).awaitFirstOrNull()
        assertNotNull(ownershipRepository.findById(owner.id).awaitFirstOrNull())

        handler.runLongTask(null, "").collect()
        assertNull(ownershipRepository.findById(owner.id).awaitFirstOrNull())
    }

    @Test
    fun `shouldn't remove lazy ownership`() = runBlocking {
        val owner = ownership().copy(lazyValue = EthUInt256.ONE)
        ownershipRepository.save(owner).awaitFirstOrNull()
        assertNotNull(ownershipRepository.findById(owner.id).awaitFirstOrNull())

        handler.runLongTask(null, "").collect()
        assertNotNull(ownershipRepository.findById(owner.id).awaitFirstOrNull())
    }

    @Test
    fun `shouldn't remove ownership if tx is existed`() = runBlocking {
        val owner = ownership()
        ownershipRepository.save(owner).awaitFirstOrNull()
        val log = logEvent(owner.token, owner.tokenId, owner.owner)
        nftItemHistoryRepository.save(log).awaitFirstOrNull()
        assertNotNull(ownershipRepository.findById(owner.id).awaitFirstOrNull())

        handler.runLongTask(null, "").collect()
        assertNotNull(ownershipRepository.findById(owner.id).awaitFirstOrNull())
    }

    @Test
    fun `shouldn't remove ownership with pending`() = runBlocking {
        val owner = ownership().copy(pending = listOf(itemTransfer()))
        ownershipRepository.save(owner).awaitFirstOrNull()
        assertNotNull(ownershipRepository.findById(owner.id).awaitFirstOrNull())

        handler.runLongTask(null, "").collect()
        assertNotNull(ownershipRepository.findById(owner.id).awaitFirstOrNull())
    }

    @Test
    fun `should remove old ownerships without lazy field`() = runBlocking {
        val owner = ownership()
        ownershipRepository.save(owner).awaitFirstOrNull()
        mongo.updateMulti(Query(), Update().unset("lazyValue"), OwnershipRepository.COLLECTION).awaitFirst()
        assertNotNull(ownershipRepository.findById(owner.id).awaitFirstOrNull())

        handler.runLongTask(null, "").collect()
        assertNull(ownershipRepository.findById(owner.id).awaitFirstOrNull())
    }

    fun ownership(): Ownership {
        return Ownership(
            token = randomAddress(),
            tokenId = EthUInt256.ONE,
            creators = listOf(),
            owner = randomAddress(),
            value = EthUInt256.ONE,
            lazyValue = EthUInt256.ZERO,
            date = Instant.now(),
            pending = listOf()
        )
    }

    fun logEvent(token: Address, tokenId: EthUInt256, owner: Address): LogEvent {
        return LogEvent(
            data = ItemTransfer(
                owner = owner,
                from = randomAddress(),
                token = token,
                tokenId = tokenId,
                date = nowMillis(),
                value = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000))
            ),
            address = randomAddress(),
            topic = CreateERC721RaribleUserEvent.id(),
            transactionHash = Word.apply(randomWord()),
            status = LogEventStatus.CONFIRMED,
            index = 0,
            minorLogIndex = 0
        )
    }

    fun itemTransfer() = ItemTransfer(
        owner = randomAddress(),
        token = randomAddress(),
        tokenId = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 2)),
        date = nowMillis(),
        from = randomAddress(),
        value = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000))
    )
}
