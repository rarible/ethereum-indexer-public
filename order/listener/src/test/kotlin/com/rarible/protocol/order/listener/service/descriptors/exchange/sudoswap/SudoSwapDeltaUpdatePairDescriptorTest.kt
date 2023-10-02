package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolDeltaUpdate
import com.rarible.protocol.order.core.trace.TraceCallServiceImpl
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class SudoSwapDeltaUpdatePairDescriptorTest {
    private val counter = mockk<RegisteredCounter> { every { increment() } returns Unit }
    private val traceCallService = TraceCallServiceImpl(mockk(), mockk())
    private val sudoSwapEventConverter = SudoSwapEventConverter(traceCallService)
    private val autoReduceService = mockk<AutoReduceService>()

    private val descriptor = SudoSwapDeltaUpdatePairDescriptor(
        sudoSwapEventConverter = sudoSwapEventConverter,
        sudoSwapUpdateDeltaEventCounter = counter,
        autoReduceService = autoReduceService,
    )

    @Test
    fun `should convert update delta in pair`() = runBlocking<Unit> {
        // from: https://etherscan.io/tx/0x89459c7816a12ecbcfb216c1eeee6ea3c5f32a7b6f505a249ced487eaa4f848d
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
                Word.apply("0xc958ae052d28f8d17bc2c4ddbabb699a3cab5cccefd034d0fc971efdadc01da5")
            ),
            "0000000000000000000000000000000000000000000000000e30a65522d64000"
        )
        val ethBlock = EthereumBlockchainBlock(
            number = 1,
            hash = randomWord(),
            parentHash = randomWord(),
            timestamp = date.epochSecond,
            ethBlock = mockk()
        )
        val ethLog = EthereumBlockchainLog(
            ethLog = log,
            ethTransaction = transaction,
            index = 0,
            total = 1,
        )
        val update = descriptor
            .getEthereumEventRecords(ethBlock, ethLog)
            .map { it.asEthereumLogRecord().data as PoolDeltaUpdate }
            .single()

        Assertions.assertThat(update.hash).isEqualTo(sudoSwapEventConverter.getPoolHash(log.address()))
        Assertions.assertThat(update.newDelta).isEqualTo(BigInteger("1022500000000000000"))
        Assertions.assertThat(update.date).isEqualTo(date)
        Assertions.assertThat(update.source).isEqualTo(HistorySource.SUDOSWAP)
    }
}
