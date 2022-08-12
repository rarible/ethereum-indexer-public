package com.rarible.protocol.order.listener.integration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.ext.KafkaTestExtension.Companion.kafkaContainer
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.common.NewKeys
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.BlockHead
import com.rarible.ethereum.listener.log.domain.BlockStatus
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.ethereum.listener.log.persist.BlockRepository
import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.dto.ActivityDto
import com.rarible.protocol.dto.ActivityTopicProvider
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.toWord
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.repository.approval.ApprovalHistoryRepository
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.asset.AssetBalanceProvider
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Request
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.core.MonoEthereum
import scalether.core.MonoParity
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.domain.request.Transaction
import scalether.domain.response.TransactionReceipt
import scalether.java.Lists
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import scalether.transaction.MonoTransactionPoller
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import javax.annotation.PostConstruct

abstract class AbstractIntegrationTest : BaseListenerApplicationTest() {

    protected val logger = LoggerFactory.getLogger(javaClass)
    protected lateinit var sender: MonoTransactionSender

    @Autowired
    protected lateinit var application: ApplicationEnvironmentInfo

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    protected lateinit var exchangeHistoryRepository: ExchangeHistoryRepository

    @Autowired
    protected lateinit var orderRepository: OrderRepository

    @Autowired
    protected lateinit var orderVersionRepository: OrderVersionRepository

    @Autowired
    protected lateinit var orderIndexerProperties: OrderIndexerProperties

    @Autowired
    protected lateinit var transferProxyAddresses: OrderIndexerProperties.TransferProxyAddresses

    @Autowired
    protected lateinit var exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses

    @Autowired
    protected lateinit var orderReduceService: OrderReduceService

    @Autowired
    protected lateinit var orderUpdateService: OrderUpdateService

    @Autowired
    protected lateinit var ethereum: MonoEthereum

    @Autowired
    protected lateinit var poller: MonoTransactionPoller

    @Autowired
    protected lateinit var assetBalanceProvider: AssetBalanceProvider

    @Autowired
    protected lateinit var erc1271SignService: ERC1271SignService

    @Autowired
    lateinit var currencyApi: CurrencyControllerApi

    @Autowired
    lateinit var meterRegistry: MeterRegistry

    @Autowired
    lateinit var blockRepository: BlockRepository

    @Autowired
    lateinit var approvalHistoryRepository: ApprovalHistoryRepository

    protected lateinit var parity: MonoParity

    protected lateinit var consumer: RaribleKafkaConsumer<ActivityDto>

    private lateinit var consumingJob: Job

    private val activities = CopyOnWriteArrayList<ActivityDto>()

    private fun Mono<Word>.waitReceipt(): TransactionReceipt {
        val value = this.block()
        require(value != null) { "txHash is null" }
        return ethereum.ethGetTransactionReceipt(value).block()!!.get()
    }

    @BeforeEach
    fun startConsumers() {
        consumingJob = GlobalScope.launch {
            consumer
                .receiveAutoAck()
                .collect { activities.add(it.value) }
        }
    }

    @AfterEach
    fun stopConsumers() = runBlocking {
        consumingJob.cancelAndJoin()
    }

    @BeforeEach
    fun cleanDatabase() = runBlocking<Unit> {
        //TODO: previous tests (when running the whole package) might not have finished before the next test starts.
        // Such activities might insert the old order right after cleaning the database here.
        // We need a proper way of ending activities in the tests.
        delay(300)

        mongo.collectionNames
            .filter { !it.startsWith("system") }
            .flatMap { mongo.remove(Query(), it) }
            .then().awaitFirstOrNull()

        orderRepository.createIndexes()
        orderRepository.dropIndexes()
        orderVersionRepository.createIndexes()
        orderVersionRepository.dropIndexes()
        exchangeHistoryRepository.createIndexes()
        exchangeHistoryRepository.dropIndexes()

        ignorePreviousNodeBlocks()
    }

    /**
     * Insert the current node's block as the latest known to ignore all blocks from previous tests.
     */
    private fun ignorePreviousNodeBlocks() = runBlocking<Unit> {
        val currentBlockNumber = ethereum.ethBlockNumber().awaitFirst()
        val currentBlock = ethereum.ethGetBlockByNumber(currentBlockNumber).awaitFirst()
        try {
            val blockHead = BlockHead(
                id = currentBlock.blockNumber.toLong(),
                hash = currentBlock.hash(),
                timestamp = currentBlock.timestamp().toLong(),
                status = BlockStatus.SUCCESS
            )
            blockRepository.save(blockHead)
        } catch (ex: Throwable) {
        }
    }

    @BeforeEach
    fun setUpMocks() {
        clearMocks(currencyApi)
        every { currencyApi.getCurrencyRate(any(), any(), any()) } returns CurrencyRateDto(
            "test",
            "usd",
            TestPropertiesConfiguration.ETH_CURRENCY_RATE,
            nowMillis()
        ).toMono()

        clearMocks(assetBalanceProvider)
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } coAnswers r@ {
            val asset = secondArg<Asset>()
            if (asset.type is EthAssetType) {
                return@r MakeBalanceState(asset.value)
            }
            return@r MakeBalanceState(EthUInt256.ONE)
        }

        clearMocks(erc1271SignService)
        coEvery { erc1271SignService.isSigner(any(), any() as Word, any()) } returns true
    }

    @PostConstruct
    fun setUp() {
        sender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            Numeric.toBigInt("0x0a2853fac2c0a03f463f04c4567839473c93f3307da459132b7dd1ca633c0e16"),
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        consumer = createConsumer()
    }

    protected fun Mono<Word>.verifyError(): TransactionReceipt {
        val receipt = waitReceipt()
        Assertions.assertFalse(receipt.success())
        return receipt
    }

    protected fun Mono<Word>.verifySuccess(): TransactionReceipt {
        val receipt = waitReceipt()
        Assertions.assertTrue(receipt.success()) {
            val result = ethereum.executeRaw(Request(1, "trace_replayTransaction", Lists.toScala(
                receipt.transactionHash().toString(),
                Lists.toScala("trace", "stateDiff")
            ), "2.0")).block()!!
            "traces: ${result.result().get()}"
        }
        return receipt
    }

    protected suspend fun TransactionReceipt.getTimestamp(): Instant =
        Instant.ofEpochSecond(ethereum.ethGetFullBlockByHash(blockHash()).map { it.timestamp() }.awaitFirst().toLong())

    protected fun depositInitialBalance(to: Address, amount: BigInteger) {
        val coinBaseWalletPrivateKey =
            BigInteger(Numeric.hexStringToByteArray("00120de4b1518cf1f16dc1b02f6b4a8ac29e870174cb1d8575f578480930250a"))
        val (coinBaseAddress, coinBaseSender) = newSender(coinBaseWalletPrivateKey)
        coinBaseSender.sendTransaction(
            Transaction(
                to,
                coinBaseAddress,
                BigInteger.valueOf(8000000),
                BigInteger.ZERO,
                amount,
                Binary(ByteArray(1)),
                null
            )
        ).verifySuccess()
    }

    protected fun newSender(privateKey0: BigInteger? = null): Triple<Address, MonoSigningTransactionSender, BigInteger> {
        val (privateKey, _, address) = generateNewKeys(privateKey0)
        val sender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )
        return Triple(address, sender, privateKey)
    }

    protected fun generateNewKeys(privateKey0: BigInteger? = null): NewKeys {
        val privateKey = privateKey0 ?: Numeric.toBigInt(RandomUtils.nextBytes(32))
        val publicKey = Sign.publicKeyFromPrivate(privateKey)
        val signer = Address.apply(Keys.getAddressFromPrivateKey(privateKey))
        return NewKeys(privateKey, publicKey, signer)
    }

    protected suspend fun getEthBalance(account: Address): BigInteger =
        ethereum.ethGetBalance(account, "latest").awaitFirst()

    protected suspend fun <T> saveItemHistory(data: T, token: Address = AddressFactory.create(), transactionHash: Word = WordFactory.create(), logIndex: Int? = null, status: LogEventStatus = LogEventStatus.CONFIRMED): T {
        if (data is OrderExchangeHistory) {
            val log = exchangeHistoryRepository.save(LogEvent(data = data, address = token, topic = WordFactory.create(), transactionHash = transactionHash, status = status, index = 0, logIndex = logIndex, minorLogIndex = 0)).awaitFirst()
            return log.data as T
        }
        throw IllegalArgumentException("Unsupported history type")
    }

    private fun createConsumer(): RaribleKafkaConsumer<ActivityDto> {
        return RaribleKafkaConsumer(
            clientId = "test-consumer-order-activity",
            consumerGroup = "test-group-order-activity",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = ActivityDto::class.java,
            defaultTopic = ActivityTopicProvider.getTopic(application.name, orderIndexerProperties.blockchain.name.toLowerCase()),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    protected suspend fun cancelOrder(
        orderHash: Word,
        contractAddress: Address = Address.ZERO(),
        topic: Word = 0.toBigInteger().toWord()
    ) {
        exchangeHistoryRepository.save(
            LogEvent(
                data = OrderCancel(
                    hash = orderHash,
                    date = nowMillis(),

                    // Do not matter.
                    maker = null,
                    make = null,
                    take = null,
                    source = HistorySource.RARIBLE
                ),
                address = contractAddress,
                topic = topic,
                transactionHash = Word.apply(randomWord()),
                status = LogEventStatus.CONFIRMED,
                index = 0,
                logIndex = 0,
                minorLogIndex = 0
            )
        ).awaitFirst()
        orderReduceService.updateOrder(orderHash)
    }

    protected suspend fun updateOrderMakeStock(orderHash: Word, makeBalance: EthUInt256) {
        orderUpdateService.updateMakeStock(orderHash, MakeBalanceState(makeBalance))
    }

    protected suspend fun checkActivityWasPublished(asserter: ActivityDto.() -> Unit) = coroutineScope {
        Wait.waitAssert {
            assertThat(activities)
                .hasSizeGreaterThanOrEqualTo(1)
                .anySatisfy { it.asserter() }
        }
    }
}
