package com.rarible.protocol.order.api.service.order.validation

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.api.data.createOrderVersion
import com.rarible.protocol.order.api.service.order.validation.validators.ParametersPatchValidator
import com.rarible.protocol.order.core.exception.OrderUpdateException
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.toOrderExactFields
import com.rarible.protocol.order.core.repository.order.OrderRepository
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import scalether.domain.AddressFactory

@ExtendWith(MockKExtension::class)
class ParametersDeprecatedOrderValidatorTest {

    @InjectMockKs
    private lateinit var validator: ParametersPatchValidator

    @MockK
    private lateinit var orderRepository: OrderRepository

    private val make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN)
    private val take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5))

    @Test
    fun `make change validation`() = runBlocking<Unit> {
        val order = createOrderVersion(make, take)
        coEvery { orderRepository.findById(order.hash) } returns order.toOrderExactFields()
        assertThrows<OrderUpdateException> {
            validator.validate(
                order.copy(make = order.make.copy(value = EthUInt256.of(9))).toOrderExactFields()
            )
        }
        validator.validate(
            order.copy(make = order.make.copy(value = EthUInt256.of(11))).toOrderExactFields()
        )
    }

    @Test
    fun `price validation`() = runBlocking<Unit> {
        val order = createOrderVersion(make, take)
        coEvery { orderRepository.findById(order.hash) } returns order.toOrderExactFields()
        assertThrows<OrderUpdateException> {
            validator.validate(
                order.copy(
                    make = order.make.copy(value = EthUInt256.of(20)),
                    take = order.make.copy(value = EthUInt256.of(11))
                ).toOrderExactFields()
            )
        }
        validator.validate(
            order.copy(
                make = order.make.copy(value = EthUInt256.of(20)),
                take = order.make.copy(value = EthUInt256.of(10))
            ).toOrderExactFields()
        )
        validator.validate(
            order.copy(
                make = order.make.copy(value = EthUInt256.of(20)),
                take = order.make.copy(value = EthUInt256.of(9))
            ).toOrderExactFields()
        )
    }

    @Test
    fun `bid price validation`() = runBlocking<Unit> {
        val order = createOrderVersion(make, take)
        coEvery { orderRepository.findById(order.hash) } returns order.toOrderExactFields()
        assertThrows<OrderUpdateException> {
            validator.validate(
                order.copy(
                    make = order.make.copy(value = EthUInt256.of(9)),
                    take = order.make.copy(value = EthUInt256.of(5))
                ).toOrderExactFields()
            )
        }
        validator.validate(
            order.copy(
                make = order.make.copy(value = EthUInt256.of(11)),
                take = order.make.copy(value = EthUInt256.of(5))
            ).toOrderExactFields()
        )
    }
}
