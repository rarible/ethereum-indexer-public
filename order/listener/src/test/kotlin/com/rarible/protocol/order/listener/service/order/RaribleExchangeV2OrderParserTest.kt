package com.rarible.protocol.order.listener.service.order

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.OrderRaribleV2Data
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.parser.ExchangeV2OrderParser
import com.rarible.protocol.order.core.service.PrepareTxService
import com.rarible.protocol.order.core.service.RaribleExchangeV2OrderParser
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.AddressFactory
import java.util.stream.Stream
import com.rarible.protocol.contracts.exchange.v2.rev3.MatchEvent as MatchEventRev3

@IntegrationTest
internal class RaribleExchangeV2OrderParserTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var prepareTxService: PrepareTxService

    @Autowired
    private lateinit var raribleExchangeV2OrderParser: RaribleExchangeV2OrderParser

    companion object {
        @JvmStatic
        fun signServiceMethodResult(): Stream<Arguments> = run {
            fun randomParts() = (1..4).map { Part(AddressFactory.create(), EthUInt256.Companion.of(randomInt())) }

            val leftDataV1 = OrderRaribleV2DataV1(originFees = randomParts(), payouts = randomParts())
            val rightDataV1 = OrderRaribleV2DataV1(originFees = randomParts(), payouts = randomParts())
            val emptyDataV1 = OrderRaribleV2DataV1(emptyList(), emptyList())

            val leftDataV2False = OrderRaribleV2DataV2(
                originFees = randomParts(),
                payouts = randomParts(),
                isMakeFill = false
            )
            val leftDataV2True = OrderRaribleV2DataV2(
                originFees = randomParts(),
                payouts = randomParts(),
                isMakeFill = true
            )
            val rightDataV2False = OrderRaribleV2DataV2(
                originFees = randomParts(),
                payouts = randomParts(),
                isMakeFill = false
            )
            val rightDataV2True = OrderRaribleV2DataV2(
                originFees = randomParts(),
                payouts = randomParts(),
                isMakeFill = true
            )
            val emptyDataV2False = OrderRaribleV2DataV2(emptyList(), emptyList(), isMakeFill = false)
            val emptyDataV2True = OrderRaribleV2DataV2(emptyList(), emptyList(), isMakeFill = true)

            Stream.of(
                Arguments.of(true, leftDataV1, rightDataV1),
                Arguments.of(true, leftDataV1.copy(originFees = emptyList()), rightDataV1),
                Arguments.of(true, leftDataV1.copy(originFees = emptyList()), rightDataV1),
                Arguments.of(true, leftDataV1.copy(payouts = emptyList()), rightDataV1),
                Arguments.of(true, leftDataV1.copy(payouts = emptyList()), rightDataV1),

                Arguments.of(false, leftDataV1, rightDataV1),
                Arguments.of(false, leftDataV1.copy(originFees = emptyList()), rightDataV1),
                Arguments.of(false, leftDataV1.copy(originFees = emptyList()), rightDataV1),
                Arguments.of(false, leftDataV1.copy(payouts = emptyList()), rightDataV1),
                Arguments.of(false, leftDataV1.copy(payouts = emptyList()), rightDataV1),

                Arguments.of(true, emptyDataV1, emptyDataV1),
                Arguments.of(false, emptyDataV1, emptyDataV1),

                Arguments.of(false, leftDataV2False, rightDataV2False),
                Arguments.of(false, leftDataV2False, rightDataV2True),
                Arguments.of(false, leftDataV2True, rightDataV2False),
                Arguments.of(false, leftDataV2True, rightDataV2True),

                Arguments.of(false, emptyDataV2False, emptyDataV2False),
                Arguments.of(false, emptyDataV2True, emptyDataV2True)
            )
        }
    }

    @ParameterizedTest
    @MethodSource("signServiceMethodResult")
    fun `should decode order match transaction input`(
        isSigner: Boolean,
        leftData: OrderRaribleV2Data,
        rightData: OrderRaribleV2Data
    ) = runBlocking<Unit> {
        val orderLeft = createOrder().copy(
            type = OrderType.RARIBLE_V2,
            data = leftData,
            signature = Binary.apply(ByteArray(65))
        )
        val orderRight = createOrder().copy(
            type = OrderType.RARIBLE_V2,
            data = rightData,
            signature = Binary.apply(ByteArray(65))
        )
        coEvery { erc1271SignService.isSigner(any(), any<Word>(), any()) } returns isSigner

        val input = prepareTxService.prepareTxFor2Orders(orderLeft, orderRight).transaction.data

        val result = ExchangeV2OrderParser.parseMatchedOrders(input)
        assertThat(result).isNotNull
        assertThat(result.left.data).isEqualTo(leftData)
        assertThat(result.right.data).isEqualTo(rightData)
    }

    @Test
    fun `parse directAcceptBid - ok`() = runBlocking<Unit> {
        val log = log(
            listOf(
                Word.apply("0x956cd63ee4cdcd81fda5f0ec7c6c36dceda99e1b412f4a650a5d26055dc3c450"),
            ),
            "b3b5a33688d25199ee0d1464aeb2e112af0e19f092c7ba52c10eb29fbc93e62a" +
                  "a396e5a527248ecd44316713165e48fa591623d1b9487f059992577bb670eb02" +
                  "00000000000000000000000000000000000000000000000000000002540be3ec" +
                  "000000000000000000000000000000000000000000000000000000003b9ac9fe"
        )
        val result = raribleExchangeV2OrderParser.parseMatchedOrders(
            txHash = Word.apply(randomWord()),
            txInput = Binary.apply(polygonDirectAcceptBid),
            event = MatchEventRev3.apply(log)
        )
        assertThat(result).isNotNull
        assertThat(result?.left?.hash).isEqualTo(
            Word.apply("b3b5a33688d25199ee0d1464aeb2e112af0e19f092c7ba52c10eb29fbc93e62a")
        )
        // BUY order
        assertThat(result?.right?.hash).isEqualTo(
            Word.apply("a396e5a527248ecd44316713165e48fa591623d1b9487f059992577bb670eb02")
        )
    }

    // https://polygonscan.com/tx/0xfb499642a31dee0b40d55676a079607b2c9d14ec9cf05b1528aa9f57703b61c2
    private val polygonDirectAcceptBid = "0x67d49a3b0000000000000000000000000000000000000000000000000000000000" +
            "0000200000000000000000000000003c5593af538b2278ebdcee385fdc5db94473daf00000000000000000000000000000000000000" +
            "00000000000000000003b9aca00973bb640000000000000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000001e000000000000000000000000000000000000000000000000000000002540" +
            "be4000000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619dade06dfc356ce2baf0ac1903fc4eb6171a8e5" +
            "c790258c59fab140124b4d8af5000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000023d235ef0000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000240000000000000000000000000000000000000000" +
            "000000000000000000000036000000000000000000000000000000000000000000000000000000002540be3ec000000000000000000" +
            "000000000000000000000000000000000000003b9ac9fe0000000000000000000000000000000000000000000000000000000000000" +
            "3e0000000000000000000000000000000000000000000000000000000000000004000000000000000000000000022d5f9b75c524fec" +
            "1d6619787e582644cd4d742200000000000000000000000000000000000000000000000000000000000000d20000000000000000000" +
            "00000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000" +
            "20000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000" +
            "00000000000000000000080000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "10000000000000000000000000f22f838aaca272afb0f268e4f4e655fac3a35ec000000000000000000000000000000000000000000" +
            "00000000000000000000640000000000000000000000000000000000000000000000000000000000000041d13f14a74c88c1619de89" +
            "66531fa3c4301ba37c1601b7f3c3034e11d19651ed161897377819889ce4634eca9f70779bd6fc027d8bd5a8d41709389e66740253b" +
            "1c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000010000000000000000000000000000000000000000000000000000000000000000200000000000000000000000" +
            "00000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000800" +
            "00000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000" +
            "00f22f838aaca272afb0f268e4f4e655fac3a35ec000000000000000000000000000000000000000000000000000000000000006400" +
            "000000000000000000000000000000000000000000000109616c6c64617461"
}
