package com.rarible.protocol.order.core.integration

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.PriceNormalizer
import io.daonomic.rpc.domain.Request
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.domain.response.TransactionReceipt
import scalether.java.Lists
import scalether.transaction.*
import java.math.BigInteger
import javax.annotation.PostConstruct

abstract class AbstractIntegrationTest : BaseCoreTest() {
    protected lateinit var sender: MonoTransactionSender

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    protected lateinit var exchangeHistoryRepository: ExchangeHistoryRepository

    @Autowired
    protected lateinit var orderRepository: OrderRepository

    @Autowired
    protected lateinit var orderReduceService: OrderReduceService

    @Autowired
    protected lateinit var orderUpdateService: OrderUpdateService

    @Autowired
    protected lateinit var ethereum: MonoEthereum

    @Autowired
    protected lateinit var poller: MonoTransactionPoller

    @Autowired
    protected lateinit var priceNormalizer: PriceNormalizer

    private fun Mono<Word>.waitReceipt(): TransactionReceipt {
        val value = this.block()
        require(value != null) { "txHash is null" }
        return ethereum.ethGetTransactionReceipt(value).block()!!.get()
    }

    @BeforeEach
    fun cleanDatabase() = runBlocking {
        mongo.collectionNames
            .filter { !it.startsWith("system") }
            .flatMap { mongo.remove(Query(), it) }
            .then().block()

        orderRepository.createIndexes()
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

    @Suppress("UNCHECKED_CAST")
    protected suspend fun <T> saveItemHistory(
        data: T,
        token: Address = AddressFactory.create(),
        transactionHash: Word = WordFactory.create(),
        logIndex: Int? = null,
        status: LogEventStatus = LogEventStatus.CONFIRMED
    ): T {
        if (data is OrderExchangeHistory) {
            val log = exchangeHistoryRepository.save(
                LogEvent(
                    data = data,
                    address = token,
                    topic = WordFactory.create(),
                    transactionHash = transactionHash,
                    status = status,
                    index = 0,
                    logIndex = logIndex,
                    minorLogIndex = 0
                )
            ).awaitFirst()

            return log.data as T
        }
        throw IllegalArgumentException("Unsupported history type")
    }
}
