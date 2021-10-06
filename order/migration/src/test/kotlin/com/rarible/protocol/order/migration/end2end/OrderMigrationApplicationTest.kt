package com.rarible.protocol.order.migration.end2end

import com.rarible.core.test.containers.MongodbTestContainer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@End2EndTest
@Disabled
internal class OrderMigrationApplicationTest {
    init {
        System.setProperty(
            "spring.data.mongodb.uri", mongoTest.connectionString()
        )
        System.setProperty(
            "spring.data.mongodb.database", "protocol"
        )
    }
    companion object {
        val mongoTest = MongodbTestContainer()
    }

    @Test
    fun contextStartup() {
    }
}
