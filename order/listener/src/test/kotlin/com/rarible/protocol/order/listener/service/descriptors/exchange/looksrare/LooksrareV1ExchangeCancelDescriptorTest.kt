package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.protocol.order.listener.misc.convert
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class LooksrareV1ExchangeCancelDescriptorTest {

    private val metrics = mockk<ForeignOrderMetrics>() {
        every { onOrderEventHandled(Platform.LOOKSRARE, "cancel") } returns Unit
    }
    private val orderRepository = mockk<OrderRepository>()
    private val contractsProvider = mockk<ContractsProvider> {
        every { looksrareV1() } returns listOf(randomAddress())
    }
    private val autoReduceService = mockk<AutoReduceService>()
    private val descriptor = LooksrareV1ExchangeCancelDescriptor(
        contractsProvider,
        orderRepository,
        metrics,
        autoReduceService,
    )

    @Test
    fun `should convert event to OrderCancel`() = runBlocking<Unit> {
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
                Platform.LOOKSRARE, order.maker, listOf(BigInteger.valueOf(2))
            )
        } returns flow { emit(order) }

        val cancels = descriptor.convert<OrderCancel>(log, data.epochSecond, 0, 1)

        assertThat(cancels).hasSize(1)
        assertThat(cancels.single().hash).isEqualTo(order.hash)
        assertThat(cancels.single().maker).isEqualTo(order.maker)
        assertThat(cancels.single().make).isEqualTo(order.make)
        assertThat(cancels.single().take).isEqualTo(order.take)
        assertThat(cancels.single().date).isEqualTo(data)
        assertThat(cancels.single().source).isEqualTo(HistorySource.LOOKSRARE)
    }
}
