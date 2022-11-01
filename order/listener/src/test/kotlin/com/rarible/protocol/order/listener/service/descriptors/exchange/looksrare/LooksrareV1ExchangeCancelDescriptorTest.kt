package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.data.log
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class LooksrareV1ExchangeCancelDescriptorTest {

    private val exchangeContractAddresses = mockk<OrderIndexerProperties.ExchangeContractAddresses>()
    private val looksrareCancelOrdersEventMetric = mockk<RegisteredCounter> { every { increment(any()) } returns Unit }
    private val orderRepository = mockk<OrderRepository>()

    private val descriptor = LooksrareV1ExchangeCancelDescriptor(
        exchangeContractAddresses,
        orderRepository,
        looksrareCancelOrdersEventMetric,
    )

    @Test
    fun `should convert event to OrderCancel`() = runBlocking<Unit> {
        val transaction = mockk<Transaction>()
        val data = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val order = createOrder().copy(maker = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"))
        val log = log(
            listOf(
                Word.apply("0xfa0ae5d80fe3763c880a3839fab0294171a6f730d1f82c4cd5392c6f67b41732"),
                Word.apply("0x00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6")
            ),
            "000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000002"
        )
        coEvery {
            orderRepository.findByMakeAndByCounters(
                Platform.LOOKSRARE, order.maker, listOf(2)
            )
        } returns flow { emit(order) }
        val cancels = descriptor.convert(log, transaction, data.epochSecond, 0, 0).toFlux().collectList().awaitFirst()
        assertThat(cancels).hasSize(1)
        assertThat(cancels.single().hash).isEqualTo(order.hash)
        assertThat(cancels.single().maker).isEqualTo(order.maker)
        assertThat(cancels.single().make).isEqualTo(order.make)
        assertThat(cancels.single().take).isEqualTo(order.take)
        assertThat(cancels.single().date).isEqualTo(data)
        assertThat(cancels.single().source).isEqualTo(HistorySource.LOOKSRARE)
    }
}
