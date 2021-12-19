package com.rarible.protocol.block.scanner.test

import com.rarible.blockchain.scanner.framework.data.BlockEvent
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.apache.commons.lang3.RandomUtils
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import scalether.transaction.MonoTransactionPoller
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger
import javax.annotation.PostConstruct

@FlowPreview
abstract class AbstractIntegrationTest {
    private lateinit var sender: MonoTransactionSender

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    protected lateinit var ethereum: MonoEthereum

    @Autowired
    protected lateinit var poller: MonoTransactionPoller

    @Autowired
    private lateinit var application: ApplicationEnvironmentInfo

    private lateinit var blockConsumer: RaribleKafkaConsumer<BlockEvent>

    @BeforeEach
    fun cleanDatabase() {
        mongo.collectionNames
            .filter { !it.startsWith("system") }
            .flatMap { mongo.remove(Query(), it) }
            .then().block()

        //blockConsumer = createNftActivityConsumer()
    }

    @PostConstruct
    fun setUp() {
        sender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            Numeric.toBigInt("0x0a2853fac2c0a03f463f04c4567839473c93f3307da459132b7dd1ca633c0e16"),
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }
    }

//    protected suspend fun Mono<Word>.verifySuccess(): TransactionReceipt {
//        val receipt = waitReceipt()
//        Assertions.assertTrue(receipt.success())
//        return receipt
//    }
//
//    protected fun newSender(privateKey0: BigInteger? = null): Pair<Address, MonoSigningTransactionSender> {
//        val (privateKey, _, address) = generateNewKeys(privateKey0)
//        val sender = MonoSigningTransactionSender(
//            ethereum,
//            MonoSimpleNonceProvider(ethereum),
//            privateKey,
//            BigInteger.valueOf(8000000),
//            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
//        )
//        return address to sender
//    }
//
//    private fun generateNewKeys(privateKey0: BigInteger? = null): NewKeys {
//        val privateKey = privateKey0 ?: Numeric.toBigInt(RandomUtils.nextBytes(32))
//        val publicKey = Sign.publicKeyFromPrivate(privateKey)
//        val signer = Address.apply(Keys.getAddressFromPrivateKey(privateKey))
//        return NewKeys(privateKey, publicKey, signer)
//    }
//
//    private suspend fun Mono<Word>.waitReceipt(): TransactionReceipt {
//        val value = this.awaitFirstOrNull()
//        require(value != null) { "txHash is null" }
//        return ethereum.ethGetTransactionReceipt(value).awaitFirst().get()
//    }
//
//    private fun createNftActivityConsumer(): RaribleKafkaConsumer<BlockEvent> {
//        return RaribleKafkaConsumer(
//            clientId = "test-consumer-order-activity",
//            consumerGroup = "test-group-order-activity",
//            valueDeserializerClass = JsonDeserializer::class.java,
//            valueClass = BlockEvent::class.java,
//            defaultTopic = ActivityTopicProvider.getTopic(application.name, nftIndexerProperties.blockchain.value),
//            bootstrapServers = nftIndexerProperties.kafkaReplicaSet,
//            offsetResetStrategy = OffsetResetStrategy.EARLIEST
//        )
//    }
//
//    protected suspend fun TransactionReceipt.getTimestamp(): Instant =
//        Instant.ofEpochSecond(ethereum.ethGetFullBlockByHash(blockHash()).map { it.timestamp() }.awaitFirst().toLong())
//
//    protected suspend fun checkActivityWasPublished(
//        token: Address,
//        tokenId: EthUInt256,
//        topic: Word,
//        activityType: Class<out NftActivityDto>
//    ) = coroutineScope {
//        val logEvent = nftItemHistoryRepository.findItemsHistory(token, tokenId)
//            .filter { it.log.topic == topic }
//            .map { it.log }
//            .awaitFirstOrNull()
//
//        assertThat(logEvent).isNotNull
//
//        val events = CopyOnWriteArrayList<KafkaMessage<ActivityDto>>()
//
//        val job = async {
//           activityConsumer
//                .receive()
//                .collect { events.add(it) }
//        }
//        Wait.waitAssert {
//            assertThat(events)
//                .hasSizeGreaterThanOrEqualTo(1)
//                .satisfies {
//                    val event = it.firstOrNull { event -> event.value.id == logEvent?.id.toString() }
//                    val activity = event?.value
//                    assertThat(activity?.javaClass).isEqualTo(activityType)
//
//                    when (activity) {
//                        is MintDto -> {
//                            assertThat(activity.id).isEqualTo(logEvent?.id.toString())
//                            assertThat(activity.contract).isEqualTo(token)
//                            assertThat(activity.tokenId).isEqualTo(tokenId.value)
//                        }
//                        is TransferDto -> {
//                            assertThat(activity.id).isEqualTo(logEvent?.id.toString())
//                            assertThat(activity.contract).isEqualTo(token)
//                            assertThat(activity.tokenId).isEqualTo(tokenId.value)
//                        }
//                        else -> Assertions.fail<String>("Unexpected event type ${activity?.javaClass}")
//                    }
//                }
//        }
//        job.cancel()
//    }
}
