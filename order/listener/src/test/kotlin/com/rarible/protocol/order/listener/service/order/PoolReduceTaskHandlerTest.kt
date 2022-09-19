package com.rarible.protocol.order.listener.service.order

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.data.randomSellOnChainAmmOrder
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class PoolReduceTaskHandlerTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var handler: PoolReduceTaskHandler

    @Test
    fun `should reduce pool order`() = runBlocking<Unit> {
        val poolCreate = randomSellOnChainAmmOrder()
        val logEvent = LogEvent(
            data = poolCreate,
            address = randomAddress(),
            topic = Word.apply(RandomUtils.nextBytes(32)),
            transactionHash = Word.apply(RandomUtils.nextBytes(32)),
            index = RandomUtils.nextInt(),
            minorLogIndex = 0,
            status = LogEventStatus.CONFIRMED
        )
        poolHistoryRepository.save(logEvent).awaitFirst()
        val hash = handler.runLongTask(from = null, param = "").toList().map { Word.apply(it) }.single()
        assertThat(hash).isEqualTo(poolCreate.hash)
        val order = orderRepository.findById(hash)
        assertThat(order).isNotNull
    }
}