package com.rarible.protocol.nft.migration.integration

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query

abstract class AbstractIntegrationTest {

    @Autowired
    lateinit var mongo: ReactiveMongoOperations

    @Autowired
    lateinit var nftIndexerProperties: NftIndexerProperties

    @Autowired
    lateinit var lazyNftItemHistoryRepository: LazyNftItemHistoryRepository

    @Autowired
    lateinit var tokenRepository: TokenRepository

    @Autowired
    lateinit var nftHistoryRepository: NftHistoryRepository

    @BeforeEach
    fun cleanDatabase() {
        mongo.collectionNames
            .filter { !it.startsWith("system") }
            .flatMap { mongo.remove(Query(), it) }
            .then().block()
    }
}
