package com.rarible.protocol.block.scanner.test

import com.rarible.blockchain.scanner.ethereum.configuration.EthereumScannerProperties
import com.rarible.blockchain.scanner.framework.data.BlockEvent
import com.rarible.blockchain.scanner.util.getBlockTopic
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.core.test.ext.KafkaTestExtension.Companion.kafkaContainer
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.common.NewKeys
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.domain.request.Transaction
import scalether.domain.response.TransactionReceipt
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import scalether.transaction.MonoTransactionPoller
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger
import java.util.concurrent.CopyOnWriteArrayList
import javax.annotation.PostConstruct

@DelicateCoroutinesApi
abstract class AbstractIntegrationTest {
    private lateinit var sender: MonoTransactionSender

    @Autowired
    protected lateinit var application: ApplicationEnvironmentInfo

    @Autowired
    protected lateinit var scannerProperties: EthereumScannerProperties

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    protected lateinit var ethereum: MonoEthereum

    @Autowired
    protected lateinit var poller: MonoTransactionPoller

    private lateinit var consumer: RaribleKafkaConsumer<BlockEvent>

    private lateinit var consumingJob: Job

    private val blockEvents = CopyOnWriteArrayList<BlockEvent>()

    private fun Mono<Word>.waitReceipt(): TransactionReceipt {
        val value = this.block()
        require(value != null) { "txHash is null" }
        return ethereum.ethGetTransactionReceipt(value).block()!!.get()
    }

    @BeforeEach
    fun startConsumers() {
        consumingJob = GlobalScope.launch {
            consumer
                .receive()
                .collect { blockEvents.add(it.value) }
        }
    }

    @AfterEach
    fun stopConsumers() = runBlocking {
        consumingJob.cancelAndJoin()
    }

    @BeforeEach
    fun cleanDatabase() = runBlocking<Unit> {
        delay(300)

        mongo.collectionNames
            .filter { !it.startsWith("system") }
            .flatMap { mongo.remove(Query(), it) }
            .then().awaitFirstOrNull()
    }

    @PostConstruct
    fun setUp() {
        sender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            Numeric.toBigInt("0x0a2853fac2c0a03f463f04c4567839473c93f3307da459132b7dd1ca633c0e16"),
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }

        consumer = createConsumer()
    }

    protected fun depositTransaction(to: Address, amount: BigInteger): TransactionReceipt {
        val coinBaseWalletPrivateKey = BigInteger(
            Numeric.hexStringToByteArray("00120de4b1518cf1f16dc1b02f6b4a8ac29e870174cb1d8575f578480930250a")
        )
        val (coinBaseAddress, coinBaseSender) = newSender(coinBaseWalletPrivateKey)

        return coinBaseSender.sendTransaction(
            Transaction(
                to,
                coinBaseAddress,
                BigInteger.valueOf(8000000),
                BigInteger.ZERO,
                amount,
                Binary(ByteArray(1)),
                null
            )
        ).waitReceipt()
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

    private fun createConsumer(): RaribleKafkaConsumer<BlockEvent> {
        return RaribleKafkaConsumer(
            clientId = "test-consumer-block-event",
            consumerGroup = "test-group-block-event",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = BlockEvent::class.java,
            defaultTopic = getBlockTopic(application.name, scannerProperties.service, scannerProperties.blockchain),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.LATEST
        )
    }

    protected suspend fun checkBlockEventWasPublished(asserter: BlockEvent.() -> Unit) = coroutineScope {
        Wait.waitAssert {
            assertThat(blockEvents)
                .hasSizeGreaterThanOrEqualTo(1)
                .anySatisfy { it.asserter() }
        }
    }
}
