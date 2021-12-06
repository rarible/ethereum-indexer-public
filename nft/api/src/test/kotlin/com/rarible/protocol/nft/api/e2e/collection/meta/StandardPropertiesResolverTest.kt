package com.rarible.protocol.nft.api.e2e.collection.meta

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.contracts.erc721.rarible.ERC721Rarible
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.token.meta.descriptors.StandardPropertiesResolver
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoSigningTransactionSender
import java.net.URI

@End2EndTest
class StandardPropertiesResolverTest : SpringContainerBaseTest() {

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Autowired
    private lateinit var mapper: ObjectMapper

    @Autowired
    private lateinit var ipfsService: IpfsService

    private lateinit var resolver: StandardPropertiesResolver

    private lateinit var userSender: MonoSigningTransactionSender
    private lateinit var erc721: ERC721Rarible

    @BeforeEach
    fun before() = runBlocking<Unit> {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        userSender = createSigningSender(privateKey)
        erc721 = ERC721Rarible.deployAndWait(userSender, poller).awaitFirst()
        erc721.__ERC721Rarible_init(
            "Feudalz",
            "FEUDALZ",
            "baseURI",
            "ipfs://QmeRwHVnYHthtPezLFNMLamC21b7BMm6Er18bG3DzTVE3T"
        ).execute().verifySuccess()
        val tokenToSave = createToken().copy(
            id = erc721.address(),
            standard = TokenStandard.ERC721
        )
        tokenRepository.save(tokenToSave).awaitSingle()
    }

    @Test
    fun `should parse from ipfs`() = runBlocking<Unit> {
        resolver = object : StandardPropertiesResolver(userSender, ipfsService, tokenRepository, mapper, 60000) {
            override val client get() = mockSuccessIpfsResponse()
        }
        val props = resolver.resolve(erc721.address())
        assertThat(props).isEqualTo(TokenProperties(
            name = "Feudalz",
            description = "Feudalz emerged to protect their Peasants. When the system run smoothly, it lead to peace and prosperity for everyone.",
            externalLink = "https://feudalz.io",
            image = "https://ipfs.io/ipfs/QmTGtDqnPi8TiQrSHqg44Lm7DNvvye6Tw4Z6eMMuMqkS6d",
            feeRecipient = Address.apply("0x6EF5129faca91E410fa27188495753a33c36E305"),
            sellerFeeBasisPoints = 250
        ))
    }

    @Test
    fun `should return null if not found`() = runBlocking<Unit> {
        resolver = object : StandardPropertiesResolver(userSender, ipfsService, tokenRepository, mapper, 60000) {
            override val client get() = mockNotFoundIpfsResponse()
        }
        val props = resolver.resolve(erc721.address())
        assertThat(props).isNull()
    }

    @Test
    fun `should return null if broken json`() = runBlocking<Unit> {
        resolver = object : StandardPropertiesResolver(userSender, ipfsService, tokenRepository, mapper, 60000) {
            override val client get() = mockBrokenJsonIpfsResponse()
        }
        val props = resolver.resolve(erc721.address())
        assertThat(props).isNull()
    }

    private fun mockSuccessIpfsResponse(): WebClient {
        return WebClient.builder()
            .exchangeFunction { request ->
                Assertions.assertThat(request.url())
                    .isEqualTo(URI("https://rarible.mypinata.cloud/ipfs/QmeRwHVnYHthtPezLFNMLamC21b7BMm6Er18bG3DzTVE3T"))
                Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                        .header("content-type", "application/json")
                        .body("ipfs.json".asResource())
                        .build()
                )
            }.build()
    }

    private fun mockNotFoundIpfsResponse(): WebClient {
        return WebClient.builder()
            .exchangeFunction { request ->
                Assertions.assertThat(request.url()).isEqualTo(URI("https://rarible.mypinata.cloud/ipfs/QmeRwHVnYHthtPezLFNMLamC21b7BMm6Er18bG3DzTVE3T"))
                Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build())
            }.build()
    }

    private fun mockBrokenJsonIpfsResponse(): WebClient {
        return WebClient.builder()
            .exchangeFunction { request ->
                Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                        .header("content-type", "plain/text")
                        .body("Hello world!")
                        .build()
                )
            }.build()
    }

    fun String.asResource() = this.javaClass::class.java.getResource("/data/token/response/$this").readText()

}
