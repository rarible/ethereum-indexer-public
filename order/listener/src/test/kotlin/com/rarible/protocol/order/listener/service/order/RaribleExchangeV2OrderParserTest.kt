package com.rarible.protocol.order.listener.service.order

import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.test.data.randomInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
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

    @MockkBean
    private lateinit var erc1271SignService: ERC1271SignService

    companion object {
        @JvmStatic
        fun signServiceMethodResult(): Stream<Arguments> = run {
            val leftData = OrderRaribleV2DataV1(
                originFees = (1..4).map { Part(AddressFactory.create(), EthUInt256.Companion.of(randomInt())) },
                payouts = (1..4).map { Part(AddressFactory.create(), EthUInt256.Companion.of(randomInt())) }
            )
            val rightData = OrderRaribleV2DataV1(
                originFees = (1..4).map { Part(AddressFactory.create(), EthUInt256.Companion.of(randomInt())) },
                payouts = (1..4).map { Part(AddressFactory.create(), EthUInt256.Companion.of(randomInt())) }
            )
            val emptyData = OrderRaribleV2DataV1(emptyList(), emptyList())

            Stream.of(
                Arguments.of(true, leftData, rightData),
                Arguments.of(true, leftData.copy(originFees = emptyList()), rightData),
                Arguments.of(true, leftData.copy(originFees = emptyList()), rightData),
                Arguments.of(true, leftData.copy(payouts = emptyList()), rightData),
                Arguments.of(true, leftData.copy(payouts = emptyList()), rightData),

                Arguments.of(false, leftData, rightData),
                Arguments.of(false, leftData.copy(originFees = emptyList()), rightData),
                Arguments.of(false, leftData.copy(originFees = emptyList()), rightData),
                Arguments.of(false, leftData.copy(payouts = emptyList()), rightData),
                Arguments.of(false, leftData.copy(payouts = emptyList()), rightData),

                Arguments.of(true, emptyData, emptyData),
                Arguments.of(false, emptyData, emptyData)
            )
        }
    }

    @ParameterizedTest
    @MethodSource("signServiceMethodResult")
    fun `should decode order match transaction input`(
        isSigner: Boolean,
        leftData: OrderRaribleV2DataV1,
        rightData: OrderRaribleV2DataV1
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

        val input = prepareTxService.prepareTxFor2Orders(orderLeft, orderRight).transaction.data.prefixed()

        val result = raribleExchangeV2OrderParser.parseMatchedOrders(input)
        assertThat(result.left.data).isEqualTo(leftData)
        assertThat(result.right.data).isEqualTo(rightData)
    }
}
