package com.rarible.protocol.nft.core.service

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.data.createRandomItemProperties
import com.rarible.protocol.nft.core.event.OutgoingEventListener
import com.rarible.protocol.nft.core.model.ActionEvent
import com.rarible.protocol.nft.core.model.BurnItemActionEvent
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

internal class EnsDomainServiceTest {
    private val listener = mockk<OutgoingEventListener<ActionEvent>>()
    private val nftIndexerProperties = mockk<NftIndexerProperties> {
        every { action } returns NftIndexerProperties.ActionProperties(burnDelay = Duration.ZERO)
    }
    private val ensDomainService = EnsDomainService(listOf(listener), nftIndexerProperties)

    @Test
    fun `should emit burn action with target burnAt params`() = runBlocking {
        val expirationDate = "2023-04-01T17:26:15Z"

        val itemId = ItemId(randomAddress(), EthUInt256.of(randomBigInt()))
        val properties = createRandomItemProperties().copy(
            attributes = listOf(
                ItemAttribute("test key", "test valur"),
                ItemAttribute(EnsDomainService.EXPIRATION_DATE_PROPERTY, expirationDate)
            )
        )
        coEvery { listener.onEvent(any()) } returns Unit

        ensDomainService.onGetProperties(itemId, properties)

        coVerify(exactly = 1) {
            listener.onEvent(withArg {
                assertThat(it).isInstanceOf(BurnItemActionEvent::class.java)
                val burnEvent = it as BurnItemActionEvent
                assertThat(burnEvent.itemId()).isEqualTo(itemId)
                assertThat(burnEvent.burnAt).isEqualTo(Instant.parse(expirationDate))
            })
        }
    }

    @Test
    fun `should emit burn action with now burn at`() = runBlocking {
        val itemId = ItemId(randomAddress(), EthUInt256.of(randomBigInt()))
        val properties = createRandomItemProperties().copy(
            attributes = emptyList(),
        )
        coEvery { listener.onEvent(any()) } returns Unit

        ensDomainService.onGetProperties(itemId, properties)

        coVerify(exactly = 1) {
            listener.onEvent(withArg {
                assertThat(it).isInstanceOf(BurnItemActionEvent::class.java)
                val burnEvent = it as BurnItemActionEvent
                assertThat(burnEvent.itemId()).isEqualTo(itemId)
            })
        }
    }
}
