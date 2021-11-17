package com.rarible.protocol.order.listener.service.order

import com.rarible.core.test.data.randomInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.misc.toBinary
import com.rarible.protocol.order.core.model.OrderRaribleV2Data
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.service.PrepareTxService
import com.rarible.protocol.order.core.service.RaribleExchangeV2OrderParser
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.AddressFactory
import java.util.stream.Stream

@FlowPreview
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

        val result = raribleExchangeV2OrderParser.parseMatchedOrders(input)
        assertThat(result).isNotNull
        assertThat(result!!.left.data).isEqualTo(leftData)
        assertThat(result.right.data).isEqualTo(rightData)
    }

    @Test
    fun `should safe parse invalid order match transaction input`() = runBlocking<Unit> {
        val input = "0x23445435656464".toBinary()
        val result = raribleExchangeV2OrderParser.parseMatchedOrders(input)
        assertThat(result).isNull()
    }
}
