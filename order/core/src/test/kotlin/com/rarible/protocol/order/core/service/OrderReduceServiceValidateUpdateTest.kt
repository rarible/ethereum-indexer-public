package com.rarible.protocol.order.core.service

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.model.toOrderExactFields
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrderReduceServiceValidateUpdateTest {

    @Test
    fun `make change validation`() {
        val order = createOrderVersion()
        assertThrows<OrderReduceService.OrderUpdateError> {
            OrderReduceService.validateUpdate(order.toOrderExactFields(), order.copy(make = order.make.copy(value = EthUInt256.of(9))))
        }
        OrderReduceService.validateUpdate(order.toOrderExactFields(), order.copy(make = order.make.copy(value = EthUInt256.of(11))))
    }

    @Test
    fun `price validation`() {
        val order = createOrderVersion()
        assertThrows<OrderReduceService.OrderUpdateError> {
            OrderReduceService.validateUpdate(
                order.toOrderExactFields(), order.copy(
                    make = order.make.copy(value = EthUInt256.of(20)),
                    take = order.make.copy(value = EthUInt256.of(11))
                )
            )
        }
        OrderReduceService.validateUpdate(
            order.toOrderExactFields(), order.copy(
                make = order.make.copy(value = EthUInt256.of(20)),
                take = order.make.copy(value = EthUInt256.of(10))
            )
        )
        OrderReduceService.validateUpdate(
            order.toOrderExactFields(), order.copy(
                make = order.make.copy(value = EthUInt256.of(20)),
                take = order.make.copy(value = EthUInt256.of(9))
            )
        )
    }

    @Test
    fun `bid price validation`() {
        val order = createOrderVersion()
        assertThrows<OrderReduceService.OrderUpdateError> {
            OrderReduceService.validateUpdate(
                order.toOrderExactFields(), order.copy(
                    make = order.make.copy(value = EthUInt256.of(9)),
                    take = order.make.copy(value = EthUInt256.of(5))
                )
            )
        }
        OrderReduceService.validateUpdate(
            order.toOrderExactFields(), order.copy(
                make = order.make.copy(value = EthUInt256.of(11)),
                take = order.make.copy(value = EthUInt256.of(5))
            )
        )
    }
}
