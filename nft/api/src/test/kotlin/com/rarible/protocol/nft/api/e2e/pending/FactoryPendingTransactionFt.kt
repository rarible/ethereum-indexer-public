package com.rarible.protocol.nft.api.e2e.pending

import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.contracts.erc721.rarible.factory.ERC721RaribleFactoryC2
import com.rarible.protocol.contracts.erc721.rarible.factory.beacon.ERC721RaribleBeaconMinimal
import com.rarible.protocol.contracts.erc721.rarible.factory.token.ERC721RaribleMinimal
import com.rarible.protocol.contracts.erc721.v3.MintableOwnableToken
import com.rarible.protocol.contracts.erc721.v4.rarible.MintableToken
import com.rarible.protocol.dto.CreateTransactionRequestDto
import com.rarible.protocol.dto.LogEventDto
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.misc.SignUtils
import com.rarible.protocol.nft.api.service.pending.PendingTransactionService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.repository.TemporaryItemPropertiesRepository
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.meta.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesService
import com.rarible.protocol.nft.core.service.item.meta.descriptors.PropertiesCacheDescriptor
import io.daonomic.rpc.domain.Request
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.remove
import org.springframework.test.util.ReflectionTestUtils.setField
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Transaction
import scalether.domain.response.TransactionReceipt
import scalether.java.Lists
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger
import java.util.*

@End2EndTest
@Import(FactoryPendingTransactionFt.MockItemPropertiesServiceConfiguration::class)
class FactoryPendingTransactionFt : SpringContainerBaseTest() {

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Autowired
    private lateinit var propertiesCacheDescriptor: PropertiesCacheDescriptor

    @Autowired
    private lateinit var pendingTransactionService: PendingTransactionService

    @Autowired
    private lateinit var  ipfsService: IpfsService

    @Autowired
    private lateinit var nftIndexerProperties: NftIndexerProperties

    private val temporaryProperties = ItemProperties(
        name = UUID.randomUUID().toString(),
        description = "Test",
        image = "Test",
        imagePreview = "Test",
        imageBig = "Test",
        attributes = emptyList()
    )

    @BeforeEach
    fun cleanup() = runBlocking<Unit> {
        tokenRepository.drop().awaitFirstOrNull()

        every { ipfsService.resolveIpfsUrl(any()) } returns "Test"
        every { propertiesCacheDescriptor.getByUri(any()) } returns Mono.just(temporaryProperties)
    }

    @Test
    fun `should create contract`() = runBlocking<Unit> {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )
        val transferAddress = randomAddress()
        val lazyAddress = randomAddress()
        val token = ERC721RaribleMinimal.deployAndWait(userSender, poller).awaitSingle()
        val beacon = ERC721RaribleBeaconMinimal.deployAndWait(userSender, poller, token.address()).awaitSingle()
        val factory = ERC721RaribleFactoryC2.deployAndWait(userSender, poller,
            beacon.address(), transferAddress, lazyAddress).awaitSingle()
        setField(nftIndexerProperties, "factory", NftIndexerProperties.FactoryAddresses(
            erc721Rarible = factory.address().hex(),
            erc721RaribleUser = randomAddress().hex(),
            erc1155Rarible = randomAddress().hex(),
            erc1155RaribleUser = randomAddress().hex()
        ))

        val receipt = factory.createToken("NAME", "SYMBOL", "uri", "uri", BigInteger.ONE).execute().verifySuccess()
        val contract = factory.getAddress("NAME", "SYMBOL", "uri", "uri", BigInteger.ONE).awaitSingle()

        processTransaction(receipt, contract)
    }

    private suspend fun processTransaction(receipt: TransactionReceipt, contract: Address) {
        val tx = ethereum.ethGetTransactionByHash(receipt.transactionHash()).awaitFirst().get()

        val eventLogs = nftTransactionApiClient.createNftPendingTransaction(tx.toRequest()).collectList().awaitFirst()

        assertThat(eventLogs).hasSize(1)

        with(eventLogs.single()) {
            assertThat(transactionHash).isEqualTo(tx.hash())
            assertThat(address).isEqualTo(contract)
            assertThat(status).isEqualTo(LogEventDto.Status.PENDING)
        }
    }

    protected suspend fun Mono<Word>.verifySuccess(): TransactionReceipt {
        val receipt = waitReceipt()
        Assertions.assertTrue(receipt.success()) {
            val result = ethereum.executeRaw(
                Request(1, "trace_replayTransaction", Lists.toScala(
                    receipt.transactionHash().toString(),
                    Lists.toScala("trace", "stateDiff")
                ), "2.0")
            ).block()!!
            "traces: ${result.result().get()}"
        }
        return receipt
    }

    private suspend fun Mono<Word>.waitReceipt(): TransactionReceipt {
        val value = this.awaitFirstOrNull()
        require(value != null) { "txHash is null" }
        return ethereum.ethGetTransactionReceipt(value).awaitFirst().get()
    }

    private fun Transaction.toRequest() = CreateTransactionRequestDto(
        hash = hash(),
        from = from(),
        input = input(),
        nonce = nonce().toLong(),
        to = to()
    )

    @BeforeEach
    fun beforeEach() = runBlocking<Unit> {
        mongo.remove<Item>().all().awaitFirst()
    }


    @TestConfiguration
    class MockItemPropertiesServiceConfiguration {
        @Bean
        @Primary
        fun mockPropertiesCacheDescriptor(): PropertiesCacheDescriptor {
            return mockk()
        }

        @Bean
        @Primary
        fun mockIpfsService(): IpfsService {
            return mockk()
        }

        @Autowired
        private lateinit var nftIndexerProperties: NftIndexerProperties

        @Bean
        @Primary
        fun mockItemPropertiesService(
            mockkPropertiesCacheDescriptor: PropertiesCacheDescriptor,
            temporaryItemPropertiesRepository: TemporaryItemPropertiesRepository,
            mockkIpfsService: IpfsService
        ): ItemPropertiesService {
            return ItemPropertiesService(
                propertiesCacheDescriptor = mockkPropertiesCacheDescriptor,
                temporaryItemPropertiesRepository = temporaryItemPropertiesRepository,
                ipfsService = mockkIpfsService,
                kittiesCacheDescriptor = mockk(),
                lootCacheDescriptor = mockk(),
                yInsureCacheDescriptor = mockk(),
                hegicCacheDescriptor = mockk(),
                hashmasksCacheDescriptor = mockk(),
                waifusionCacheDescriptor = mockk(),
                openSeaCacheDescriptor = mockk(),
                yInsureAddress = Address.FOUR().toString(),
                hegicAddress = Address.FOUR().toString(),
                hashmasksAddress = Address.FOUR().toString(),
                waifusionAddress = Address.FOUR().toString(),
                cacheService = null,
                properties = nftIndexerProperties,
                cryptoPunksMetaService = mockk()
            )
        }
    }
}
