package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class SudoSwapFeeUpdatePairDescriptorTest {
    private val counter = mockk<RegisteredCounter> { every { increment() } returns Unit }
    private val traceCallService = TraceCallService(mockk(), mockk())
    private val sudoSwapEventConverter = SudoSwapEventConverter(traceCallService)

    private val descriptor = SudoSwapFeeUpdatePairDescriptor(
        sudoSwapEventConverter = sudoSwapEventConverter,
        sudoSwapUpdateFeeEventCounter = counter,
    )

    @Test
    fun `should convert update fee in pair`() = runBlocking<Unit> {
        // from: https://etherscan.io/tx/0x2fbde7eb2bb0ec4a84377b4154980bb5c22a80bf1ddcd668db0e795eff5ec7b2
        val transaction = mockk<Transaction> {
            every { hash() } returns Word.apply(randomWord())
            every { from() } returns randomAddress()
            every { to() } returns randomAddress()
            every { input() } returns Binary.apply("")
            every { value() } returns BigInteger.ZERO
        }
        val date = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val log = log(
            listOf(
                Word.apply("0x66c55c30868c51e7ad52e3d85d1403576a9967614e67c48e25b55a10baa650c0")
            ),
            "00000000000000000000000000000000000000000000000002c68af0bb140000"
        )
        val update = descriptor.convert(log, transaction, date.epochSecond, 0, 1).toFlux().awaitSingle()
        Assertions.assertThat(update.hash).isEqualTo(sudoSwapEventConverter.getPoolHash(log.address()))
        Assertions.assertThat(update.newFee).isEqualTo(BigInteger("200000000000000000"))
        Assertions.assertThat(update.date).isEqualTo(date)
        Assertions.assertThat(update.source).isEqualTo(HistorySource.SUDOSWAP)
    }
}