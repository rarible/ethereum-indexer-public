package com.rarible.protocol.nft.core.repository.ownership

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomOwnership
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.misc.isEqualToOwnership
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipContinuation
import com.rarible.protocol.nft.core.model.OwnershipFilter
import com.rarible.protocol.nft.core.model.OwnershipFilterAll
import com.rarible.protocol.nft.core.repository.ownership.OwnershipFilterCriteria.toCriteria
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.time.Duration

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

    @Test
    fun `should search ownerships with continuation and sort by ascending`() = runBlocking<Unit> {
        // given
        val first = createRandomOwnership().copy(date = nowMillis() - Duration.ofMinutes(60))
        val second = createRandomOwnership().copy(date = nowMillis() - Duration.ofMinutes(50))
        val third = createRandomOwnership().copy(date = nowMillis() - Duration.ofMinutes(40))
        val fourth = createRandomOwnership().copy(date = nowMillis() - Duration.ofMinutes(30))
        val fifth = createRandomOwnership().copy(date = nowMillis() - Duration.ofMinutes(20))
        listOf(first, second, third, fourth, fifth).forEach { ownershipRepository.save(it).awaitFirst() }
        val filter = OwnershipFilterAll(
            sort = OwnershipFilter.Sort.LAST_UPDATE_ASC,
            showDeleted = false,
        )

        // when
        var continuation: String? = null
        val criteria1 = filter.toCriteria(OwnershipContinuation.parse(continuation), 3)
        val result1 = ownershipRepository.search(criteria1)
        continuation = OwnershipContinuation(result1.last().date, result1.last().id).toString()
        val criteria2 = filter.toCriteria(OwnershipContinuation.parse(continuation), 3)
        val result2 = ownershipRepository.search(criteria2)
        continuation = OwnershipContinuation(result2.last().date, result2.last().id).toString()
        val criteria3 = filter.toCriteria(OwnershipContinuation.parse(continuation), 3)
        val result3 = ownershipRepository.search(criteria3)

        // then
        assertThat(result1).usingRecursiveComparison().ignoringFields("version").isEqualTo(listOf(first, second, third))
        assertThat(result2).usingRecursiveComparison().ignoringFields("version").isEqualTo(listOf(fourth, fifth))
        assertThat(result3).isEmpty()
    }

    @Test
    fun findByOwner() = runBlocking<Unit> {
        val ownership1 = ownershipRepository.save(
            createRandomOwnership(
                token = Address.ONE(),
                tokenId = EthUInt256.of(1),
                owner = Address.TWO()
            )
        ).awaitFirst()
        val ownership2 = ownershipRepository.save(
            createRandomOwnership(
                token = Address.ONE(),
                tokenId = EthUInt256.of(2),
                owner = Address.TWO()
            )
        ).awaitFirst()
        val ownership3 = ownershipRepository.save(
            createRandomOwnership(token = Address.ONE(), tokenId = EthUInt256.of(3), owner = Address.TWO())
        ).awaitFirst()
        val ownership4 = ownershipRepository.save(
            createRandomOwnership(token = Address.ONE(), tokenId = EthUInt256.of(4), owner = Address.THREE())
        ).awaitFirst()

        val result =
            ownershipRepository.findByOwner(owner = Address.TWO(), fromIdExcluded = ownership1.id).toList()

        assertThat(result.map { it.id }).containsExactly(ownership2.id, ownership3.id)
    }
}
