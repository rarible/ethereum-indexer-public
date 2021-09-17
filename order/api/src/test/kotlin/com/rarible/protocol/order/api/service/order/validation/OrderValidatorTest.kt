package com.rarible.protocol.order.api.service.order.validation

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.api.data.createOrderVersion
import com.rarible.protocol.order.api.exceptions.OrderUpdateException
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.toOrderExactFields
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import scalether.domain.AddressFactory

class OrderValidatorTest {

    private val validator = OrderValidator(mockk(), mockk())

    private val make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN)
    private val take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5))

    @Test
    fun `make change validation`() {
        val order = createOrderVersion(make, take)
        assertThrows<OrderUpdateException> {
            validator.validate(
                order.toOrderExactFields(),
                order.copy(make = order.make.copy(value = EthUInt256.of(9)))
            )
        }
        validator.validate(
            order.toOrderExactFields(),
            order.copy(make = order.make.copy(value = EthUInt256.of(11)))
        )
    }

    @Test
    fun `price validation`() {
        val order = createOrderVersion(make, take)
        assertThrows<OrderUpdateException> {
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
        val order = createOrderVersion(make, take)
        assertThrows<OrderUpdateException> {
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
