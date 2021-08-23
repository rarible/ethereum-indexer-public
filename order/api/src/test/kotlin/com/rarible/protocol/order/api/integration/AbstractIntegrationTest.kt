package com.rarible.protocol.order.api.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.Blockchain
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.client.NoopWebClientCustomizer
import com.rarible.protocol.erc20.api.client.Erc20BalanceControllerApi
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.order.api.client.*
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import com.rarible.protocol.order.core.service.OrderUpdateService
import io.daonomic.rpc.domain.Request
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.domain.response.TransactionReceipt
import scalether.java.Lists
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoTransactionPoller
import java.net.URI
import javax.annotation.PostConstruct

abstract class AbstractIntegrationTest : BaseApiApplicationTest() {
    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    protected lateinit var erc20BalanceApi: Erc20BalanceControllerApi

    @Autowired
    protected lateinit var nftOwnershipApi: NftOwnershipControllerApi

    @Autowired
    protected lateinit var nftCollectionApi: NftCollectionControllerApi

    @Autowired
    protected lateinit var nftItemApi: NftItemControllerApi

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var ethereum: MonoEthereum

    @Autowired
    protected lateinit var poller: MonoTransactionPoller

    @Autowired
    protected lateinit var orderRepository: OrderRepository

    @Autowired
    protected lateinit var orderUpdateService: OrderUpdateService

    @Autowired
    protected lateinit var orderReduceService: OrderReduceService

    @Autowired
    protected lateinit var exchangeHistoryRepository: ExchangeHistoryRepository

    protected fun createMonoSigningTransactionSender(): MonoSigningTransactionSender {
        return openEthereumTest.signingTransactionSender()
    }

    protected fun createMonoTransactionPoller(): MonoTransactionPoller {
        return openEthereumTest.monoTransactionPoller()
    }

    private fun createEthereum(): MonoEthereum {
        return openEthereumTest.ethereum()
    }

    private suspend fun Mono<Word>.waitReceipt(): TransactionReceipt {
        val value = this.awaitFirst()
        require(value != null) { "txHash is null" }
        return  createEthereum().ethGetTransactionReceipt(value).awaitFirst().get()
    }

    protected suspend fun Mono<Word>.verifySuccess(): TransactionReceipt {
        val receipt = waitReceipt()
        Assertions.assertTrue(receipt.success()) {
            val result = createEthereum().executeRaw(
                Request(
                    1, "trace_replayTransaction", Lists.toScala(
                        receipt.transactionHash().toString(),
                        Lists.toScala("trace", "stateDiff")
                    ), "2.0"
                )
            ).block()!!
            "traces: ${result.result().get()}"
        }
        return receipt
    }

    protected suspend fun cancelOrder(orderHash: Word) {
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
                address = Address.ZERO(),
                topic = Word.apply(randomWord()),
                transactionHash = Word.apply(randomWord()),
                status = LogEventStatus.CONFIRMED,
                index = 0,
                logIndex = 0,
                minorLogIndex = 0
            )
        ).awaitFirst()
        orderReduceService.updateOrder(orderHash)
    }

    protected lateinit var orderClient: OrderControllerApi
    protected lateinit var encodeClient: OrderEncodeControllerApi
    protected lateinit var orderAggregationApi: OrderAggregationControllerApi
    protected lateinit var orderActivityClient: OrderActivityControllerApi
    protected lateinit var orderBidsClient: OrderBidControllerApi
    protected lateinit var transactionApi: OrderTransactionControllerApi

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    open fun setupDatabase() {
        mongo.collectionNames
            .filter { !it.startsWith("system") }
            .flatMap { mongo.remove(Query(), it) }
            .then().block()
    }

    @PostConstruct
    fun setup() {
        val urlProvider = FixedOrderIndexerApiServiceUriProvider(URI.create("http://127.0.0.1:$port"))
        val clientsFactory = OrderIndexerApiClientFactory(urlProvider, NoopWebClientCustomizer())

        orderActivityClient = clientsFactory.createOrderActivityApiClient(Blockchain.ETHEREUM.name)
        orderBidsClient = clientsFactory.createOrderBidApiClient(Blockchain.ETHEREUM.name)
        orderClient = clientsFactory.createOrderApiClient(Blockchain.ETHEREUM.name)
        encodeClient = clientsFactory.createOrderEncodeApiClient(Blockchain.ETHEREUM.name)
        orderAggregationApi = clientsFactory.createOrderAggregationApiClient(Blockchain.ETHEREUM.name)
        transactionApi = clientsFactory.createOrderTransactionApiClient(Blockchain.ETHEREUM.name)
    }
}
