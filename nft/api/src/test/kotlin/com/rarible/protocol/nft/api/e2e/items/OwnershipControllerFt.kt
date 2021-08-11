package com.rarible.protocol.nft.api.e2e.items

import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createOwnership
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@End2EndTest
class OwnershipControllerFt : SpringContainerBaseTest() {

    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Test
    fun `should get ownership by id`() = runBlocking<Unit> {
        val ownership = createOwnership()
        ownershipRepository.save(ownership).awaitFirst()

        val ownershipDto = nftOwnershipApiClient.getNftOwnershipById(ownership.id.decimalStringValue).awaitFirst()

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
}
