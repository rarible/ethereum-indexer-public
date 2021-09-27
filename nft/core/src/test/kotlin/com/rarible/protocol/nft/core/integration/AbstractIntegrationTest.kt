package com.rarible.protocol.nft.core.integration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.dto.*
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.repository.TemporaryItemPropertiesRepository
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.domain.response.TransactionReceipt
import scalether.transaction.MonoTransactionPoller
import java.util.*

@FlowPreview
@Suppress("UNCHECKED_CAST")
abstract class AbstractIntegrationTest : BaseCoreTest() {
    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations
    @Autowired
    protected lateinit var tokenRepository: TokenRepository
    @Autowired
    protected lateinit var temporaryItemPropertiesRepository: TemporaryItemPropertiesRepository
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

    private lateinit var ownershipEventConsumer: RaribleKafkaConsumer<NftOwnershipEventDto>

    private lateinit var itemEventConsumer: RaribleKafkaConsumer<NftItemEventDto>

    @BeforeEach
    fun cleanDatabase() {
        mongo.collectionNames
            .filter { !it.startsWith("system") }
            .flatMap { mongo.remove(Query(), it) }
            .then().block()

        ownershipEventConsumer = createOwnershipEventConsumer()
        itemEventConsumer = createItemEventConsumer()
    }

    suspend fun <T> saveItemHistory(data: T, token: Address = AddressFactory.create(), transactionHash: Word = WordFactory.create(), logIndex: Int? = null, status: LogEventStatus = LogEventStatus.CONFIRMED): T {
        return nftItemHistoryRepository.save(
            LogEvent(
                data = data as EventData,
                address = token,
                topic = WordFactory.create(),
                transactionHash = transactionHash,
                status = status,
                index = 0,
                logIndex = logIndex,
                blockNumber = 1,
                minorLogIndex = 0)
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

    protected suspend fun checkItemEventWasPublished(
        token: Address,
        tokenId: EthUInt256,
        itemMeta: NftItemMetaDto,
        eventType: Class<out NftItemEventDto>
    ) = coroutineScope {
        val events = Collections.synchronizedList(ArrayList<KafkaMessage<NftItemEventDto>>())

        val job = async {
            itemEventConsumer
                .receive()
                .collect { events.add(it) }
        }
        Wait.waitAssert {
            assertThat(events)
                .hasSizeGreaterThanOrEqualTo(1)
                .satisfies { messages ->
                    val filteredEvents = messages.filter { message ->
                        when (val event = message.value) {
                            is NftItemUpdateEventDto -> {
                                event.item.contract == token
                                        && event.item.tokenId == tokenId.value
                                        && event.item.meta == itemMeta
                            }
                            is NftItemDeleteEventDto -> {
                                event.item.token == token && event.item.tokenId == tokenId.value
                            }
                        }
                    }
                    assertThat(filteredEvents).hasSize(1)
                    assertThat(filteredEvents.single().value).isInstanceOf(eventType)
                }
        }
        job.cancel()
    }

    protected suspend fun checkOwnershipEventWasPublished(
        token: Address,
        tokenId: EthUInt256,
        owner: Address,
        eventType: Class<out NftOwnershipEventDto>
    ) = coroutineScope {
        val events = Collections.synchronizedList(ArrayList<KafkaMessage<NftOwnershipEventDto>>())

        val job = async {
            ownershipEventConsumer
                .receive()
                .collect { events.add(it) }
        }
        Wait.waitAssert {
            assertThat(events)
                .hasSizeGreaterThanOrEqualTo(1)
                .satisfies { messages ->
                    val filteredEvents = messages.filter { message ->
                        when (val event = message.value) {
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
                    assertThat(filteredEvents).hasSize(1)
                    assertThat(filteredEvents.single().value).isInstanceOf(eventType)
                }
        }
        job.cancel()
    }

    protected suspend fun Mono<Word>.verifySuccess(): TransactionReceipt {
        val receipt = waitReceipt()
        Assertions.assertTrue(receipt.success())
        return receipt
    }

    protected suspend fun Mono<Word>.waitReceipt(): TransactionReceipt {
        val value = this.awaitFirstOrNull()
        require(value != null) { "txHash is null" }
        return ethereum.ethGetTransactionReceipt(value).awaitFirst().get()
    }
}
