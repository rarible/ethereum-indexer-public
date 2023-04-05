package com.rarible.protocol.order.listener.service.task

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.data.createOrderBasicSeaportDataV1
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.service.order.ReduceCanceledSeaportTaskHandler
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.Instant

@ExperimentalCoroutinesApi
@FlowPreview
@IntegrationTest
class ReduceCanceledSeaportOrdersTaskHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var handler: ReduceCanceledSeaportTaskHandler

    @BeforeEach
    fun setup() = runBlocking<Unit> {
        orderRepository.createIndexes()
    }

    @Test
    internal fun `remove order`() = runBlocking<Unit> {
        val now = Instant.now()
        val seaportAddress = randomAddress()
        val order = createOrder().copy(
            make = randomErc721(),
            lastUpdateAt = now - Duration.ofMinutes(1),
            data = createOrderBasicSeaportDataV1().copy(protocol = seaportAddress),
            status = OrderStatus.ACTIVE,
            type = OrderType.SEAPORT_V1,
            platform = Platform.OPEN_SEA,
        )
        orderRepository.save(order)
        orderVersionRepository.save(createOrderVersion().copy(hash = order.hash)).awaitFirst()
        val cancel = OrderCancel(
            hash = order.hash,
            date = nowMillis(),
            maker = null,
            make = randomErc721(),
            take = null,
            source = HistorySource.OPEN_SEA
        )
        val logEvent = LogEvent(
            data = cancel,
            address = randomAddress(),
            topic = Word.apply(ByteArray(32)),
            transactionHash = Word.apply(randomWord()),
            status = LogEventStatus.CONFIRMED,
            index = 0,
            logIndex = 0,
            minorLogIndex = 0
        )
        exchangeHistoryRepository.save(logEvent).awaitSingle()

        handler.runLongTask(null, "").collect()

        Wait.waitAssert {
            val reducedOrder = orderRepository.findById(order.hash)
            assertThat(reducedOrder?.status).isEqualTo(OrderStatus.CANCELLED)
        }
    }
}
