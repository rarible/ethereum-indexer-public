package com.rarible.protocol.order.listener.service.descriptors.exchange.x2y2

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
import com.rarible.protocol.order.listener.misc.convert
import com.rarible.protocol.order.listener.service.x2y2.X2Y2EventConverter
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class X2Y2OrderCancelDescriptorTest {

    private val existsOrderHash = Word.apply("0x039b0a8b3591535498ce70112227790408a73e034f66671b8701440172172dc1")
    private val nonExistsOrderHash = Word.apply("0x5b0b06d07e20243724d90e17a20034972f339eb28bd1c9437a71999bd15a1e7a")

    private val converter = X2Y2EventConverter(
        mockk {
            coEvery { findById(existsOrderHash) } returns createOrder()
            coEvery { findById(nonExistsOrderHash) } returns null
        },
        mockk(), mockk(), mockk()
    )
    private val contractsProvider = mockk<ContractsProvider>() {
        every { x2y2V1() } returns listOf(randomAddress())
    }
    private val metrics = mockk<ForeignOrderMetrics> {
        every { onOrderEventHandled(Platform.X2Y2, "cancel") } returns Unit
    }

    private val descriptor = X2Y2OrderCancelDescriptor(
        contractsProvider,
        converter,
        metrics
    )

    @Test
    internal fun `should process cancel event with exist order`() {
        runBlocking {
            val log = log(
                topics = listOf(
                    Word.apply("0x5b0b06d07e20243724d90e17a20034972f339eb28bd1c9437a71999bd15a1e7a"),
                    existsOrderHash
                ),
                data = "0x"
            )
            val date = Instant.ofEpochSecond(1L)

            val actual = descriptor
                .convert<OrderCancel>(log, date.epochSecond, 1, 1)
                .single()

            assertThat(actual).isNotNull
            assertThat(actual.make).isNotNull
            assertThat(actual.take).isNotNull
            assertThat(actual.maker).isNotNull
            assertThat(actual.date).isEqualTo(date)
            assertThat(actual.hash).isEqualTo(existsOrderHash)
        }
    }

    @Test
    internal fun `should process cancel event with non-exist order`() {
        runBlocking {
            val log = log(
                topics = listOf(
                    Word.apply("0x5b0b06d07e20243724d90e17a20034972f339eb28bd1c9437a71999bd15a1e7a"),
                    nonExistsOrderHash
                ),
                data = "0x"
            )
            val date = Instant.ofEpochSecond(1L)

            val actual = descriptor
                .convert<OrderCancel>(log, date.epochSecond, 1, 1)
                .single()

            assertThat(actual).isNotNull
            assertThat(actual.make).isNull()
            assertThat(actual.take).isNull()
            assertThat(actual.maker).isNull()
            assertThat(actual.date).isEqualTo(date)
            assertThat(actual.hash).isEqualTo(nonExistsOrderHash)
        }
    }
}
