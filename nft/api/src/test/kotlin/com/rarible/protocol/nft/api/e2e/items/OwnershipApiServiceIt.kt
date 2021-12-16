package com.rarible.protocol.nft.api.e2e.items

import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createOwnership
import com.rarible.protocol.nft.api.service.ownership.OwnershipApiService
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.remove
import scalether.domain.Address

@End2EndTest
class OwnershipApiServiceIt : SpringContainerBaseTest() {
    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Autowired
    private lateinit var ownershipApiService: OwnershipApiService

    private val defaultSort = OwnershipFilter.Sort.LAST_UPDATE

    @Test
    fun `should find all ownerships`() = runBlocking<Unit> {
        saveOwnership(Address.ONE(), Address.TWO(), Address.THREE())
        saveOwnership(Address.TWO(), Address.TWO(), Address.FOUR())

        var ownerships = ownershipApiService.search(OwnershipFilterAll(defaultSort, false), null, 10)
        Assertions.assertThat(ownerships).hasSize(2)

        ownerships = ownershipApiService.search(OwnershipFilterAll(defaultSort, false), null, 1)
        Assertions.assertThat(ownerships).hasSize(1)
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
