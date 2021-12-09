package com.rarible.protocol.nft.core.service.token

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.contracts.collection.CreateEvent
import com.rarible.protocol.dto.NftCollectionMetaDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.model.TokenProperties
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address

@IntegrationTest
class TokenUpdateServiceTest : AbstractIntegrationTest() {

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

        checkMetaWasPublished(
            NftCollectionMetaDto(
                name = "Feudalz",
                description = "Feudalz emerged to protect their Peasants.",
                external_link = "https://feudalz.io",
                image = NftMediaDto(
                    url = mapOf("ORIGINAL" to "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d"),
                    meta = mapOf(
                        "ORIGINAL" to NftMediaMetaDto(
                            type = "image/png",
                            width = 256,
                            height = 256
                        )
                    )
                ),
                fee_recipient = Address.apply("0x6EF5129faca91E410fa27188495753a33c36E305"),
                seller_fee_basis_points = 250
            )
        )
    }
}
