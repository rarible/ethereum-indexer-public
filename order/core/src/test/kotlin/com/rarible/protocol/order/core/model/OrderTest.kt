package com.rarible.protocol.order.core.model

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createOrder
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import scalether.domain.AddressFactory

class OrderTest {
    private val order = createOrder()

    @Test
    fun `should calculate make stock for bid`() {
        val order = createOrder()
            .copy(
                make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(100)),
                take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.of(2))
            )
        Assertions
            .assertThat(order.withMakeBalance(EthUInt256.of(75), EthUInt256.ZERO).makeStock)
            .isEqualTo(EthUInt256.of(50))
    }

    @Test
    fun `should set make stock with new values`() {
        val order = createOrder()
            .copy(
                make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(100)),
                take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.of(4))
            )
        Assertions
            .assertThat(
                order.withNewValues(
                    EthUInt256.of(140),
                    EthUInt256.of(4),
                    EthUInt256.of(100),
                    null,
                    nowMillis()
                ).makeStock
            )
            .isEqualTo(EthUInt256.of(100))
    }

    @Test
    fun `should calculate make stock for sale`() {
        val order = createOrder()
            .copy(
                make = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.of(10)),
                take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(100))
            )
        Assertions
            .assertThat(order.withMakeBalance(EthUInt256.of(7), EthUInt256.ZERO).makeStock)
            .isEqualTo(EthUInt256.of(7))
    }

    @Test
    fun `should get zero stock for opensea sell order`() {
        val order = createOrder()
            .copy(
                make = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.of(10)),
                take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(100)),
                type = OrderType.OPEN_SEA_V1
            )
        Assertions
            .assertThat(order.withMakeBalance(EthUInt256.of(7), EthUInt256.ZERO).makeStock)
            .isEqualTo(EthUInt256.ZERO)
    }

    @Test
    fun `should get zero stock for opensea buy order`() {
        val order = createOrder()
            .copy(
                make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(100)),
                take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.of(10)),
                type = OrderType.OPEN_SEA_V1
            )
        Assertions
            .assertThat(order.withMakeBalance(EthUInt256.of(7), EthUInt256.ZERO).makeStock)
            .isEqualTo(EthUInt256.ZERO)
    }

    @Test
    fun `stock is 0 when cancelled`() {
        Assertions.assertThat(
            order.copy(
                cancelled = true,
                lastUpdateAt = nowMillis()
            ).withMakeBalance(
                makeBalance = EthUInt256.TEN,
                protocolCommission = EthUInt256.ZERO
            ).makeStock
        )
            .isEqualTo(EthUInt256.ZERO)
    }

    @Test
    fun `stock is less than make value when balance is low`() {
        Assertions
            .assertThat(order.withMakeBalance(EthUInt256.of(5), EthUInt256.ZERO).makeStock)
            .isEqualTo(EthUInt256.of(4))
    }

    @Test
    fun `stock is make value when is enough of asset`() {
        Assertions.assertThat(order.withMakeBalance(EthUInt256.of(20), EthUInt256.ZERO).makeStock)
            .isEqualTo(EthUInt256.of(10))
        Assertions.assertThat(order.withMakeBalance(EthUInt256.of(10), EthUInt256.ZERO).makeStock)
            .isEqualTo(EthUInt256.of(10))
    }

    @Test
    fun `stock is less when order is partially filled`() {
        Assertions.assertThat(
            order.copy(
                fill = EthUInt256.of(3),
                lastUpdateAt = nowMillis()
            ).withMakeBalance(
                makeBalance = EthUInt256.TEN,
                protocolCommission = EthUInt256.ZERO
            ).makeStock
        )
            .isEqualTo(EthUInt256.of(4))
    }

    @Test
    fun `stock is 0 when order is filled`() {
        Assertions.assertThat(
            order.copy(
                fill = EthUInt256.of(5),
                lastUpdateAt = nowMillis()
            ).withMakeBalance(
                makeBalance = EthUInt256.TEN,
                protocolCommission = EthUInt256.ZERO
            ).makeStock
        )
            .isEqualTo(EthUInt256.ZERO)
    }

    @Test
    fun `should calculate make stock for bid respect origin fee v1`() {
        val order = createOrder()
            .copy(
                make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(100)),
                take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.of(4)),
                data = OrderDataLegacy(3000)
            )
        Assertions
            .assertThat(order.withMakeBalance(EthUInt256.of(75), EthUInt256.ZERO).makeStock)
            .isEqualTo(EthUInt256.of(50))
    }

    @Test
    fun `should calculate make stock for bid respect origin fee v2`() {
        val order = createOrder()
            .copy(
                make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(100)),
                take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.of(4)),
                data = OrderRaribleV2DataV1(
                    payouts = emptyList(),
                    originFees = listOf(
                        Part(AddressFactory.create(), EthUInt256.of(1500)),
                        Part(AddressFactory.create(), EthUInt256.of(1500))
                    )
                )
            )

        Assertions
            .assertThat(order.withMakeBalance(EthUInt256.of(75), EthUInt256.ZERO).makeStock)
            .isEqualTo(EthUInt256.of(50))
    }

    @Test
    fun `should calculate make stock for bid respect origin fee v2 and protocol fee`() {
        val order = createOrder()
            .copy(
                make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(100)),
                take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.of(4)),
                data = OrderRaribleV2DataV1(
                    payouts = emptyList(),
                    originFees = listOf(
                        Part(AddressFactory.create(), EthUInt256.of(1500)),
                        Part(AddressFactory.create(), EthUInt256.of(1500))
                    )
                )
            )

        Assertions
            .assertThat(order.withMakeBalance(EthUInt256.of(75), EthUInt256.of(3000)).makeStock)
            .isEqualTo(EthUInt256.of(25))
    }
}
