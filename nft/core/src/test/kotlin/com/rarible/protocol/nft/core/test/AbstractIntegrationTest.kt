package com.rarible.protocol.nft.core.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.common.nowMillis
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.common.NewKeys
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemMetaEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.core.service.item.meta.ItemMetaResolver
import com.rarible.protocol.nft.core.service.item.meta.ItemMetaService
import com.rarible.protocol.nft.core.service.token.meta.descriptors.StandardTokenPropertiesResolver
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import io.mockk.clearMocks
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.web3jold.crypto.Keys
import org.web3jold.crypto.Sign
import org.web3jold.utils.Numeric
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.domain.response.TransactionReceipt
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import scalether.transaction.MonoTransactionPoller
import java.math.BigInteger
import java.time.Instant
import java.util.UUID
import java.util.function.Consumer

@Suppress("UNCHECKED_CAST")
abstract class AbstractIntegrationTest {

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    protected lateinit var tokenRepository: TokenRepository

    @Autowired
    @Qualifier("mockItemMetaResolver")
    protected lateinit var mockItemMetaResolver: ItemMetaResolver

    @Autowired
    @Qualifier("mockStandardTokenPropertiesResolver")
    protected lateinit var mockStandardTokenPropertiesResolver: StandardTokenPropertiesResolver

    @Autowired
    @Qualifier("mockExternalHttpClient")
    lateinit var mockExternalHttpClient: ExternalHttpClient

    @Autowired
    protected lateinit var itemMetaService: ItemMetaService

    @Autowired
    protected lateinit var itemRepository: ItemRepository

    @Autowired
    protected lateinit var nftItemHistoryRepository: NftItemHistoryRepository

    @Autowired
    protected lateinit var lazyNftItemHistoryRepository: LazyNftItemHistoryRepository

    @Autowired
    protected lateinit var nftIndexerProperties: NftIndexerProperties

    @Autowired
    private lateinit var application: ApplicationEnvironmentInfo

    @Autowired
    protected lateinit var properties: NftIndexerProperties

    @Autowired
    protected lateinit var ethereum: MonoEthereum

    @Autowired
    protected lateinit var poller: MonoTransactionPoller

    @Autowired
    protected lateinit var tokenHistoryRepository: NftHistoryRepository

    @Autowired
    protected lateinit var featureFlags: FeatureFlags

    @Autowired
    protected lateinit var itemEventHandler: TestKafkaHandler<NftItemEventDto>

    @Autowired
    protected lateinit var ownershipEventHandler: TestKafkaHandler<NftOwnershipEventDto>

    @Autowired
    protected lateinit var collectionEventHandler: TestKafkaHandler<NftCollectionEventDto>

    @Autowired
    protected lateinit var itemMetaEventHandler: TestKafkaHandler<NftItemMetaEventDto>

    @Autowired
    protected lateinit var eventHandlers: List<TestKafkaHandler<*>>

    @BeforeEach
    fun clearMock() {
        clearMocks(mockItemMetaResolver)
        clearMocks(mockExternalHttpClient)
        eventHandlers.forEach { it.clear() }
    }

    @BeforeEach
    fun cleanDatabase() {
        mongo.collectionNames
            .filter { !it.startsWith("system") }
            .flatMap { mongo.remove(Query(), it) }
            .then().block()
    }

    suspend fun <T> saveItemHistory(
        data: T,
        token: Address = randomAddress(),
        transactionHash: Word = Word.apply(randomWord()),
        logIndex: Int? = 0,
        status: LogEventStatus = LogEventStatus.CONFIRMED,
        from: Address = randomAddress(),
        blockTimestamp: Instant = nowMillis()
    ): T {
        return nftItemHistoryRepository.save(
            LogEvent(
                data = data as EventData,
                address = token,
                topic = WordFactory.create(),
                transactionHash = transactionHash,
                status = status,
                from = from,
                index = 0,
                logIndex = logIndex,
                blockNumber = 1,
                minorLogIndex = 0,
                blockTimestamp = blockTimestamp.epochSecond
            )
        ).awaitFirst().data as T
    }

    protected suspend fun checkItemEventWasPublished(
        token: Address,
        tokenId: EthUInt256,
        pendingSize: Int,
        eventType: Class<out NftItemEventDto>
    ) = coroutineScope {
        Wait.waitAssert {
            val itemEvents = itemEventHandler.events
            assertThat(itemEvents).hasSizeGreaterThanOrEqualTo(1)

            val filteredEvents = itemEvents.filter { event ->
                when (event) {
                    is NftItemUpdateEventDto -> {
                        event.item.contract == token &&
                            event.item.tokenId == tokenId.value &&
                            (event.item.pending?.size ?: 0) == pendingSize
                    }

                    is NftItemDeleteEventDto -> {
                        event.item.token == token && event.item.tokenId == tokenId.value
                    }
                }
            }

            assertThat(filteredEvents).hasSizeGreaterThanOrEqualTo(1)
            filteredEvents.forEach { assertThat(it).isInstanceOf(eventType) }
        }
    }

    protected suspend fun checkOwnershipEventWasPublished(
        token: Address,
        tokenId: EthUInt256,
        owner: Address,
        eventType: Class<out NftOwnershipEventDto>
    ) = coroutineScope {
        val ownershipEvents = ownershipEventHandler.events
        Wait.waitAssert {
            assertThat(ownershipEvents)
                .hasSizeGreaterThanOrEqualTo(1)
                .anyMatch { event ->
                    eventType.isInstance(event) && when (event) {
                        is NftOwnershipUpdateEventDto -> {
                            event.ownership.contract == token &&
                                event.ownership.tokenId == tokenId.value &&
                                event.ownership.owner == owner
                        }

                        is NftOwnershipDeleteEventDto -> {
                            event.ownership!!.token == token &&
                                event.ownership!!.tokenId == tokenId.value &&
                                event.ownership!!.owner == owner
                        }

                        else -> false
                    }
                }
        }
    }

    protected suspend fun checkCollectionWasPublished(
        expected: NftCollectionDto
    ) = coroutineScope {
        val collectionEvents = collectionEventHandler.events
        Wait.waitAssert {
            assertThat(collectionEvents).anySatisfy(Consumer { event ->
                assertThat(event).isInstanceOfSatisfying(NftCollectionUpdateEventDto::class.java) {
                    assertThat(it.collection).isEqualTo(expected)
                }
            })
        }
    }

    protected suspend fun Mono<Word>.verifySuccess(): TransactionReceipt {
        val receipt = waitReceipt()
        Assertions.assertTrue(receipt.success())
        return receipt
    }

    protected suspend fun TransactionReceipt.getTimestamp(): Instant =
        Instant.ofEpochSecond(ethereum.ethGetFullBlockByHash(blockHash()).map { it.timestamp() }.awaitFirst().toLong())

    private suspend fun Mono<Word>.waitReceipt(): TransactionReceipt {
        val value = this.awaitFirstOrNull()
        require(value != null) { "txHash is null" }
        return ethereum.ethGetTransactionReceipt(value).awaitFirst().get()
    }

    protected fun newSender(
        privateKey0: BigInteger? = null
    ): Triple<Address, MonoSigningTransactionSender, BigInteger> {
        val (privateKey, _, address) = generateNewKeys(privateKey0)
        val sender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }
        return Triple(address, sender, privateKey)
    }

    private fun generateNewKeys(privateKey0: BigInteger? = null): NewKeys {
        val privateKey = privateKey0 ?: Numeric.toBigInt(RandomUtils.nextBytes(32))
        val publicKey = Sign.publicKeyFromPrivate(privateKey)
        val signer = Address.apply(Keys.getAddressFromPrivateKey(privateKey))
        return NewKeys(privateKey, publicKey, signer)
    }

    protected fun createToken(): Token {
        return Token(
            id = AddressFactory.create(),
            owner = AddressFactory.create(),
            name = UUID.randomUUID().toString(),
            symbol = UUID.randomUUID().toString(),
            status = ContractStatus.values().random(),
            features = (1..10).map { TokenFeature.values().random() }.toSet(),
            standard = TokenStandard.values().random(),
            version = null
        )
    }
}
