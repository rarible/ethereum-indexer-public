package com.rarible.protocol.nft.api.e2e

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.client.NoopWebClientCustomizer
import com.rarible.protocol.nft.api.client.FixedNftIndexerApiServiceUriProvider
import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftIndexerApiClientFactory
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftLazyMintControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.nft.api.client.NftTransactionControllerApi
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import io.daonomic.rpc.domain.Request
import io.daonomic.rpc.domain.Word
import io.mockk.clearMocks
import io.mockk.every
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.springframework.web.reactive.function.client.WebClient
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.domain.response.TransactionReceipt
import scalether.java.Lists
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import scalether.transaction.MonoTransactionPoller
import java.math.BigInteger
import java.net.URI
import javax.annotation.PostConstruct

abstract class SpringContainerBaseTest {
    init {
        System.setProperty(
            "common.blockchain", Blockchain.ETHEREUM.name.toLowerCase()
        )
        System.setProperty(
            "api.operator.private-key", "0x0000000000000000000000000000000000000000000000000000000000000000"
        )
        System.setProperty(
            "spring.data.mongodb.database", "protocol"
        )
    }

    protected lateinit var nftItemApiClient: NftItemControllerApi
    protected lateinit var nftOwnershipApiClient: NftOwnershipControllerApi
    protected lateinit var nftCollectionApiClient: NftCollectionControllerApi
    protected lateinit var nftLazyMintApiClient: NftLazyMintControllerApi
    protected lateinit var nftTransactionApiClient: NftTransactionControllerApi
    protected lateinit var nftActivityApiClient: NftActivityControllerApi
    protected lateinit var webClient: WebClient

    protected lateinit var poller: MonoTransactionPoller

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    protected lateinit var ethereum: MonoEthereum

    @Autowired
    @Qualifier("mockItemPropertiesResolver")
    protected lateinit var mockItemPropertiesResolver: ItemPropertiesResolver

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    open fun setupDatabase() {
        mongo.collectionNames
            .filter { !it.startsWith("system") }
            .flatMap { mongo.remove(Query(), it) }
            .then().block()
    }

    @BeforeEach
    fun clear() {
        clearMocks(mockItemPropertiesResolver)
        every { mockItemPropertiesResolver.name } returns "MockResolver"
        every { mockItemPropertiesResolver.canBeCached } returns true
    }

    @PostConstruct
    fun setup() {
        val urlProvider = FixedNftIndexerApiServiceUriProvider(URI.create("http://127.0.0.1:$port"))
        val clientFactory = NftIndexerApiClientFactory(urlProvider, NoopWebClientCustomizer())

        nftItemApiClient = clientFactory.createNftItemApiClient(Blockchain.ETHEREUM.name)
        nftOwnershipApiClient = clientFactory.createNftOwnershipApiClient(Blockchain.ETHEREUM.name)
        nftCollectionApiClient = clientFactory.createNftCollectionApiClient(Blockchain.ETHEREUM.name)
        nftLazyMintApiClient = clientFactory.createNftMintApiClient(Blockchain.ETHEREUM.name)
        nftTransactionApiClient = clientFactory.createNftTransactionApiClient(Blockchain.ETHEREUM.name)
        nftActivityApiClient = clientFactory.createNftActivityApiClient(Blockchain.ETHEREUM.name)
        webClient = WebClient.builder().baseUrl("http://127.0.0.1:$port").build()

        poller = MonoTransactionPoller(ethereum)
    }

    protected fun createSigningSender(privateKey: BigInteger = Numeric.toBigInt(RandomUtils.nextBytes(32))) =
        MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

    protected suspend fun Mono<Word>.verifySuccess(): TransactionReceipt {
        val receipt = waitReceipt()
        Assertions.assertTrue(receipt.success()) {
            val result = ethereum.executeRaw(
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

    private suspend fun Mono<Word>.waitReceipt(): TransactionReceipt {
        val value = this.awaitFirstOrNull()
        require(value != null) { "txHash is null" }
        return ethereum.ethGetTransactionReceipt(value).awaitFirst().get()
    }
}
