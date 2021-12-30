package com.rarible.protocol.nft.core.repository.ownership

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.repository.data.createOwnership
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address

@FlowPreview
@IntegrationTest
internal class OwnershipRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Test
    fun `should save and get ownership`() = runBlocking<Unit> {
        val item = createOwnership()

        ownershipRepository.save(item).awaitFirst()

        val savedItem = ownershipRepository.findById(item.id).awaitFirstOrNull()
        Assertions.assertThat(savedItem).isEqualTo(item)
    }

    @Test
    fun `should return ownership if removed`() = runBlocking {
        val ownership = createOwnership()

        ownershipRepository.save(ownership).awaitFirst()
        val deleted = ownershipRepository.deleteById(ownership.id).awaitFirst()

        assertEquals(deleted, ownership)
    }

    @Test
    fun `should return empty mono if not removed`() = runBlocking {
        val ownership = createOwnership()

        val deleted = ownershipRepository.deleteById(ownership.id).awaitFirstOrNull()

        assertNull(deleted)
    }

    @Test
    fun `test ownership raw format`() = runBlocking<Unit> {
        val ownership = createOwnership()

        ownershipRepository.save(ownership).awaitFirst()

        val document = mongo.findById(
            ownership.id,
            Document::class.java,
            mongo.getCollectionName(Ownership::class.java)
        ).block()

        assertEquals(
            ownership.token,
            Address.apply(document.getString(Ownership::token.name))
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
