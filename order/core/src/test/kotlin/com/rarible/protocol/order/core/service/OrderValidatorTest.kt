package com.rarible.protocol.order.core.service

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.model.toOrderExactFields
import com.rarible.protocol.order.core.service.validation.OrderValidator
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger

class OrderValidatorTest {
    
    private val validator = OrderValidator(mockk(), mockk())

    private val eip712Domain = EIP712Domain("", "", BigInteger.ONE, randomAddress())

    @Test
    fun `make change validation`() {
        val order = createOrderVersion(eip712Domain)
        assertThrows<OrderReduceService.OrderUpdateError> {
            validator.validate(order.toOrderExactFields(), order.copy(make = order.make.copy(value = EthUInt256.of(9))))
        }
        validator.validate(order.toOrderExactFields(), order.copy(make = order.make.copy(value = EthUInt256.of(11))))
    }

    @Test
    fun `price validation`() {
        val order = createOrderVersion(eip712Domain)
        assertThrows<OrderReduceService.OrderUpdateError> {
            validator.validate(
                order.toOrderExactFields(), order.copy(
                    make = order.make.copy(value = EthUInt256.of(20)),
                    take = order.make.copy(value = EthUInt256.of(11))
                )
            )
        }
        validator.validate(
            order.toOrderExactFields(), order.copy(
                make = order.make.copy(value = EthUInt256.of(20)),
                take = order.make.copy(value = EthUInt256.of(10))
            )
        )
        validator.validate(
            order.toOrderExactFields(), order.copy(
                make = order.make.copy(value = EthUInt256.of(20)),
                take = order.make.copy(value = EthUInt256.of(9))
            )
        )
    }

    @Test
    fun `bid price validation`() {
        val order = createOrderVersion(eip712Domain)
        assertThrows<OrderReduceService.OrderUpdateError> {
            validator.validate(
                order.toOrderExactFields(), order.copy(
                    make = order.make.copy(value = EthUInt256.of(9)),
                    take = order.make.copy(value = EthUInt256.of(5))
                )
            )
        }
        validator.validate(
            order.toOrderExactFields(), order.copy(
                make = order.make.copy(value = EthUInt256.of(11)),
                take = order.make.copy(value = EthUInt256.of(5))
            )
        )
    }
}
