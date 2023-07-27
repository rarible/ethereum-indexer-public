package com.rarible.protocol.nft.api.e2e.items

import com.rarible.protocol.nft.api.e2e.data.createOwnership
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.api.service.ownership.OwnershipApiService
import com.rarible.protocol.nft.api.test.AbstractIntegrationTest
import com.rarible.protocol.nft.api.test.End2EndTest
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipFilter
import com.rarible.protocol.nft.core.model.OwnershipFilterAll
import com.rarible.protocol.nft.core.model.OwnershipFilterByCollection
import com.rarible.protocol.nft.core.model.OwnershipFilterByCreator
import com.rarible.protocol.nft.core.model.OwnershipFilterByOwner
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.remove
import scalether.domain.Address

@End2EndTest
class OwnershipApiServiceIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Autowired
    private lateinit var ownershipApiService: OwnershipApiService

    private val defaultSort = OwnershipFilter.Sort.LAST_UPDATE

    @Test
    fun `should find ownership - not deleted`() = runBlocking<Unit> {
        val ownership = createOwnership().copy(deleted = false)
        ownershipRepository.save(ownership).awaitFirst()

        val savedOwnership = ownershipApiService.get(ownership.id, false)
        assertThat(savedOwnership.id).isEqualTo(ownership.id.decimalStringValue)
    }

    @Test
    fun `should find ownership - deleted`() = runBlocking<Unit> {
        val ownership = createOwnership().copy(deleted = true)
        ownershipRepository.save(ownership).awaitFirst()

        val savedOwnership = ownershipApiService.get(ownership.id, true)
        assertThat(savedOwnership.id).isEqualTo(ownership.id.decimalStringValue)
    }

    @Test
    fun `should not find deleted ownership`() = runBlocking<Unit> {
        val ownership = createOwnership().copy(deleted = true)
        ownershipRepository.save(ownership).awaitFirst()

        assertThrows<EntityNotFoundApiException> {
            ownershipApiService.get(ownership.id, false)
        }
    }

    @Test
    fun `should find all ownerships`() = runBlocking<Unit> {
        saveOwnership(Address.ONE(), Address.TWO(), Address.THREE())
        saveOwnership(Address.TWO(), Address.TWO(), Address.FOUR())

        var ownerships = ownershipApiService.search(OwnershipFilterAll(defaultSort, false), null, 10)
        assertThat(ownerships).hasSize(2)

        ownerships = ownershipApiService.search(OwnershipFilterAll(defaultSort, false), null, 1)
        assertThat(ownerships).hasSize(1)
    }

    @Test
    fun `should find all ownerships by collection`() = runBlocking<Unit> {
        saveOwnership(Address.ONE(), Address.TWO(), Address.THREE())
        saveOwnership(Address.TWO(), Address.TWO(), Address.FOUR())

        var ownerships =
            ownershipApiService.search(OwnershipFilterByCollection(defaultSort, Address.ONE()), null, 10)
        Assertions.assertThat(ownerships).hasSize(1)

        ownerships = ownershipApiService.search(OwnershipFilterByCollection(defaultSort, Address.TWO()), null, 10)
        Assertions.assertThat(ownerships).hasSize(1)
    }

    @Test
    fun `should find all ownerships by owner`() = runBlocking<Unit> {
        saveOwnership(Address.ONE(), Address.TWO(), Address.THREE())
        saveOwnership(Address.TWO(), Address.TWO(), Address.FOUR())

        var ownerships = ownershipApiService.search(OwnershipFilterByOwner(defaultSort, Address.ONE()), null, 10)
        Assertions.assertThat(ownerships).isEmpty()

        ownerships = ownershipApiService.search(OwnershipFilterByOwner(defaultSort, Address.THREE()), null, 10)
        Assertions.assertThat(ownerships).hasSize(1)

        ownerships = ownershipApiService.search(OwnershipFilterByOwner(defaultSort, Address.FOUR()), null, 10)
        Assertions.assertThat(ownerships).hasSize(1)
    }

    @Test
    fun `should find all ownerships by creator`() = runBlocking<Unit> {
        saveOwnership(Address.ONE(), Address.TWO(), Address.THREE())
        saveOwnership(Address.TWO(), Address.TWO(), Address.FOUR())

        var ownerships =
            ownershipApiService.search(OwnershipFilterByCreator(defaultSort, Address.ONE()), null, 10)
        Assertions.assertThat(ownerships).isEmpty()

        ownerships = ownershipApiService.search(OwnershipFilterByCreator(defaultSort, Address.TWO()), null, 10)
        Assertions.assertThat(ownerships).hasSize(2)
    }

    @Test
    fun `should find by ids`() = runBlocking<Unit> {
        val expected = listOf(createOwnership(), createOwnership(), createOwnership())
            .onEach { ownershipRepository.save(it).awaitFirst() }
        val ids = expected.map { it.id }

        ownershipApiService.get(emptyList()).let { actual ->
            assertThat(actual).isEmpty()
        }

        ownershipApiService.get(listOf(ids.first())).let { actual ->
            assertThat(actual).hasSize(1)
        }

        ownershipApiService.get(ids).let { actual ->
            assertThat(actual).hasSize(3)
        }
    }

    @Test
    fun `should get all ownerships by owner`() = runBlocking<Unit> {
        // given
        val owner = Address.FOUR()
        saveOwnership(Address.ONE(), Address.TWO(), owner)
        saveOwnership(Address.TWO(), Address.THREE(), owner)

        // when
        val actual = ownershipApiService.getAllByOwner(owner.toString())

        // then
        assertThat(actual.map { it.token }).containsExactly(Address.TWO(), Address.ONE())
    }

    @AfterEach
    fun afterEach() = runBlocking<Unit> {
        mongo.remove<Ownership>().all().awaitFirst()
    }

    private suspend fun saveOwnership(token: Address, creator: Address, owner: Address) {
        ownershipRepository.save(
            createOwnership(token, creator, owner)
        ).awaitFirst()
    }
}
