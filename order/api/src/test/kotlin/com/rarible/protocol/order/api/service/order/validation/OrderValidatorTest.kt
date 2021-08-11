package com.rarible.protocol.order.api.service.order.validation

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.api.data.createOrder
import com.rarible.protocol.order.api.exceptions.OrderUpdateError
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrderValidatorTest {
    
    private val validator = OrderValidator(mockk(), mockk())

    @Test
    fun `make change validation`() {
        val order = createOrder()
        assertThrows<OrderUpdateError> {
            validator.validate(order, order.copy(make = order.make.copy(value = EthUInt256.of(9))))
        }
        validator.validate(order, order.copy(make = order.make.copy(value = EthUInt256.of(11))))
    }

    @Test
    fun `price validation`() {
        val order = createOrder()
        assertThrows<OrderUpdateError> {
            validator.validate(order, order.copy(
                make = order.make.copy(value = EthUInt256.of(20)),
                take = order.make.copy(value = EthUInt256.of(11))
            ))
        }
        validator.validate(order, order.copy(
            make = order.make.copy(value = EthUInt256.of(20)),
            take = order.make.copy(value = EthUInt256.of(10))
        ))
        validator.validate(order, order.copy(
            make = order.make.copy(value = EthUInt256.of(20)),
            take = order.make.copy(value = EthUInt256.of(9))
        ))
    }

    @Test
    fun `bid price validation`() {
        val order = createOrder()
        assertThrows<OrderUpdateError> {
            validator.validate(order, order.copy(
                make = order.make.copy(value = EthUInt256.of(9)),
                take = order.make.copy(value = EthUInt256.of(5))
            ))
        }
        validator.validate(order, order.copy(
            make = order.make.copy(value = EthUInt256.of(11)),
            take = order.make.copy(value = EthUInt256.of(5))
        ))
    }
}
