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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class SudoSwapSpotPriceUpdatePairDescriptorTest {
    private val counter = mockk<RegisteredCounter> { every { increment() } returns Unit }
    private val traceCallService = TraceCallService(mockk(), mockk())
    private val sudoSwapEventConverter = SudoSwapEventConverter(traceCallService)

    private val descriptor = SudoSwapSpotPriceUpdatePairDescriptor(
        sudoSwapEventConverter = sudoSwapEventConverter,
        sudoSwapUpdateSpotPriceEventCounter = counter,
    )

    @Test
    fun `should convert update spot price to pair`() = runBlocking<Unit> {
        // from: https://etherscan.io/tx/0xacd9c875bf76a7181af3c379b547d59e93efa3dfbe781127b0c66d9b170f15e6#eventlog
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
                Word.apply("0xf06180fdbe95e5193df4dcd1352726b1f04cb58599ce58552cc952447af2ffbb")
            ),
            "0x000000000000000000000000000000000000000000000000048ed26aaef2fa50"
        )
        val update = descriptor.convert(log, transaction, date.epochSecond, 0, 1).toFlux().awaitSingle()
        Assertions.assertThat(update.hash).isEqualTo(sudoSwapEventConverter.getPoolHash(log.address()))
        Assertions.assertThat(update.newSpotPrice).isEqualTo(BigInteger("328431178488150608"))
        Assertions.assertThat(update.date).isEqualTo(date)
        Assertions.assertThat(update.source).isEqualTo(HistorySource.SUDOSWAP)
    }
}