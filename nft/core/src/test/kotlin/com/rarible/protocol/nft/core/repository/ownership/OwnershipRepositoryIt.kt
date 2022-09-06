package com.rarible.protocol.nft.core.repository.ownership

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.UnversionedOwnership
import com.rarible.protocol.nft.core.data.createRandomOwnership
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.misc.isEqualToOwnership
import com.rarible.protocol.nft.core.model.Ownership
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address

@IntegrationTest
internal class OwnershipRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Test
    fun `should save and get ownership`() = runBlocking<Unit> {
        val ownership = createRandomOwnership()

        ownershipRepository.save(ownership).awaitFirst()

        val savedOwnership = ownershipRepository.findById(ownership.id).awaitFirstOrNull()
        assertThat(savedOwnership).isEqualToOwnership(ownership)
    }

    @Test
    fun `should save and get ownership with version`() = runBlocking<Unit> {
        val ownership = createRandomOwnership()

        val unversionedOwnership = UnversionedOwnership(
            token = ownership.token,
            tokenId = ownership.tokenId,
            creators = ownership.creators,
            owner = ownership.owner,
            value = ownership.value,
            lazyValue = ownership.lazyValue,
            date = ownership.date,
            pending = ownership.pending,
            lastUpdatedAt = ownership.lastUpdatedAt
        )

        mongo.save(unversionedOwnership).awaitFirst()

        assertThrows<RuntimeException> { ownershipRepository.save(ownership).awaitFirst() }

        ownershipRepository.save(ownership.withVersion(0)).awaitFirst()

        val savedOwnership = ownershipRepository.findById(ownership.id).awaitFirstOrNull()
        assertThat(savedOwnership).isEqualToOwnership(ownership)
    }

    @Test
    fun `should return ownership if removed`() = runBlocking<Unit> {
        val ownership = createRandomOwnership()
        ownershipRepository.save(ownership).awaitFirst()
        val deleted = ownershipRepository.deleteById(ownership.id).awaitFirst()
        assertThat(deleted).isEqualToOwnership(deleted)
    }

    @Test
    fun `should return empty mono if not removed`() = runBlocking {
        val ownership = createRandomOwnership()

        val deleted = ownershipRepository.deleteById(ownership.id).awaitFirstOrNull()

        assertNull(deleted)
    }

    @Test
    fun `test ownership raw format`() = runBlocking<Unit> {
        val ownership = createRandomOwnership()

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
