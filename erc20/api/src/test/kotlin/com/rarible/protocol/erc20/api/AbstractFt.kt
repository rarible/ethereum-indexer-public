package com.rarible.protocol.erc20.api

import com.rarible.protocol.client.FixedApiServiceUriProvider
import com.rarible.protocol.client.NoopWebClientCustomizer
import com.rarible.protocol.erc20.api.client.Erc20IndexerApiClientFactory
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import java.net.URI
import javax.annotation.PostConstruct

abstract class AbstractFt {
    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @BeforeEach
    fun cleanDatabase() {
        mongo.collectionNames
            .filter { !it.startsWith("system") }
            .flatMap { mongo.remove(Query(), it) }
            .then().block()
    }

    protected lateinit var clientFactory: Erc20IndexerApiClientFactory

    @LocalServerPort
    private var port: Int = 0

    @PostConstruct
    fun setUp() {
        val urlProvider = FixedApiServiceUriProvider(URI.create("http://127.0.0.1:$port"))
        clientFactory = Erc20IndexerApiClientFactory(urlProvider, NoopWebClientCustomizer())
    }
}
