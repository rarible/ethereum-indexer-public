package com.rarible.protocol.nft.api.service.admin

import com.ninjasquad.springmockk.MockkBean
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createAddress
import com.rarible.protocol.nft.api.e2e.data.createItem
import com.rarible.protocol.nft.api.e2e.data.createOwnership
import com.rarible.protocol.nft.api.e2e.data.randomItemId
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.flux
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address

@OptIn(ExperimentalCoroutinesApi::class)
@End2EndTest
class MaintenanceServiceIt : SpringContainerBaseTest() {

    @Autowired
    private lateinit var service: MaintenanceService

    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @MockkBean
    private lateinit var itemReduceService: ItemReduceService

    @Test
    fun `should fix user items`() = runBlocking<Unit> {
        // given
        val user = createAddress()
        val validItemId = randomItemId()
        val fixableItemId = randomItemId()
        val unfixableItemId = randomItemId()
        saveItem(validItemId, user)
        saveOwnership(validItemId, user)
        saveOwnership(fixableItemId, user)
        saveOwnership(unfixableItemId, user)
        every { itemReduceService.update(any<Address>(), any<EthUInt256>(), null, null) } answers {
            val itemId = ItemId(firstArg(), secondArg())
            if (itemId == fixableItemId) {
                runBlocking { saveItem(itemId, user) }
            }
            flux { send(randomItemId()) }
        }

        // when
        val actual = service.fixUserItems(user.toString())

        // then
        assertThat(actual.valid).containsExactly(validItemId.toString())
        assertThat(actual.fixed).containsExactly(fixableItemId.toString())
        assertThat(actual.unfixed).containsExactly(unfixableItemId.toString())
    }

    private suspend fun saveOwnership(itemId: ItemId, owner: Address) {
        ownershipRepository.save(
            createOwnership(itemId.token, itemId.tokenId, creator = null, owner)
        ).awaitFirst()
    }

    private suspend fun saveItem(itemId: ItemId, owner: Address): Item {
        val item = createItem(itemId.token, itemId.tokenId, listOf(owner))
        itemRepository.save(item).awaitFirst()
        return item
    }
}