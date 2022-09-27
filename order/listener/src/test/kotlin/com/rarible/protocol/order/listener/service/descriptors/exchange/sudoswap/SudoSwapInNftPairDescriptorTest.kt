package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.randomPoolInfo
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolTargetNftIn
import com.rarible.protocol.order.core.service.curve.SudoSwapCurve
import com.rarible.protocol.order.core.trace.TraceCallServiceImpl
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapPoolInfoProvider
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class SudoSwapInNftPairDescriptorTest {
    private val counter = mockk<RegisteredCounter> { every { increment() } returns Unit }
    private val sudoSwapPoolInfoProvider = mockk<SudoSwapPoolInfoProvider>()
    private val traceCallService = TraceCallServiceImpl(mockk(), mockk())
    private val sudoSwapCurve = mockk<SudoSwapCurve>()
    private val sudoSwapEventConverter = SudoSwapEventConverter(traceCallService)

    private val descriptor = SudoSwapInNftPairDescriptor(
        sudoSwapEventConverter = sudoSwapEventConverter,
        sudoSwapInNftEventCounter = counter,
        sudoSwapPoolInfoProvider = sudoSwapPoolInfoProvider,
        sudoSwapCurve = sudoSwapCurve
    )

    @Test
    fun `should convert target nft in pair`() = runBlocking<Unit> {
        // from:
        val transaction = mockk<Transaction> {
            every { hash() } returns Word.apply(randomWord())
            every { from() } returns randomAddress()
            every { to() } returns randomAddress()
            every { input() } returns Binary.apply("0xb1d3f1c100000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000000000000000000000000000046e9ab8cad0c0c00000000000000000000000003cb23ccc26a1870eb9e79b7a061907bdaef4f7d60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000001841")
            every { value() } returns BigInteger.ZERO
        }
        val date = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val collection = randomAddress()
        val poolInfo = randomPoolInfo().copy(collection)
        val log = log(
            listOf(
                Word.apply("0x3614eb567740a0ee3897c0e2b11ad6a5720d2e4438f9c8accf6c95c24af3a470")
            ),
            ""
        )
        coEvery { sudoSwapPoolInfoProvider.gePollInfo(log.address()) } returns poolInfo
        val nftOut = descriptor.convert(log, transaction, date.epochSecond, 0, 1).toFlux().awaitSingle()

        Assertions.assertThat(nftOut).isInstanceOf(PoolTargetNftIn::class.java)
        nftOut as PoolTargetNftIn

        Assertions.assertThat(nftOut.hash).isEqualTo(sudoSwapEventConverter.getPoolHash(log.address()))
        Assertions.assertThat(nftOut.collection).isEqualTo(collection)
        Assertions.assertThat(nftOut.tokenIds).containsExactlyInAnyOrder(EthUInt256.of(6209))
        Assertions.assertThat(nftOut.tokenRecipient).isEqualTo(Address.apply("0x3Cb23ccc26a1870eb9E79B7A061907BDaeF4F7D6"))
        Assertions.assertThat(nftOut.date).isEqualTo(date)
        Assertions.assertThat(nftOut.source).isEqualTo(HistorySource.SUDOSWAP)
    }
}