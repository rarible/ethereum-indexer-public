package com.rarible.protocol.order.core.repository

import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import com.rarible.protocol.order.core.model.AutoReduce
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class AutoReduceRepositoryIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var autoReduceRepository: AutoReduceRepository

    @Test
    fun crud() = runBlocking<Unit> {
        val order1 = AutoReduce(randomWord())
        val order2 = AutoReduce(randomWord())
        autoReduceRepository.saveOrders(listOf(order1))
        autoReduceRepository.saveOrders(listOf(order1, order2))

        assertThat(autoReduceRepository.findOrders().toList()).containsExactlyInAnyOrder(order1, order2)

        autoReduceRepository.removeOrder(order1)

        assertThat(autoReduceRepository.findOrders().toList()).containsExactlyInAnyOrder(order2)
    }
}
