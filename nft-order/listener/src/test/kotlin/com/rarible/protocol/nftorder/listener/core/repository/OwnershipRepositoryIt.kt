package com.rarible.protocol.nftorder.listener.core.repository

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nftorder.core.model.Ownership
import com.rarible.protocol.nftorder.core.repository.OwnershipRepository
import com.rarible.protocol.nftorder.listener.test.IntegrationTest
import com.rarible.protocol.nftorder.listener.test.data.randomOwnership
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import scalether.domain.Address

@IntegrationTest
internal class OwnershipRepositoryIt {

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Test
    fun `test ownership raw format`() = runBlocking<Unit> {
        val ownership = randomOwnership()

        ownershipRepository.save(ownership)

        val document = mongo.findById(
            ownership.id,
            Document::class.java,
            mongo.getCollectionName(Ownership::class.java)
        ).block()

        assertEquals(
            ownership.contract,
            Address.apply(document.getString(Ownership::contract.name))
        )
        assertEquals(
            ownership.date.toEpochMilli(),
            document.getDate(Ownership::date.name).time
        )
        assertEquals(
            ownership.tokenId,
            EthUInt256.Companion.of(document.getString(Ownership::tokenId.name))
        )
    }
}
