package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.Blockchain
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createOrderBasicSeaportDataV1
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OpenSeaOrderFeeMethod
import com.rarible.protocol.order.core.model.OpenSeaOrderHowToCall
import com.rarible.protocol.order.core.model.OpenSeaOrderSaleKind
import com.rarible.protocol.order.core.model.OpenSeaOrderSide
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.service.CallDataEncoder
import com.rarible.protocol.order.core.service.CommonSigner
import com.rarible.protocol.order.core.service.OpenSeaSigner
import com.rarible.protocol.order.listener.configuration.SeaportLoadProperties
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger

internal class OpenSeaOrderValidatorTest {
    private val properties = mockk<OrderIndexerProperties> {
        every { chainId } returns 4
        every { minSeaportMakePrice } returns BigDecimal.valueOf(1, 15)
        every { blockchain } returns Blockchain.ETHEREUM
    }
    private val openSeaOrderValidator = OpenSeaOrderValidatorImp(
        commonSigner = CommonSigner(),
        callDataEncoder = CallDataEncoder(),
        openSeaSigner = OpenSeaSigner(
            CommonSigner(),
            EIP712Domain(
                "Wyvern Exchange Contract",
                "2.3",
                BigInteger.valueOf(4),
                Address.apply("0xdd54d660178b28f6033a953b0e55073cfa7e3744")
            )
        ),
        metrics = ForeignOrderMetrics(properties, SimpleMeterRegistry()),
        seaportLoadProperties = SeaportLoadProperties(),
        properties = properties
    )

    @Test
    fun `should validate seaport order`() {
        val seaportOrder = createOrderVersion().copy(
            hash = Word.apply("0xc3c0b20b40fde2ae91cd324dca5c95f2227e0825b6eef8e298ff5b5352313727"),
            maker = Address.apply("0x54b126961a8f2ba34654a4cb7c89ce3a9421e6a9"),
            type = OrderType.SEAPORT_V1,
            data = createOrderBasicSeaportDataV1().copy(
                protocol = Address.apply("0x00000000006c3852cbef3e08e8df289169ede581")
            ),
            signature = Binary.apply("0x038e202fa2f1a7ac7944cd404057f4f9d945c9e2a1d4e8dd251592aa9f55587c11b2710f56474dbcd285035c4be1d61516e4c69e2ad9512baa0f699b43637fee1c")
        )
        assertThat(openSeaOrderValidator.validate(seaportOrder)).isTrue
    }

    @Test
    fun `should validate seaport order with compact signature and v = 28`() {
        val seaportOrder = createOrderVersion().copy(
            hash = Word.apply("0x2502ba826f537d2b43747a78b1e74d8eaa115e6840b531b893a144e08d16520f"),
            maker = Address.apply("0x22d491bde2303f2f43325b2108d26f1eaba1e32b"),
            type = OrderType.SEAPORT_V1,
            data = createOrderBasicSeaportDataV1().copy(
                protocol = Address.apply("0x00000000006c3852cbef3e08e8df289169ede581")
            ),
            signature = Binary.apply("0x0433bd90606f054ce6575b0afff9fc83b143e150c2214065c53205d83bfc9c14cb6acdaa5754a54d9c6a77924f777e1bfd3158e8324c833d0339e6fe73962df6")
        )
        assertThat(openSeaOrderValidator.validate(seaportOrder)).isTrue
    }

    @Test
    fun `should validate opensea order with price less 1000 wei`() {
        val seaportOrder = createOrderVersion().copy(
            hash = Word.apply("0xc3c0b20b40fde2ae91cd324dca5c95f2227e0825b6eef8e298ff5b5352313727"),
            maker = Address.apply("0x54b126961a8f2ba34654a4cb7c89ce3a9421e6a9"),
            type = OrderType.SEAPORT_V1,
            makePrice = BigDecimal.valueOf(1, 15),
            data = createOrderBasicSeaportDataV1().copy(
                protocol = Address.apply("0x00000000006c3852cbef3e08e8df289169ede581")
            ),
            signature = Binary.apply("0x038e202fa2f1a7ac7944cd404057f4f9d945c9e2a1d4e8dd251592aa9f55587c11b2710f56474dbcd285035c4be1d61516e4c69e2ad9512baa0f699b43637fee1c")
        )
        assertThat(openSeaOrderValidator.validate(seaportOrder)).isFalse
    }

    @Test
    fun `should validate seaport order with compact signature and v = 27`() {
        val seaportOrder = createOrderVersion().copy(
            hash = Word.apply("0x36c136ce4db81f5b04f9b7fdabdba241720382b6f4366b1459bde9f3698f21d9"),
            maker = Address.apply("0x162e5a9f5da4aca243ee397c8389eeaf1231d301"),
            type = OrderType.SEAPORT_V1,
            data = createOrderBasicSeaportDataV1().copy(
                protocol = Address.apply("0x00000000006c3852cbef3e08e8df289169ede581")
            ),
            signature = Binary.apply("0x6fdcf0e849e23451848d709f3c48d25108c0bf3ad8f256642d4aaa9b57f957585e1c77f8f87e35256bec2699b37cb9a000ed1d565ef5229f70f5201a2adb1b85")
        )
        assertThat(openSeaOrderValidator.validate(seaportOrder)).isTrue
    }

    @Test
    fun `should validate buy order`() {
        val buyOrder = OrderVersion(
            maker = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            taker = null,
            make = Asset(
                Erc20AssetType(
                    Address.apply("0xc778417e063141139fce010982780140aa0cd5ab")
                ),
                EthUInt256.of(13000000000000000)
            ),
            take = Asset(
                Erc721AssetType(
                    Address.apply("0x509fd4cdaa29be7b1fad251d8ea0fca2ca91eb60"),
                    EthUInt256.of(110711)
                ),
                EthUInt256.ONE
            ),
            type = OrderType.OPEN_SEA_V1,
            salt = EthUInt256.of(BigInteger("81538619411536663679971542969406122025226616498230290046022479480700489875715")),
            start = 1628140271,
            end = 1628745154,
            data = OrderOpenSeaV1DataV1(
                exchange = Address.apply("0x5206e78b21ce315ce284fb24cf05e0585a93b1d9"),
                makerRelayerFee = BigInteger.ZERO,
                takerRelayerFee = BigInteger.valueOf(250),
                makerProtocolFee = BigInteger.ZERO,
                takerProtocolFee = BigInteger.ZERO,
                feeRecipient = Address.apply("0x5b3256965e7c3cf26e11fcaf296dfc8807c01073"),
                feeMethod = OpenSeaOrderFeeMethod.SPLIT_FEE,
                side = OpenSeaOrderSide.BUY,
                saleKind = OpenSeaOrderSaleKind.FIXED_PRICE,
                howToCall = OpenSeaOrderHowToCall.CALL,
                callData = Binary.apply("0x23b872dd000000000000000000000000000000000000000000000000000000000000000000000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6000000000000000000000000000000000000000000000000000000000001b077"),
                replacementPattern = Binary.apply("0x00000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
                staticTarget = Address.ZERO(),
                staticExtraData = Binary.apply(),
                extra = BigInteger.ZERO,
                target = null,
                nonce = null,
            ),
            signature = Binary
                .apply("0x795def388ba0e82cf711448a6a36f64868d340b53a2f5277e9fc37651a156007")
                .add(Binary.apply("0x4c045da9e384ce70007ee08aa7602a1808a873d35ac2561f343ec1ab1d80ae4f"))
                .add(Binary.apply(byteArrayOf(28))),
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        ).let {
            it.copy(hash = Order.hash(it))
        }
        assertThat(openSeaOrderValidator.validate(buyOrder)).isTrue
    }

    @Test
    fun `should validate sell order`() {
        val buyOrder = OrderVersion(
            maker = Address.apply("0x61cc669f3eb0dc4c34206b99bba094f1022a8ca5"),
            taker = null,
            make = Asset(
                Erc721AssetType(
                    Address.apply("0xf43aaa80a8f9de69bc71aea989afceb8db7b690f"),
                    EthUInt256.of(BigInteger("7054"))
                ),
                EthUInt256.ONE
            ),
            take = Asset(
                EthAssetType,
                EthUInt256.of(100000000000000000)
            ),
            type = OrderType.OPEN_SEA_V1,
            salt = EthUInt256.of(BigInteger("55508393874026465186944315088619012378930272996363489732422158804309124571777")),
            start = 1645006343,
            end = 1645092843,
            data = OrderOpenSeaV1DataV1(
                exchange = Address.apply("0x7be8076f4ea4a4ad08075c2508e481d6c946d12b"),
                makerRelayerFee = BigInteger.valueOf(1250),
                takerRelayerFee = BigInteger.ZERO,
                makerProtocolFee = BigInteger.ZERO,
                takerProtocolFee = BigInteger.ZERO,
                feeRecipient = Address.apply("0x5b3256965e7c3cf26e11fcaf296dfc8807c01073"),
                feeMethod = OpenSeaOrderFeeMethod.SPLIT_FEE,
                side = OpenSeaOrderSide.SELL,
                saleKind = OpenSeaOrderSaleKind.FIXED_PRICE,
                howToCall = OpenSeaOrderHowToCall.CALL,
                callData = Binary.apply("0xf242432a00000000000000000000000061cc669f3eb0dc4c34206b99bba094f1022a8ca500000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001b8e000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000000"),
                replacementPattern = Binary.apply("0x000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
                staticTarget = Address.ZERO(),
                staticExtraData = Binary.apply(),
                extra = BigInteger.ZERO,
                target = Address.apply("0xf43aaa80a8f9de69bc71aea989afceb8db7b690f"),
                nonce = null,
            ),
            signature = Binary
                .apply("0x169742cbc3546a6f1847e5594424bf188916df731eb2da6439cc851b3474f9cf")
                .add(Binary.apply("0x7b12ac249e97d6a45aebcfb425c813c4dfb73e44284ebab8de45df059c949b8b"))
                .add(Binary.apply(byteArrayOf(27))),
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        ).let {
            it.copy(hash = Order.hash(it))
        }
        assertThat(openSeaOrderValidator.validate(buyOrder)).isTrue
    }

    @Test
    fun `should validate sell order with delegate call`() {
        val sellOrder = OrderVersion(
            maker = Address.apply("0x301f807b197d73f008897b1ac997c677337e10be"),
            taker = null,
            make = Asset(
                Erc721AssetType(
                    Address.apply("0xee0ba89699a3dd0f08cb516c069d81a762f65e56"),
                    EthUInt256.of(2093)
                ),
                EthUInt256.ONE
            ),
            take = Asset(
                EthAssetType,
                EthUInt256.of(250000000000000000)
            ),
            type = OrderType.OPEN_SEA_V1,
            salt = EthUInt256.of(BigInteger("9734063144914631932552722503382852712478064128767983837905229023810878955692")),
            start = 1645088141,
            end = 1645603876,
            data = OrderOpenSeaV1DataV1(
                exchange = Address.apply("0x7be8076f4ea4a4ad08075c2508e481d6c946d12b"),
                makerRelayerFee = BigInteger.valueOf(500),
                takerRelayerFee = BigInteger.ZERO,
                makerProtocolFee = BigInteger.ZERO,
                takerProtocolFee = BigInteger.ZERO,
                feeRecipient = Address.apply("0x5b3256965e7c3cf26e11fcaf296dfc8807c01073"),
                feeMethod = OpenSeaOrderFeeMethod.SPLIT_FEE,
                side = OpenSeaOrderSide.SELL,
                saleKind = OpenSeaOrderSaleKind.FIXED_PRICE,
                howToCall = OpenSeaOrderHowToCall.DELEGATE_CALL,
                callData = Binary.apply("0xfb16a595000000000000000000000000301f807b197d73f008897b1ac997c677337e10be0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000ee0ba89699a3dd0f08cb516c069d81a762f65e56000000000000000000000000000000000000000000000000000000000000082d000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000"),
                replacementPattern = Binary.apply("0x000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
                staticTarget = Address.ZERO(),
                staticExtraData = Binary.apply(),
                extra = BigInteger.ZERO,
                target = Address.apply("0xbaf2127b49fc93cbca6269fade0f7f31df4c88a7"),
                nonce = null,
            ),
            signature = Binary
                .apply("0xe0225be35caf54b3f8b0ec01a801fc0a5e4e93b8fc9e210f92e8d53635eef5cf")
                .add(Binary.apply("0x57d49567a0a0340f3dea23d792b8956224d252fde42fb93d3a99b410f5826538"))
                .add(Binary.apply(byteArrayOf(27))),
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        ).let {
            it.copy(hash = Order.hash(it))
        }
        assertThat(openSeaOrderValidator.validate(sellOrder)).isTrue
    }

    @Test
    fun `should validate buy order with delegate call`() {
        val buyOrder = OrderVersion(
            maker = Address.apply("0xd79c49696904ba297f71cfcb61026e4863a9eac0"),
            taker = null,
            make = Asset(
                Erc20AssetType(
                    Address.apply("0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2"),
                ),
                EthUInt256.of(450000000000000000)
            ),
            take = Asset(
                Erc721AssetType(
                    Address.apply("0x7cba74d0b16c8e18a9e48d3b7404d7739bb24f23"),
                    EthUInt256.of(9741)
                ),
                EthUInt256.ONE
            ),
            type = OrderType.OPEN_SEA_V1,
            salt = EthUInt256.of(BigInteger("4326950299410846034084849785542768189809240468762698308737214692728500662403")),
            start = 1645089145,
            end = 1645348442,
            data = OrderOpenSeaV1DataV1(
                exchange = Address.apply("0x7be8076f4ea4a4ad08075c2508e481d6c946d12b"),
                makerRelayerFee = BigInteger.ZERO,
                takerRelayerFee = BigInteger.valueOf(750),
                makerProtocolFee = BigInteger.ZERO,
                takerProtocolFee = BigInteger.ZERO,
                feeRecipient = Address.apply("0x5b3256965e7c3cf26e11fcaf296dfc8807c01073"),
                feeMethod = OpenSeaOrderFeeMethod.SPLIT_FEE,
                side = OpenSeaOrderSide.BUY,
                saleKind = OpenSeaOrderSaleKind.FIXED_PRICE,
                howToCall = OpenSeaOrderHowToCall.DELEGATE_CALL,
                callData = Binary.apply("0xfb16a5950000000000000000000000000000000000000000000000000000000000000000000000000000000000000000d79c49696904ba297f71cfcb61026e4863a9eac00000000000000000000000007cba74d0b16c8e18a9e48d3b7404d7739bb24f23000000000000000000000000000000000000000000000000000000000000260d000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000"),
                replacementPattern = Binary.apply("0x00000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
                staticTarget = Address.ZERO(),
                staticExtraData = Binary.apply(),
                extra = BigInteger.ZERO,
                target = Address.apply("0xbaf2127b49fc93cbca6269fade0f7f31df4c88a7"),
                nonce = null,
            ),
            signature = Binary
                .apply("0x9ed89b7b7135f528b9af24bfcec47e8bccc4c31e78e0def0783f3be37e9b65d3")
                .add(Binary.apply("0x6076354d317a923e90f1d968fbeb8b921cd59c47ea01bf3c9acbef705406d6fe"))
                .add(Binary.apply(byteArrayOf(28))),
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        ).let {
            it.copy(hash = Order.hash(it))
        }
        assertThat(openSeaOrderValidator.validate(buyOrder)).isTrue
    }

    @Test
    fun `should not validate order with invalid signature`() {
        val buyOrder = OrderVersion(
            maker = Address.apply("0xd79c49696904ba297f71cfcb61026e4863a9eac0"),
            taker = null,
            make = Asset(
                Erc20AssetType(
                    Address.apply("0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2"),
                ),
                EthUInt256.of(450000000000000000)
            ),
            take = Asset(
                Erc721AssetType(
                    Address.apply("0x7cba74d0b16c8e18a9e48d3b7404d7739bb24f23"),
                    EthUInt256.of(9741)
                ),
                EthUInt256.ONE
            ),
            type = OrderType.OPEN_SEA_V1,
            salt = EthUInt256.of(BigInteger("4326950299410846034084849785542768189809240468762698308737214692728500662403")),
            start = 1645089145,
            end = 1645348442,
            data = OrderOpenSeaV1DataV1(
                exchange = Address.apply("0x7be8076f4ea4a4ad08075c2508e481d6c946d12b"),
                makerRelayerFee = BigInteger.ZERO,
                takerRelayerFee = BigInteger.valueOf(750),
                makerProtocolFee = BigInteger.ZERO,
                takerProtocolFee = BigInteger.ZERO,
                feeRecipient = Address.apply("0x5b3256965e7c3cf26e11fcaf296dfc8807c01073"),
                feeMethod = OpenSeaOrderFeeMethod.SPLIT_FEE,
                side = OpenSeaOrderSide.BUY,
                saleKind = OpenSeaOrderSaleKind.FIXED_PRICE,
                howToCall = OpenSeaOrderHowToCall.DELEGATE_CALL,
                callData = Binary.apply("0xfb16a5950000000000000000000000000000000000000000000000000000000000000000000000000000000000000000d79c49696904ba297f71cfcb61026e4863a9eac00000000000000000000000007cba74d0b16c8e18a9e48d3b7404d7739bb24f23000000000000000000000000000000000000000000000000000000000000260d000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000"),
                replacementPattern = Binary.apply("0x00000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
                staticTarget = Address.ZERO(),
                staticExtraData = Binary.apply(),
                extra = BigInteger.ZERO,
                target = Address.apply("0xbaf2127b49fc93cbca6269fade0f7f31df4c88a7"),
                nonce = null,
            ),
            signature = Binary
                .apply("0x0ed89b7b7135f528b9af24bfcec47e8bccc4c31e78e0def0783f3be37e9b65d3")
                .add(Binary.apply("0x6076354d317a923e90f1d968fbeb8b921cd59c47ea01bf3c9acbef705406d6fe"))
                .add(Binary.apply(byteArrayOf(28))),
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        ).let {
            it.copy(hash = Order.hash(it))
        }
        assertThat(openSeaOrderValidator.validate(buyOrder)).isFalse
    }

    @Test
    fun `should not validate order with invalid calldata`() {
        val buyOrder = OrderVersion(
            maker = Address.apply("0xd79c49696904ba297f71cfcb61026e4863a9eac0"),
            taker = null,
            make = Asset(
                Erc20AssetType(
                    Address.apply("0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2"),
                ),
                EthUInt256.of(450000000000000000)
            ),
            take = Asset(
                Erc721AssetType(
                    Address.apply("0x7cba74d0b16c8e18a9e48d3b7404d7739bb24f23"),
                    EthUInt256.of(9741)
                ),
                EthUInt256.ONE
            ),
            type = OrderType.OPEN_SEA_V1,
            salt = EthUInt256.of(BigInteger("4326950299410846034084849785542768189809240468762698308737214692728500662403")),
            start = 1645089145,
            end = 1645348442,
            data = OrderOpenSeaV1DataV1(
                exchange = Address.apply("0x7be8076f4ea4a4ad08075c2508e481d6c946d12b"),
                makerRelayerFee = BigInteger.ZERO,
                takerRelayerFee = BigInteger.valueOf(750),
                makerProtocolFee = BigInteger.ZERO,
                takerProtocolFee = BigInteger.ZERO,
                feeRecipient = Address.apply("0x5b3256965e7c3cf26e11fcaf296dfc8807c01073"),
                feeMethod = OpenSeaOrderFeeMethod.SPLIT_FEE,
                side = OpenSeaOrderSide.BUY,
                saleKind = OpenSeaOrderSaleKind.FIXED_PRICE,
                howToCall = OpenSeaOrderHowToCall.DELEGATE_CALL,
                callData = Binary.apply("0xfa16a5950000000000000000000000000000000000000000000000000000000000000000000000000000000000000000d79c49696904ba297f71cfcb61026e4863a9eac00000000000000000000000007cba74d0b16c8e18a9e48d3b7404d7739bb24f23000000000000000000000000000000000000000000000000000000000000260d000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000"),
                replacementPattern = Binary.apply("0x00000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
                staticTarget = Address.ZERO(),
                staticExtraData = Binary.apply(),
                extra = BigInteger.ZERO,
                target = Address.apply("0xbaf2127b49fc93cbca6269fade0f7f31df4c88a7"),
                nonce = null,
            ),
            signature = Binary
                .apply("0x9ed89b7b7135f528b9af24bfcec47e8bccc4c31e78e0def0783f3be37e9b65d3")
                .add(Binary.apply("0x6076354d317a923e90f1d968fbeb8b921cd59c47ea01bf3c9acbef705406d6fe"))
                .add(Binary.apply(byteArrayOf(28))),
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        ).let {
            it.copy(hash = Order.hash(it))
        }
        assertThat(openSeaOrderValidator.validate(buyOrder)).isFalse
    }
}
