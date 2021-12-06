package com.rarible.protocol.nft.api.e2e.collection.meta

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.token.meta.descriptors.OpenseaPropertiesResolver
import io.mockk.InternalPlatformDsl.toStr
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.AddressFactory

@End2EndTest
class OpenseaPropertiesResolverTest : SpringContainerBaseTest() {

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Autowired
    private lateinit var mapper: ObjectMapper

    private lateinit var resolver: OpenseaPropertiesResolver
    private lateinit var token: Token

    @BeforeEach
    fun before() = runBlocking<Unit> {
        token = createToken().copy(
            id = AddressFactory.create(),
            standard = TokenStandard.ERC721
        )
        tokenRepository.save(token).awaitSingle()
    }

    @Test
    fun `should parse from json`() = runBlocking<Unit> {
        resolver = object : OpenseaPropertiesResolver(mapper, "https://api.opensea.io/api/v1", "", 60000, 60000, 60000, "") {
            override val client get() = mockOpenSeaResponse()
        }
        val props = resolver.resolve(token.id)
        assertThat(props).isEqualTo(TokenProperties(
            name = "Feudalz",
            description = "Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.",
            externalLink = "https://feudalz.io",
            image = "https://lh3.googleusercontent.com/wveucmeXBJfqyGiPZDhC1jVaJcx9SH0l2fiLmp2OdLD0KYpFzUIQD_9tTOV57cCDjJ4EjZT6X-Zoyym9eXXHTDxmVfCYzhC_RgkAU0A=s120",
            feeRecipient = Address.apply("0xc00f4b8022e4dc7f086d703328247cb6adf26858"),
            sellerFeeBasisPoints = 250
        ))
    }

    private fun mockOpenSeaResponse(): WebClient {
        return WebClient.builder()
            .exchangeFunction { request ->
                assertThat(request.url().toStr()).startsWith("https://api.opensea.io/api/v1/asset_contract/0x")
                Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                        .header("content-type", "application/json")
                        .body("opensea.json".asResource())
                        .build()
                )
            }.build()
    }

    fun String.asResource() = this.javaClass::class.java.getResource("/data/token/response/$this").readText()

}
