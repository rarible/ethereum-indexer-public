package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

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
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class SudoSwapWithdrawNftPairDescriptorTest {
    private val traceCallService = TraceCallService(mockk(), mockk())
    private val sudoSwapEventConverter = SudoSwapEventConverter(traceCallService)

    private val descriptor = SudoSwapWithdrawNftPairDescriptor(
        sudoSwapEventConverter = sudoSwapEventConverter,
    )

    @Test
    fun `should convert nft withdraw to pair`() = runBlocking<Unit> {
        // from: https://etherscan.io/tx/0xb9f5aa82e26d7b13d991e8840e745957bf22a856f0137750fe87ce7b71e3288f
        val transaction = mockk<Transaction> {
            every { hash() } returns Word.apply(randomWord())
            every { from() } returns randomAddress()
            every { to() } returns randomAddress()
            every { input() } returns Binary.apply("0x13edab81000000000000000000000000ef1a89cbfabe59397ffda11fc5df293e9bc5db9000000000000000000000000000000000000000000000000000000000000000400000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000120f")
            every { value() } returns BigInteger.ZERO
        }
        val date = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val log = log(
            listOf(
                Word.apply("0x242b9b8fb5c0e6298454fcf80a0fbcbb7308620133d92b50091a1f64cee790e8")
            ),
            ""
        )
        val withdraw = descriptor.convert(log, transaction, date.epochSecond, 0, 1).toFlux().awaitSingle()
        Assertions.assertThat(withdraw.collection).isEqualTo(Address.apply("0xeF1a89cbfAbE59397FfdA11Fc5DF293E9bC5Db90"))
        Assertions.assertThat(withdraw.tokenIds).containsExactlyInAnyOrder(EthUInt256.of(4623))
        Assertions.assertThat(withdraw.date).isEqualTo(date)
        Assertions.assertThat(withdraw.source).isEqualTo(HistorySource.SUDOSWAP)
    }
}