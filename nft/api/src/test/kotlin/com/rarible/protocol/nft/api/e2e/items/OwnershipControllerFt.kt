package com.rarible.protocol.nft.api.e2e.items

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.dto.NftOwnershipIdsDto
import com.rarible.protocol.nft.api.e2e.data.createOwnership
import com.rarible.protocol.nft.api.test.AbstractIntegrationTest
import com.rarible.protocol.nft.api.test.End2EndTest
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@End2EndTest
class OwnershipControllerFt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Test
    fun `should get ownership by id`() = runBlocking<Unit> {
        val ownership = createOwnership()
        ownershipRepository.save(ownership).awaitFirst()

        val ownershipDto = nftOwnershipApiClient.getNftOwnershipById(ownership.id.decimalStringValue, false)
            .awaitFirst()

        assertThat(ownershipDto.id).isEqualTo(OwnershipId.parseId(ownership.id.decimalStringValue).decimalStringValue)
        assertThat(ownershipDto.contract).isEqualTo(ownership.token)
        assertThat(ownershipDto.tokenId).isEqualTo(ownership.tokenId.value)
        assertThat(ownershipDto.owner).isEqualTo(ownership.owner)
        assertThat(ownershipDto.value).isEqualTo(ownership.value.value)
        assertThat(ownershipDto.date).isEqualTo(ownership.date)

        ownershipDto.pending.forEachIndexed { index, itemHistoryDto ->
            val itemHistory = ownership.pending[index]

            assertThat(itemHistoryDto.owner).isEqualTo(itemHistory.owner)
            assertThat(itemHistoryDto.contract).isEqualTo(itemHistory.token)
            assertThat(itemHistoryDto.tokenId).isEqualTo(itemHistory.tokenId.value)
            assertThat(itemHistoryDto.value).isEqualTo(itemHistory.value.value)
            assertThat(itemHistoryDto.date).isEqualTo(itemHistory.date)

            assertThat(itemHistoryDto is ItemTransferDto).isEqualTo(true)
            assertThat((itemHistoryDto as ItemTransferDto).from).isEqualTo(itemHistory.from)
        }

        val res = nftOwnershipApiClient.getNftOwnershipsByItem(
            ownership.token.hex(),
            ownership.tokenId.value.toString(),
            null,
            null
        ).awaitFirst()
        assertThat(res.ownerships)
            .hasSize(1)
        assertThat(res.ownerships.firstOrNull())
            .hasFieldOrPropertyWithValue("id", ownership.id.decimalStringValue)
    }

    @Test
    fun `should get only not deleted ownerships`() = runBlocking<Unit> {
        val ownership1 = createOwnership().copy(deleted = false)
        val ownership2 = createOwnership().copy(deleted = true)
        ownershipRepository.save(ownership1).awaitFirst()
        ownershipRepository.save(ownership2).awaitFirst()

        val ownershipDto = nftOwnershipApiClient.getNftAllOwnerships(null, null, null).awaitFirst()
        assertThat(ownershipDto.ownerships).hasSize(1)
        assertThat(ownershipDto.ownerships.single().id).isEqualTo(ownership1.id.decimalStringValue)
    }

    @Test
    fun `should get only not deleted ownerships with filter`() = runBlocking<Unit> {
        val token = randomAddress()
        val tokenId = EthUInt256.of(randomBigInt())
        val ownership1 = createOwnership().copy(token = token, tokenId = tokenId, deleted = false)
        val ownership2 = createOwnership().copy(token = token, tokenId = tokenId, deleted = true)
        ownershipRepository.save(ownership1).awaitFirst()
        ownershipRepository.save(ownership2).awaitFirst()

        val ownershipDto =
            nftOwnershipApiClient.getNftOwnershipsByItem(token.hex(), tokenId.value.toString(), null, null).awaitFirst()
        assertThat(ownershipDto.ownerships).hasSize(1)
        assertThat(ownershipDto.ownerships.single().id).isEqualTo(ownership1.id.decimalStringValue)
    }

    @Test
    fun `should get deleted ownerships`() = runBlocking<Unit> {
        val ownership1 = createOwnership().copy(deleted = false)
        val ownership2 = createOwnership().copy(deleted = true)
        ownershipRepository.save(ownership1).awaitFirst()
        ownershipRepository.save(ownership2).awaitFirst()

        val ownershipDto = nftOwnershipApiClient.getNftAllOwnerships(null, null, true).awaitFirst()
        assertThat(ownershipDto.ownerships).hasSize(2)
        assertThat(ownershipDto.ownerships.map { it.id }).containsExactlyInAnyOrder(
            ownership1.id.decimalStringValue, ownership2.id.decimalStringValue
        )
    }

    @Test
    fun `should get ownership by owner`() = runBlocking<Unit> {
        val ownership = createOwnership()
        val deletedOwnership = createOwnership().copy(deleted = true)
        ownershipRepository.save(ownership).awaitFirst()
        ownershipRepository.save(deletedOwnership).awaitFirst()

        val ownershipDto =
            nftOwnershipApiClient.getNftOwnershipsByOwner(ownership.owner.prefixed(), null, null, null).awaitFirst()
        assertThat(ownershipDto.ownerships).hasSize(1)
        assertThat(ownershipDto.ownerships[0].id).isEqualTo(ownership.id.decimalStringValue)
        assertThat(ownershipDto.continuation).isNull()
    }

    @Test
    fun `should get ownership by owner and collection`() = runBlocking<Unit> {
        val owner = randomAddress()
        val collection = randomAddress()
        val ownership1 = createOwnership().copy(owner = owner, token = collection, tokenId = EthUInt256.of(1))
        val ownership2 = createOwnership().copy(owner = owner, token = collection, tokenId = EthUInt256.of(2))
        val ownership3 = createOwnership().copy(owner = owner, tokenId = EthUInt256.of(3))
        val ownership4 = createOwnership().copy(token = collection, tokenId = EthUInt256.of(4))
        listOf(ownership1, ownership2, ownership3, ownership4).forEach {
            ownershipRepository.save(it).awaitFirst()
        }
        val ownershipDto =
            nftOwnershipApiClient.getNftOwnershipsByOwner(owner.prefixed(), collection.prefixed(), null, null)
                .awaitFirst()
        assertThat(ownershipDto.ownerships.map { it.id }).containsExactlyInAnyOrder(
            ownership1.id.decimalStringValue,
            ownership2.id.decimalStringValue
        )
        assertThat(ownershipDto.continuation).isNull()
    }

    @Test
    fun `should get ownerships by ids`() = runBlocking<Unit> {
        val ownerships = (1..5).map { createOwnership() }
            .sortedBy { it.id.stringValue }
            .onEach { ownershipRepository.save(it).awaitFirst() }

        val idsDto = NftOwnershipIdsDto(ownerships.map { it.id.stringValue })
        val result = nftOwnershipApiClient.getNftOwnershipsByIds(idsDto).awaitFirst()

        assertThat(result.ownerships).hasSize(idsDto.ids.size)
        assertThat(result.ownerships.map { it.id }).isEqualTo(idsDto.ids.map { OwnershipId.parseId(it).decimalStringValue })
    }
}
