package com.rarible.protocol.order.migration.integration.migration

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.migration.integration.AbstractMigrationTest
import com.rarible.protocol.order.migration.integration.IntegrationTest
import com.rarible.protocol.order.migration.integration.migration.AddTakeAndMakeToOrderTest.Companion.createOrderVersion
import com.rarible.protocol.order.migration.mongock.mongo.ChangeLog000019OrderVersionUpdate
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.ne

@IntegrationTest
class OrderVersionMigrationTest : AbstractMigrationTest(){

    val migration = ChangeLog000019OrderVersionUpdate()

    @Autowired
    lateinit var orderVersionRepository: OrderVersionRepository

    @Test
    fun `should set updatedAt field for order versions`() = runBlocking {
        val quantities = 100
        repeat(quantities) {
            orderVersionRepository.save(createOrderVersion()).awaitFirst()
        }

        val notNullQuery = Query(LogEvent::updatedAt ne null)
        val nullQuery = Query(LogEvent::updatedAt isEqualTo null)

        val update = Update().unset(LogEvent::updatedAt.name)
        orderVersionRepository.updateMulti(notNullQuery, update).awaitFirst()

        Assertions.assertThat(orderVersionRepository.find(nullQuery).toList()).hasSize(quantities)
        migration.updateOrderVersion(orderVersionRepository)

        Assertions.assertThat(orderVersionRepository.find(nullQuery).toList()).isEmpty()

        val orders = orderVersionRepository.findAll().asFlow().toList()
        Assertions.assertThat(orders).hasSize(quantities)
        orders.forEach {
            Assertions.assertThat(it.updatedAt).isEqualTo(it.createdAt)
        }
    }
}