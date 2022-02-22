package com.rarible.protocol.order.core.model

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.data.randomErc1155
import com.rarible.protocol.order.core.data.randomErc20
import com.rarible.protocol.order.core.data.withMakeFill
import io.daonomic.rpc.domain.Binary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger

class OrderTest {

    @Test
    fun `opensea eip712 hash is calculated correctly`() {
        val hash = Order.openSeaV1EIP712Hash(
            maker = Address.apply("0xe4b5439c6f3f3f38c4bea769dffb5fc53966410b"),
            taker = Address.ZERO(),
            paymentToken = Address.ZERO(),
            basePrice = BigInteger("1000000000000000000"),
            salt = BigInteger("12300750963342693992786613621038998771658003841568634014036697390942768250585"),
            start = 1645176865L,
            end = 1645781758L,
            OrderOpenSeaV1DataV1(
                exchange = Address.apply("0xdd54d660178b28f6033a953b0e55073cfa7e3744"),
                makerRelayerFee = BigInteger("250"),
                takerRelayerFee = BigInteger.ZERO,
                makerProtocolFee = BigInteger.ZERO,
                takerProtocolFee = BigInteger.ZERO,
                feeRecipient = Address.apply("0x5b3256965e7c3cf26e11fcaf296dfc8807c01073"),
                feeMethod = OpenSeaOrderFeeMethod.SPLIT_FEE,
                side = OpenSeaOrderSide.SELL,
                saleKind = OpenSeaOrderSaleKind.FIXED_PRICE,
                howToCall = OpenSeaOrderHowToCall.DELEGATE_CALL,
                callData = Binary.apply("0xfb16a595000000000000000000000000e4b5439c6f3f3f38c4bea769dffb5fc53966410b00000000000000000000000000000000000000000000000000000000000000000000000000000000000000009336916ad1b2a564de188020fbfb920abb0804420000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000"),
                replacementPattern = Binary.apply("0x000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
                staticTarget = Address.ZERO(),
                staticExtraData = Binary.empty(),
                extra = BigInteger.ZERO,
                target = Address.apply("0x45b594792a5cdc008d0de1c1d69faa3d16b3ddc1"),
                nonce = 0L
            )
        )
        assertThat(hash).isEqualTo(Binary.apply("0x1879973b20431b35d0bc0ba145b95fd2f2f3b5676a232fd2c78aa2f87b3703d8"))
    }

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
