package com.rarible.protocol.order.migration.integration

import kotlinx.coroutines.FlowPreview
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations

@FlowPreview
abstract class AbstractMigrationTest {

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

}
