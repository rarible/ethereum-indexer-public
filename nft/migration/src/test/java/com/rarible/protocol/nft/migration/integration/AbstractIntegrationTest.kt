package com.rarible.protocol.nft.migration.integration

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import kotlinx.coroutines.FlowPreview
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query

@FlowPreview
abstract class AbstractIntegrationTest {

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    protected lateinit var nftIndexerProperties: NftIndexerProperties

    @BeforeEach
    fun cleanDatabase() {
        mongo.collectionNames
            .filter { !it.startsWith("system") }
            .flatMap { mongo.remove(Query(), it) }
            .then().block()
    }
}
