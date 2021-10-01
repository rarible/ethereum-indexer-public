package com.rarible.protocol.order.migration.integration

import kotlinx.coroutines.FlowPreview
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query

@FlowPreview
abstract class AbstractMigrationTest {

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @BeforeEach
    fun cleanDatabase() {
        mongo.collectionNames
            .filter { !it.startsWith("system") }
            .flatMap { mongo.remove(Query(), it) }
            .then().block()
    }
}
