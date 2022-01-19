package com.rarible.protocol.nft.core.integration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.common.NewKeys
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionEventTopicProvider
import com.rarible.protocol.dto.NftCollectionMetaDto
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemEventTopicProvider
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipEventTopicProvider
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.token.TokenReduceService
import com.rarible.protocol.nft.core.service.token.meta.descriptors.StandardTokenPropertiesResolver
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import io.mockk.clearMocks
import io.mockk.every
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
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
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.EmptyCoroutineContext

@FlowPreview
@Suppress("UNCHECKED_CAST")
abstract class AbstractIntegrationTest : BaseCoreTest() {
    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    protected lateinit var tokenRepository: TokenRepository

    @Autowired
    @Qualifier("mockItemPropertiesResolver")
    protected lateinit var mockItemPropertiesResolver: ItemPropertiesResolver

    @Autowired
    @Qualifier("mockStandardTokenPropertiesResolver")
    protected lateinit var mockStandardTokenPropertiesResolver: StandardTokenPropertiesResolver

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
    private lateinit var properties: NftIndexerProperties

    @Autowired
    protected lateinit var ethereum: MonoEthereum

    @Autowired
    protected lateinit var poller: MonoTransactionPoller

    @Autowired
    protected lateinit var tokenReduceService: TokenReduceService

    @Autowired
    protected lateinit var tokenHistoryRepository: NftHistoryRepository

    @Autowired
    private lateinit var featureFlags: FeatureFlags

    private lateinit var ownershipEventConsumer: RaribleKafkaConsumer<NftOwnershipEventDto>

    private lateinit var itemEventConsumer: RaribleKafkaConsumer<NftItemEventDto>

    private lateinit var collectionEventConsumer: RaribleKafkaConsumer<NftCollectionEventDto>

    private val itemEvents = CopyOnWriteArrayList<NftItemEventDto>()
    private val ownershipEvents = CopyOnWriteArrayList<NftOwnershipEventDto>()
    private val collectionEvents = CopyOnWriteArrayList<NftCollectionEventDto>()

    private lateinit var consumingJobs: List<Job>

    @BeforeEach
    fun clearMock() {
        clearMocks(mockItemPropertiesResolver)
        every { mockItemPropertiesResolver.name } returns "MockResolver"
        every { mockItemPropertiesResolver.canBeCached } returns true
    }

    @BeforeEach
    fun cleanDatabase() {
        mongo.collectionNames
            .filter { !it.startsWith("system") }
            .flatMap { mongo.remove(Query(), it) }
            .then().block()
    }

    @BeforeEach
    fun setUpEventConsumers() {
        itemEventConsumer = createItemEventConsumer()
        collectionEventConsumer = createCollectionEventConsumer()
        ownershipEventConsumer = createOwnershipEventConsumer()
        consumingJobs = listOf(
            GlobalScope.launch {
                itemEventConsumer.receiveAutoAck().collect {
                    itemEvents += it.value
                }
            },
            GlobalScope.launch {
                createOwnershipEventConsumer().receiveAutoAck().collect {
                    ownershipEvents += it.value
                }
            },
            GlobalScope.launch {
                collectionEventConsumer.receiveAutoAck().collect {
                    collectionEvents += it.value
                }
            }
        )
    }

    @AfterEach
    fun stopConsumers() = runBlocking {
        consumingJobs.forEach { it.cancelAndJoin() }
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

    private fun createOwnershipEventConsumer(): RaribleKafkaConsumer<NftOwnershipEventDto> {
        return RaribleKafkaConsumer(
            clientId = "test-consumer-ownership-event",
            consumerGroup = "test-group-ownership-event",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = NftOwnershipEventDto::class.java,
            defaultTopic = NftOwnershipEventTopicProvider.getTopic(application.name, properties.blockchain.value),
            bootstrapServers = properties.kafkaReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    private fun createItemEventConsumer(): RaribleKafkaConsumer<NftItemEventDto> {
        return RaribleKafkaConsumer(
            clientId = "test-consumer-item-event",
            consumerGroup = "test-group-item-event",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = NftItemEventDto::class.java,
            defaultTopic = NftItemEventTopicProvider.getTopic(application.name, properties.blockchain.value),
            bootstrapServers = properties.kafkaReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    private fun createCollectionEventConsumer(): RaribleKafkaConsumer<NftCollectionEventDto> {
        return RaribleKafkaConsumer(
            clientId = "test-consumer-collection-event",
            consumerGroup = "test-group-collection-event",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = NftCollectionEventDto::class.java,
            defaultTopic = NftCollectionEventTopicProvider.getTopic(application.name, properties.blockchain.value),
            bootstrapServers = properties.kafkaReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    protected suspend fun checkItemEventWasPublished(
        token: Address,
        tokenId: EthUInt256,
        itemMeta: NftItemMetaDto,
        pendingSize: Int,
        eventType: Class<out NftItemEventDto>
    ) = coroutineScope {
        Wait.waitAssert {
            assertThat(itemEvents)
                .hasSizeGreaterThanOrEqualTo(1)
                .satisfies { events ->
                    val filteredEvents = events.filter { event ->
                        when (event) {
                            is NftItemUpdateEventDto -> {
                                event.item.contract == token
                                        && event.item.tokenId == tokenId.value
                                        && event.item.meta == itemMeta
                                        && (event.item.pending?.size ?: 0) == pendingSize
                            }
                            is NftItemDeleteEventDto -> {
                                event.item.token == token && event.item.tokenId == tokenId.value
                            }
                        }
                    }
                    assertThat(filteredEvents)
                        .hasSizeGreaterThanOrEqualTo(1)
                        .allSatisfy { assertThat(it).isInstanceOf(eventType) }
                }
        }
    }

    protected suspend fun checkOwnershipEventWasPublished(
        token: Address,
        tokenId: EthUInt256,
        owner: Address,
        eventType: Class<out NftOwnershipEventDto>
    ) = coroutineScope {
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
                            event.ownership.token == token &&
                                    event.ownership.tokenId == tokenId.value &&
                                    event.ownership.owner == owner
                        }
                    }
                }
        }
    }

    protected suspend fun checkMetaWasPublished(
        meta: NftCollectionMetaDto
    ) = coroutineScope {
        Wait.waitAssert {
            assertThat(collectionEvents).anyMatch {
                it is NftCollectionUpdateEventDto && it.collection.meta == meta
            }
        }
    }

    protected suspend fun Mono<Word>.verifySuccess(): TransactionReceipt {
        val receipt = waitReceipt()
        Assertions.assertTrue(receipt.success())
        return receipt
    }

    protected suspend fun TransactionReceipt.getTimestamp(): Instant =
        Instant.ofEpochSecond(ethereum.ethGetFullBlockByHash(blockHash()).map { it.timestamp() }.awaitFirst().toLong())


    protected suspend fun Mono<Word>.waitReceipt(): TransactionReceipt {
        val value = this.awaitFirstOrNull()
        require(value != null) { "txHash is null" }
        return ethereum.ethGetTransactionReceipt(value).awaitFirst().get()
    }

    protected fun newSender(privateKey0: BigInteger? = null): Triple<Address, MonoSigningTransactionSender, BigInteger> {
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

    fun <T> withReducer(version: ReduceVersion, block: suspend CoroutineScope.() -> T) {
        featureFlags.reduceVersion = version
        runBlocking(EmptyCoroutineContext, block)
    }

    protected fun createToken(): Token {
        return Token(
            id = AddressFactory.create(),
            owner = AddressFactory.create(),
            name = UUID.randomUUID().toString(),
            symbol = UUID.randomUUID().toString(),
            status = ContractStatus.values().random(),
            features = (1..10).map {  TokenFeature.values().random() }.toSet(),
            standard = TokenStandard.values().random(),
            version = null
        )
    }
}
