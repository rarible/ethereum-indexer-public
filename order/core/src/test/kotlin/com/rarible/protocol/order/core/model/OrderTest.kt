package com.rarible.protocol.order.core.model

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.data.randomErc1155
import com.rarible.protocol.order.core.data.randomErc20
import com.rarible.protocol.order.core.data.withMakeFill
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import scalether.domain.AddressFactory

class OrderTest {

    @Test
    fun `should calculate make stock for bid`() {
        /*
        Bid: 100 ERC20 -> 2 NFT
        Price = 50
        Balance = 75
        makeStock = 75 / 50 * 50 = 50 can be used as payment.
         */
        listOf(
            createOrder(),
            createOrder().withMakeFill()
        ).forEach {
            val order = it.copy(
                make = randomErc20(EthUInt256.of(100)),
                take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.of(2))
            )
            assertThat(order.withMakeBalance(EthUInt256.of(75), EthUInt256.ZERO).makeStock)
                .isEqualTo(EthUInt256.of(50))
        }
    }

    @Test
    fun `should calculate make stock for sale`() {
        /*
        Sell order = 10 NFT -> 100 ERC20
        Balance = 7
        Make stock = 7
         */
        listOf(
            createOrder(),
            createOrder().withMakeFill()
        ).forEach {
            val order = it
                .copy(
                    make = randomErc1155(EthUInt256.of(10)),
                    take = randomErc20(EthUInt256.of(100))
                )
            assertThat(order.withMakeBalance(EthUInt256.of(7), EthUInt256.ZERO).makeStock)
                .isEqualTo(EthUInt256.of(7))
        }
    }

    @Test
    fun `should get zero stock for opensea sell order`() {
        /*
        Sell order = 10 NFT -> 100 ERC20
        Sell price = 10
        Balance = 7

        For OpenSea 7 < 10 => makeStock = 0
         */
        val order = createOrder()
            .copy(
                make = randomErc1155(EthUInt256.of(10)),
                take = randomErc20(EthUInt256.of(100)),
                type = OrderType.OPEN_SEA_V1
            )
        assertThat(order.withMakeBalance(EthUInt256.of(7), EthUInt256.ZERO).makeStock)
            .isEqualTo(EthUInt256.ZERO)
    }

    @Test
    fun `should get zero stock for opensea bid order`() {
        /*
        Bid = 100 ERC20 -> 10 NFT
        Bid price = 10
        Balance = 7

        For OpenSea 7 < 10 => makeStock = 0
         */
        val order = createOrder()
            .copy(
                make = randomErc20(EthUInt256.of(100)),
                take = randomErc1155(EthUInt256.TEN),
                type = OrderType.OPEN_SEA_V1
            )
        assertThat(order.withMakeBalance(EthUInt256.of(7), EthUInt256.ZERO).makeStock)
            .isEqualTo(EthUInt256.ZERO)
    }

    @Test
    fun `stock is 0 when cancelled`() {
        assertThat(
            createOrder().copy(
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
    fun `stock is 0 when cancelled for OpenSea order`() {
        assertThat(
            createOrder().copy(
                type = OrderType.OPEN_SEA_V1,
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
        assertThat(
            createOrder().copy(
                make = randomErc20(EthUInt256.TEN),
                take = randomErc20(EthUInt256.of(5))
            ).withMakeBalance(EthUInt256.of(5), EthUInt256.ZERO).makeStock
        )
            .isEqualTo(EthUInt256.of(4))
    }

    @Test
    fun `stock is make value when is enough of asset`() {
        assertThat(
            createOrder().copy(
                make = randomErc20(EthUInt256.TEN),
                take = randomErc20(EthUInt256.of(5))
            ).withMakeBalance(EthUInt256.of(20), EthUInt256.ZERO).makeStock
        )
            .isEqualTo(EthUInt256.of(10))

        assertThat(
            createOrder().copy(
                make = randomErc20(EthUInt256.TEN),
                take = randomErc20(EthUInt256.of(5))
            ).withMakeBalance(EthUInt256.of(10), EthUInt256.ZERO).makeStock
        )
            .isEqualTo(EthUInt256.of(10))
    }

    @Test
    fun `V2 take-fill order - stock is less when order is partially filled`() {
        /*
        Bid = 10 ERC20 -> 5 NFT
        Bid price = 2
        take fill = 3

        Remaining bid: 4 ERC20 -> 2 NFT
        makeStock = 4
         */
        assertThat(
            createOrder().copy(
                make = randomErc20(EthUInt256.TEN),
                take = randomErc1155(EthUInt256.of(5)),
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
    fun `V2 make-fill order - stock is less when order is partially filled`() {
        /*
        Sell = 5 NFT -> 10 ERC20
        Sell price = 2
        make fill = 3

        Remaining bid: 2 NFT -> 4 ERC20
        makeStock = 2
         */
        assertThat(
            createOrder().withMakeFill().copy(
                make = randomErc1155(EthUInt256.of(5)),
                take = randomErc20(EthUInt256.TEN),
                fill = EthUInt256.of(3),
                lastUpdateAt = nowMillis()
            ).withMakeBalance(
                makeBalance = EthUInt256.TEN,
                protocolCommission = EthUInt256.ZERO
            ).makeStock
        )
            .isEqualTo(EthUInt256.of(2))
    }

    @Test
    fun `V2 take-fill order - stock is 0 when the order is filled`() {
        assertThat(
            createOrder().copy(
                make = randomErc20(EthUInt256.TEN),
                take = randomErc20(EthUInt256.of(5)),
                fill = EthUInt256.of(5), // fill by take side
                lastUpdateAt = nowMillis()
            ).withMakeBalance(
                makeBalance = EthUInt256.TEN,
                protocolCommission = EthUInt256.ZERO
            ).makeStock
        ).isEqualTo(EthUInt256.ZERO)
    }

    @Test
    fun `V2 make-fill order - stock is 0 when the order is filled`() {
        assertThat(
            createOrder().withMakeFill().copy(
                make = randomErc20(EthUInt256.TEN),
                take = randomErc20(EthUInt256.of(5)),
                fill = EthUInt256.TEN, // fill by make side
                lastUpdateAt = nowMillis()
            ).withMakeBalance(
                makeBalance = EthUInt256.TEN,
                protocolCommission = EthUInt256.ZERO
            ).makeStock
        ).isEqualTo(EthUInt256.ZERO)
    }

    @Test
    fun `make stock for V1 bid - origin fee`() {
        /*
        Bid = 100 ERC20 -> 4 ERC1155
        Bid price = 25
        Balance = 75
        Origin fee = 3000 / 10000 ~= 1/3
        Protocol fee = 3000 / 1000 ~= 1/3
        => Make stock = (75 - 1/3 * 75) / 25 * 25 = 25
         */
        val order = createOrder()
            .copy(
                make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(100)),
                take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.of(4)),
                data = OrderDataLegacy(3000)
            )
        assertThat(order.withMakeBalance(EthUInt256.of(75), EthUInt256.ZERO).makeStock)
            .isEqualTo(EthUInt256.of(50))
    }

    @Test
    fun `make stock for V2 bid - origin fee`() {
        /*
        Bid = 100 ERC20 -> 4 ERC1155
        Bid price = 25
        Balance = 75
        Origin fee = 3000 / 10000 ~= 1/3
        => Make stock = (75 - 1/3 * 75 ) / 25 * 25 = 50
         */
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

        assertThat(order.withMakeBalance(EthUInt256.of(75), EthUInt256.ZERO).makeStock)
            .isEqualTo(EthUInt256.of(50))
    }

    @Test
    fun `make stock for V2 bid - origin fee and protocol fee`() {
        /*
        Bid = 100 ERC20 -> 4 ERC1155
        Bid price = 25
        Balance = 75
        Origin fee = 3000 / 10000 ~= 1/3
        Protocol fee = 3000 / 1000 ~= 1/3
        => Make stock = (75 - 1/3 * 75 - 1/3 * 75) / 25 * 25 = 25
         */
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

        assertThat(order.withMakeBalance(EthUInt256.of(75), EthUInt256.of(3000)).makeStock)
            .isEqualTo(EthUInt256.of(25))
    }

    @Nested
    inner class HashKey {
        @Test
        fun `V2 - different hash key for make-fill and take-fill orders data V1 vs data V2`() {
            val order = createOrder()
            assertThat(order.hash).isNotEqualTo(order.withMakeFill().hash)
        }

        @Test
        fun `V2 - different hash key for make-fill and take-fill orders data V2`() {
            val order = createOrder()
            assertThat(order.withMakeFill(isMakeFill = false).hash)
                .isNotEqualTo(order.withMakeFill(isMakeFill = true).hash)
        }

    }
}
