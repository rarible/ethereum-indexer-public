package com.rarible.protocol.order.listener.service.descriptors.exchange.x2y2

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.listener.service.x2y2.X2Y2EventConverter
import io.daonomic.rpc.domain.Word
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toFlux

class X2Y2OrderCancelDescriptorTest {

    private val cancerMetric = mockk<RegisteredCounter>() {
        every { increment() } just Runs
    }

    private val existsOrderHash = Word.apply("0x039b0a8b3591535498ce70112227790408a73e034f66671b8701440172172dc1")
    private val nonExistsOrderHash = Word.apply("0x5b0b06d07e20243724d90e17a20034972f339eb28bd1c9437a71999bd15a1e7a")

    private val converter = X2Y2EventConverter(
        mockk {
            coEvery { findById(existsOrderHash) } returns createOrder()
            coEvery { findById(nonExistsOrderHash) } returns null
        },
        mockk(), mockk(), mockk()
    )

    private val descriptor = X2Y2OrderCancelDescriptor(
        mockk(), converter, cancerMetric
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
            val actual = descriptor.convert(log, mockk(), 1L, 1, 1).toFlux().awaitSingle()
            verify(exactly = 1) {
                cancerMetric.increment()
            }
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
            val actual = descriptor.convert(log, mockk(), 1L, 1, 1).toFlux().awaitSingle()
            verify(exactly = 1) {
                cancerMetric.increment()
            }
            assertThat(actual).isNotNull
            assertThat(actual.make).isNull()
            assertThat(actual.take).isNull()
            assertThat(actual.maker).isNull()
            assertThat(actual.date).isEqualTo(date)
            assertThat(actual.hash).isEqualTo(nonExistsOrderHash)
        }
    }
}
