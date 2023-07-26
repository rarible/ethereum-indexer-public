package com.rarible.protocol.order.core.model

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createOrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.data.randomErc1155
import com.rarible.protocol.order.core.data.randomErc20
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.data.withMakeFill
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger
import java.util.stream.Stream

class OrderTest {
    private companion object {
        @JvmStatic
        fun optionalRoyaltiesByPlatform(): Stream<Arguments> = Stream.concat(
            Stream.of(Arguments.of(Platform.SUDOSWAP, true)),
            Platform.values().toList().stream().filter { it != Platform.SUDOSWAP }.map { Arguments.of(it, false) }
        )
    }

    @Test
    fun `openSea order hash is calculated correctly`() {
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
    fun `seaport order hash is calculated correctly`() {
        val hash = Order.seaportV1Hash(
            maker = Address.apply("0x20f183f8f82042bb9acbd580e2d78c40f62a22a2"),
            salt = BigInteger("44291393813356285"),
            start = 1656991717L,
            end = 1659670117L,
            OrderBasicSeaportDataV1(
                protocol = Address.apply("0x00000000006c3852cbef3e08e8df289169ede581"),
                orderType = SeaportOrderType.FULL_RESTRICTED,
                offer = listOf(
                    SeaportOffer(
                        itemType = SeaportItemType.ERC721,
                        token = Address.apply("0x2ebda63bb8564abb40d7204ff0f2913d13119f34"),
                        identifier = BigInteger.ONE,
                        startAmount = BigInteger.ONE,
                        endAmount = BigInteger.ONE
                    )
                ),
                consideration = listOf(
                    SeaportConsideration(
                        itemType = SeaportItemType.NATIVE,
                        token = Address.ZERO(),
                        identifier = BigInteger.ZERO,
                        startAmount = BigInteger("975000000000000000"),
                        endAmount = BigInteger("975000000000000000"),
                        recipient = Address.apply("0x20f183f8f82042bb9acbd580e2d78c40f62a22a2")
                    ),
                    SeaportConsideration(
                        itemType = SeaportItemType.NATIVE,
                        token = Address.ZERO(),
                        identifier = BigInteger.ZERO,
                        startAmount = BigInteger("25000000000000000"),
                        endAmount = BigInteger("25000000000000000"),
                        recipient = Address.apply("0x8de9c5a032463c561423387a9648c5c7bcc5bc90")
                    )
                ),
                zone = Address.apply("0x00000000e88fe2628ebc5da81d2b3cead633e89e"),
                zoneHash = Word.apply("0x0000000000000000000000000000000000000000000000000000000000000000"),
                conduitKey = Word.apply("0x0000007b02230091a7ed01230072f7006a004d60a8d4e71d599b8104250f0000"),
                counter = 0L,
                counterHex = EthUInt256.ZERO
            )
        )
        assertThat(hash).isEqualTo(Binary.apply("0x52fc97e8de246fc3fbbc14082aac00d9b80561c5e2cce2914c71ec2fa873341a"))
    }

    @Test
    fun `opensea eip712 hash to sign is calculated correctly`() {
        val hash = Order.openSeaV1EIP712Hash(
            maker = Address.apply("0x610c242726939372a5552f764a0187adfc68dd92"),
            taker = Address.ZERO(),
            paymentToken = Address.ZERO(),
            basePrice = BigInteger("21500000000000000"),
            salt = BigInteger("73340800862702466407162619666324034326883802828380792972767741691037469737478"),
            start = 1651655292L,
            end = 1654333789L,
            OrderOpenSeaV1DataV1(
                exchange = Address.apply("0x7f268357a8c2552623316e2562d90e642bb538e5"),
                makerRelayerFee = BigInteger("750"),
                takerRelayerFee = BigInteger.ZERO,
                makerProtocolFee = BigInteger.ZERO,
                takerProtocolFee = BigInteger.ZERO,
                feeRecipient = Address.apply("0x5b3256965e7c3cf26e11fcaf296dfc8807c01073"),
                feeMethod = OpenSeaOrderFeeMethod.SPLIT_FEE,
                side = OpenSeaOrderSide.SELL,
                saleKind = OpenSeaOrderSaleKind.FIXED_PRICE,
                howToCall = OpenSeaOrderHowToCall.DELEGATE_CALL,
                callData = Binary.apply("0x96809f90000000000000000000000000610c242726939372a5552f764a0187adfc68dd920000000000000000000000000000000000000000000000000000000000000000000000000000000000000000495f947276749ce646f68ac8c248420045cb7b5e610c242726939372a5552f764a0187adfc68dd920000000000017c00000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000000000000"),
                replacementPattern = Binary.apply("0x000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
                staticTarget = Address.ZERO(),
                staticExtraData = Binary.empty(),
                extra = BigInteger.ZERO,
                target = Address.apply("0xbaf2127b49fc93cbca6269fade0f7f31df4c88a7"),
                nonce = 0L
            )

        )
        val hashToSign = Order.openSeaV1EIP712HashToSign(
            hash = hash,
            domain = Word.apply("0x72982d92449bfb3d338412ce4738761aff47fb975ceb17a1bc3712ec716a5a68")
        )
        assertThat(hashToSign).isEqualTo(Binary.apply("0x51cae95c8d9d85f58b34c08416473b4267e20ddcbf266794d9a0fd54136a0872"))
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
            randomOrder(),
            randomOrder().withMakeFill()
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
            randomOrder(),
            randomOrder().withMakeFill()
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
        val order = randomOrder()
            .copy(
                make = randomErc1155(EthUInt256.of(10)),
                take = randomErc20(EthUInt256.of(100)),
                type = OrderType.OPEN_SEA_V1
            )
        assertThat(order.withMakeBalance(EthUInt256.of(7), EthUInt256.ZERO).makeStock).isEqualTo(EthUInt256.ZERO)
    }

    @Test
    fun `should get zero stock for opensea bid order`() {
        /*
        Bid = 100 ERC20 -> 10 NFT
        Bid price = 10
        Balance = 7

        For OpenSea 7 < 10 => makeStock = 0
         */
        val order = randomOrder()
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
            randomOrder().copy(
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
    fun `should have active status for AMM order`() {
        val amm = randomOrder().copy(
            make = randomErc721(),
            take = randomErc20(EthUInt256.ZERO),
            makeStock = EthUInt256.ONE,
            data = createOrderSudoSwapAmmDataV1(),
            type = OrderType.AMM
        )
        assertThat(amm.status).isEqualTo(OrderStatus.ACTIVE)
    }

    @Test
    fun `should have inactive status for AMM order`() {
        val amm = randomOrder().copy(
            make = randomErc721().copy(value = EthUInt256.ZERO),
            take = randomErc20(EthUInt256.ZERO),
            makeStock = EthUInt256.ZERO,
            data = createOrderSudoSwapAmmDataV1(),
            type = OrderType.AMM
        )
        assertThat(amm.status).isEqualTo(OrderStatus.INACTIVE)
    }

    @Test
    fun `should calculate status for AMM order and make stock`() {
        val amm = randomOrder().copy(
            make = randomErc721().copy(value = EthUInt256.ONE),
            take = randomErc20(EthUInt256.ONE),
            makeStock = EthUInt256.ONE,
            data = createOrderSudoSwapAmmDataV1(),
            type = OrderType.AMM
        ).withMakeBalance(EthUInt256.of(28), EthUInt256.ZERO)

        assertThat(amm.makeStock).isEqualTo(EthUInt256.of(28))
        assertThat(amm.status).isEqualTo(OrderStatus.ACTIVE)
    }

    @Test
    fun `stock is 0 when cancelled for OpenSea order`() {
        assertThat(
            randomOrder().copy(
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
            randomOrder().copy(
                make = randomErc20(EthUInt256.TEN),
                take = randomErc20(EthUInt256.of(5))
            ).withMakeBalance(EthUInt256.of(5), EthUInt256.ZERO).makeStock
        )
            .isEqualTo(EthUInt256.of(4))
    }

    @Test
    fun `stock is make value when is enough of asset`() {
        assertThat(
            randomOrder().copy(
                make = randomErc20(EthUInt256.TEN),
                take = randomErc20(EthUInt256.of(5))
            ).withMakeBalance(EthUInt256.of(20), EthUInt256.ZERO).makeStock
        )
            .isEqualTo(EthUInt256.of(10))

        assertThat(
            randomOrder().copy(
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
            randomOrder().copy(
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
            randomOrder().withMakeFill().copy(
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
            randomOrder().copy(
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
            randomOrder().withMakeFill().copy(
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
        val order = randomOrder()
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
        val order = randomOrder()
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
        val order = randomOrder()
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

    @ParameterizedTest
    @MethodSource("optionalRoyaltiesByPlatform")
    fun `should get right flag for royalties`(platform: Platform, expectedValue: Boolean) {
        val order = randomOrder().copy(platform = platform)
        assertThat(order.isOptionalRoyalties()).isEqualTo(expectedValue)
    }

    @Nested
    inner class HashKey {
        @Test
        fun `V2 - different hash key for make-fill and take-fill orders data V1 vs data V2`() {
            val order = randomOrder()
            assertThat(order.id).isNotEqualTo(order.withMakeFill().id)
        }

        @Test
        fun `V2 - different hash key for make-fill and take-fill orders data V2`() {
            val order = randomOrder()
            assertThat(order.withMakeFill(isMakeFill = false).id)
                .isNotEqualTo(order.withMakeFill(isMakeFill = true).id)
        }
    }
}
