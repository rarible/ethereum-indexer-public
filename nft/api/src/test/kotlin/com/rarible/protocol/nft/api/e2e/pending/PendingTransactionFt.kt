package com.rarible.protocol.nft.api.e2e.pending

import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.contracts.erc721.v3.MintableOwnableToken
import com.rarible.protocol.contracts.erc721.v4.rarible.MintableToken
import com.rarible.protocol.dto.CreateTransactionRequestDto
import com.rarible.protocol.dto.LogEventDto
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.misc.SignUtils
import com.rarible.protocol.nft.api.service.item.meta.IpfsService
import com.rarible.protocol.nft.api.service.item.meta.ItemPropertiesService
import com.rarible.protocol.nft.api.service.item.meta.PropertiesCacheDescriptor
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.repository.TemporaryItemPropertiesRepository
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
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
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Transaction
import scalether.domain.response.TransactionReceipt
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger
import java.util.*

@End2EndTest
@Import(PendingTransactionFt.MockItemPropertiesServiceConfiguration::class)
class PendingTransactionFt : SpringContainerBaseTest() {
    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Autowired
    private lateinit var nftHistoryRepository: NftHistoryRepository

    @Autowired
    private lateinit var propertiesCacheDescriptor: PropertiesCacheDescriptor

    @Autowired
    private lateinit var  ipfsService: IpfsService

    @Autowired
    private lateinit var temporaryItemPropertiesRepository: TemporaryItemPropertiesRepository

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
    fun mint() = runBlocking {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val address = Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        val token = MintableToken.deployAndWait(userSender, poller, "TEST", "TST", userSender.from(), "https://ipfs", "https://ipfs").block()!!
        val nonce = SignUtils.sign(privateKey, 0, token.address())
        tokenRepository.save(Token(token.address(), name = "TEST", standard = TokenStandard.ERC721)).awaitFirst()

        val receipt = token.mint(BigInteger.valueOf(nonce.value), nonce.v.toEth(), nonce.r.bytes(), nonce.s.bytes(), emptyArray(), "/ipfs/QmdLV7nFFpE6HUZQAcvZD2HPqTTGmGQ9pmot63ZgPAUPBg").execute().verifySuccess()

        processTransaction(receipt)

        Wait.waitAssert {
            val item = itemRepository.search(Query()).first()
            assertThat(item.owners.single()).isEqualTo(address)
            assertThat(item.creators.single().account).isEqualTo(address)

            assertThat(item).hasFieldOrPropertyWithValue(Item::supply.name, EthUInt256.ZERO)
            assertThat(item.pending).hasSize(1)

            val savedProperties = temporaryItemPropertiesRepository.findById(item.id.decimalStringValue).awaitFirst()
            assertThat(savedProperties.value.name).isEqualTo(temporaryProperties.name)
        }
    }

    @Test
    fun simpleMintAndTransfer() = runBlocking {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val address = Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        val sender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        val contract = TestERC721.deployAndWait(sender, poller, "TEST", "TEST").awaitFirst()
        tokenRepository.save(Token(contract.address(), name = "TEST", standard = TokenStandard.ERC721)).awaitFirst()

        val receipt = contract.mint(address, BigInteger.ONE, "").execute().verifySuccess()
        processTransaction(receipt)

        Wait.waitAssert {
            val item = itemRepository.search(Query()).first()
            assertThat(item).hasFieldOrPropertyWithValue(Item::supply.name, EthUInt256.ZERO)
            assertThat(item.pending).hasSize(1)
        }
    }

    @Test
    fun createCollectionTransaction() = runBlocking<Unit> {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val signer = Address.apply(RandomUtils.nextBytes(20))

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )
        val contractDeploy = MintableOwnableToken.deploy(userSender, "Test", "TST", "https://ipfs/", "https://ipfs/", signer)
        val receipt = poller.waitForTransaction(contractDeploy).awaitFirst()
        processTransaction(receipt, isCollection = true)

        Wait.waitAssert {
            val tokens = tokenRepository.findAll().collectList().awaitFirst()
            assertThat(tokens).hasSize(1)

            with(tokens.single()) {
                assertThat(id).isEqualTo(receipt.contractAddress())
                assertThat(name).isEqualTo("Test")
                assertThat(symbol).isEqualTo("TST")
                assertThat(status).isEqualTo(ContractStatus.PENDING)
            }
        }

        val history = nftHistoryRepository.find(receipt.contractAddress()).collectList().awaitFirst()
        assertThat(history).hasSize(1)
        assertThat(history.single().first.status).isEqualTo(LogEventStatus.PENDING)
    }

    private suspend fun processTransaction(receipt: TransactionReceipt, isCollection: Boolean = false) {
        val tx = ethereum.ethGetTransactionByHash(receipt.transactionHash()).awaitFirst().get()

        val eventLogs = nftTransactionApiClient.createNftPendingTransaction(tx.toRequest()).collectList().awaitFirst()

        assertThat(eventLogs).hasSize(1)

        with(eventLogs.single()) {
            assertThat(transactionHash).isEqualTo(tx.hash())
            assertThat(address).isEqualTo(if (isCollection) tx.creates() else tx.to())
            assertThat(status).isEqualTo(LogEventDto.Status.PENDING)
        }
    }

    protected suspend fun Mono<Word>.verifySuccess(): TransactionReceipt {
        val receipt = waitReceipt()
        Assertions.assertTrue(receipt.success())
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
                yInsureCacheDescriptor = mockk(),
                hegicCacheDescriptor = mockk(),
                hashmasksCacheDescriptor = mockk(),
                waifusionCacheDescriptor = mockk(),
                openseaClient = mockk(),
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
