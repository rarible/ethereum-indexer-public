package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import io.daonomic.rpc.domain.WordFactory
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class OrderStateServiceIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var orderStateService: OrderStateService

    @Test
    fun `set cancel state - ok`() = runBlocking<Unit> {
        val id = WordFactory.create()

        val result = orderStateService.setCancelState(id)
        val fromDb = orderStateRepository.getById(id)

        assertThat(result).isEqualTo(fromDb)
    }

    @Test
    fun `set cancel state - already cancelled`() = runBlocking<Unit> {
        val id = WordFactory.create()

        val current = orderStateService.setCancelState(id)
        val alreadyCancelled = orderStateService.setCancelState(id)

        // versions should be the same
        assertThat(current).isEqualTo(alreadyCancelled)
    }

}