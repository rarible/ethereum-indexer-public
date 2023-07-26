package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.exception.OrderDataException
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Part
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class OrderDataValidatorTest {

    private val orderVersion = mockk<OrderVersion>()
    private val orderValidator = OrderDataValidator()

    @Test
    fun `validate order data - no payouts - success`() = runBlocking<Unit> {
        // given
        val orderData = OrderRaribleV2DataV1(payouts = emptyList(), originFees = emptyList())
        every { orderVersion.type } returns OrderType.RARIBLE_V2
        every { orderVersion.data } returns orderData

        // when, then
        Assertions.assertThatCode {
            runBlocking { orderValidator.validate(orderVersion) }
        }.doesNotThrowAnyException()
    }

    @Test
    fun `validate order data - some payouts - success`() = runBlocking<Unit> {
        // given
        val part1 = Part(account = randomAddress(), value = EthUInt256.of(7500))
        val part2 = Part(account = randomAddress(), value = EthUInt256.of(2500))
        val orderData = OrderRaribleV2DataV1(payouts = listOf(part1, part2), originFees = emptyList())
        every { orderVersion.type } returns OrderType.RARIBLE_V2
        every { orderVersion.data } returns orderData

        // when, then
        Assertions.assertThatCode {
            runBlocking { orderValidator.validate(orderVersion) }
        }.doesNotThrowAnyException()
    }

    @Test
    fun `validate order data - incorrect payouts - failure`() = runBlocking<Unit> {
        // given
        val part1 = Part(account = randomAddress(), value = EthUInt256.of(500))
        val part2 = Part(account = randomAddress(), value = EthUInt256.of(2500))
        val orderData = OrderRaribleV2DataV1(payouts = listOf(part1, part2), originFees = emptyList())
        every { orderVersion.type } returns OrderType.RARIBLE_V2
        every { orderVersion.data } returns orderData

        // when, then
        Assertions.assertThatCode {
            runBlocking { orderValidator.validate(orderVersion) }
        }.isExactlyInstanceOf(OrderDataException::class.java)
    }
}
