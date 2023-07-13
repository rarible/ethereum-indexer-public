package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.data.createRandomOwnership
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipFilter
import com.rarible.protocol.nft.core.model.OwnershipFilterByItem
import com.rarible.protocol.nft.core.service.item.ItemReduceEventListener
import com.rarible.protocol.nft.core.service.ownership.OwnershipService
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import reactor.core.publisher.Mono

@ExtendWith(MockKExtension::class)
class OwnershipUpdateServiceTest {

    private lateinit var ownershipService: OwnershipService

    private lateinit var eventListenerListener: ItemReduceEventListener

    private lateinit var properties: NftIndexerProperties

    private lateinit var service: OwnershipUpdateService

    @BeforeEach
    fun setUp() {
        ownershipService = mockk()
        eventListenerListener = mockk()
        properties = mockk {
            every { ownershipFetchBatchSize } returns 2
        }

        service = OwnershipUpdateService(ownershipService, eventListenerListener, properties)
    }

    @Test
    fun `delete by item ids`() = runBlocking<Unit> {
        // given
        val itemId = createRandomItemId()
        val ownership1 = createRandomOwnership()
        val ownership2 = createRandomOwnership()
        val ownership3 = createRandomOwnership()
        coEvery { ownershipService.search(any(), any(), any()) } returnsMany
                listOf(listOf(ownership1, ownership2), listOf(ownership3))
        coEvery { ownershipService.save(any()) } coAnswers { args[0] as Ownership }
        coEvery { eventListenerListener.onOwnershipChanged(any(), any()) } returns Mono.empty()
        val deleted1 = ownership1.copy(deleted = true)
        val deleted2 = ownership2.copy(deleted = true)
        val deleted3 = ownership3.copy(deleted = true)

        // when
        service.deleteByItemId(itemId)

        // then
        coVerifyOrder {
            ownershipService.search(
                OwnershipFilterByItem(
                contract = itemId.token,
                tokenId = itemId.tokenId.value,
                sort = OwnershipFilter.Sort.LAST_UPDATE
            ), null, 2)
            ownershipService.save(deleted1)
            eventListenerListener.onOwnershipChanged(deleted1, any())
            ownershipService.save(deleted2)
            eventListenerListener.onOwnershipChanged(deleted2, any())
            ownershipService.search(
                OwnershipFilterByItem(
                    contract = itemId.token,
                    tokenId = itemId.tokenId.value,
                    sort = OwnershipFilter.Sort.LAST_UPDATE
                ), any(), 2
            )
            ownershipService.save(deleted3)
            eventListenerListener.onOwnershipChanged(deleted3, any())

        }
        confirmVerified(ownershipService, eventListenerListener)
    }
}
