package com.rarible.protocol.nft.api.e2e

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.client.NoopWebClientCustomizer
import com.rarible.protocol.nft.api.client.*
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.springframework.web.reactive.function.client.WebClient
import scalether.core.MonoEthereum
import scalether.transaction.MonoTransactionPoller
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
}
