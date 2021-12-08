package com.rarible.protocol.nft.core.service.token

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.contracts.collection.CreateEvent
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.model.TokenProperties
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address

@IntegrationTest
class TokenUpdateServiceTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var tokenRegistrationService: TokenRegistrationService

    @Autowired
    lateinit var tokenUpdateService: TokenUpdateService

    @Test
    fun `should send msg to external topic with meta`() = runBlocking<Unit> {

        val props = TokenProperties(
            name = "Feudalz",
            description = "Feudalz emerged to protect their Peasants.",
            externalLink = "https://feudalz.io",
            image = "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d",
            feeRecipient = Address.apply("0x6EF5129faca91E410fa27188495753a33c36E305"),
            sellerFeeBasisPoints = 250
        )
        val id = randomAddress()
        coEvery { mockStandardTokenPropertiesResolver.resolve(any()) } returns props
        tokenHistoryRepository.save(
            LogEvent(
                CreateCollection(
                    id = id,
                    owner = randomAddress(),
                    name = "Test",
                    symbol = "TEST"
                ),
                address = id,
                topic = CreateEvent.id(),
                transactionHash = Word.apply(randomWord()),
                status = LogEventStatus.CONFIRMED,
                logIndex = 0,
                minorLogIndex = 0,
                index = 0
            )
        ).awaitFirst()

        tokenUpdateService.update(id)

        Wait.waitAssert {
            assertThat(collectionEvents)
                .hasSizeGreaterThanOrEqualTo(1)
                .satisfies { events ->
                    val filteredEvents = events.filter { event ->
                        when (event) {
                            is NftCollectionUpdateEventDto -> {
                                event.collection.meta?.name == "Feudalz"
                            }
                        }
                    }
                    assertThat(filteredEvents.size).isEqualTo(1)
                }
        }
    }
}
