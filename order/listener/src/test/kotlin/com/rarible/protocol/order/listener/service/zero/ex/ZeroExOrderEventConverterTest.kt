package com.rarible.protocol.order.listener.service.zero.ex

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.ZeroExFeeData
import com.rarible.protocol.order.core.model.ZeroExMatchOrdersData
import com.rarible.protocol.order.core.model.ZeroExOrder
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant

@ExtendWith(MockKExtension::class)
class ZeroExOrderEventConverterTest {

    private lateinit var zeroExOrderEventConverter: ZeroExOrderEventConverter

    @MockK
    private lateinit var priceUpdateService: PriceUpdateService

    @MockK
    private lateinit var priceNormalizer: PriceNormalizer

    @BeforeEach
    fun setUp() = runBlocking {
        every {
            runBlocking {
                priceUpdateService.getAssetsUsdValue(any(), any(), any())
            }
        } returns null
        every {
            runBlocking {
                priceNormalizer.normalize(any())
            }
        } returns 1.toBigDecimal()
        zeroExOrderEventConverter = ZeroExOrderEventConverter(
            priceUpdateService = priceUpdateService,
            priceNormalizer = priceNormalizer,
            properties = OrderListenerProperties().copy(zeroExExchangeDomainHash = "0x17068c8fc502c4938835d37c402e7c17f51ec6895246726893d5fe3198085a67")
        )
    }

    @Test
    fun `convert by left order log`() = runBlocking<Unit> {
        with(Erc721Data) {
            // https://polygonscan.com/tx/0x08ef7447c15669631b1b54bf6b035df31c0b3b19720a760b2093db8b1eeb14cb
            val from = Address.ONE()
            val date = Instant.now()
            val orderHash = leftOrderHash
            val counterHash = rightOrderHash
            val makerAddress = leftOrder.makerAddress
            val makerAssetFilledAmount = 1520000000000000.toBigInteger()
            val takerAssetFilledAmount = 1.toBigInteger()

            val result = zeroExOrderEventConverter.convert(
                matchOrdersData = data,
                from = from,
                date = date,
                orderHash = orderHash,
                makerAddress = makerAddress,
                makerAssetFilledAmount = makerAssetFilledAmount,
                takerAssetFilledAmount = takerAssetFilledAmount,
                input = WordFactory.create(),
            )

            assertThat(result).containsExactly(
                OrderSideMatch(
                    hash = orderHash,
                    counterHash = counterHash,
                    side = OrderSide.RIGHT,
                    fill = EthUInt256(takerAssetFilledAmount),
                    make = leftMakeErc20Asset,
                    take = leftTakeErc721Asset,
                    maker = leftOrder.makerAddress,
                    taker = rightOrder.makerAddress,
                    makeUsd = null,
                    takeUsd = null,
                    makePriceUsd = null,
                    takePriceUsd = null,
                    makeValue = 1.toBigDecimal(),
                    takeValue = 1.toBigDecimal(),
                    date = date,
                    source = HistorySource.OPEN_SEA,
                    externalOrderExecutedOnRarible = false,
                    adhoc = false,
                    counterAdhoc = false,
                ),
            )
        }
    }

    @Test
    fun `convert by right order log`() = runBlocking<Unit> {
        with(Erc721Data) {
            val from = Address.ONE()
            val date = Instant.now()
            val orderHash = rightOrderHash
            val counterHash = leftOrderHash
            val makerAddress = rightOrder.makerAddress
            val makerAssetFilledAmount = 1.toBigInteger()
            val takerAssetFilledAmount = 1520000000000000.toBigInteger()

            val result = zeroExOrderEventConverter.convert(
                matchOrdersData = data,
                from = from,
                date = date,
                orderHash = orderHash,
                makerAddress = makerAddress,
                makerAssetFilledAmount = makerAssetFilledAmount,
                takerAssetFilledAmount = takerAssetFilledAmount,
                input = WordFactory.create(),
            )

            assertThat(result).containsExactly(
                OrderSideMatch(
                    hash = orderHash,
                    counterHash = counterHash,
                    side = OrderSide.LEFT,
                    fill = EthUInt256(takerAssetFilledAmount),
                    make = leftTakeErc721Asset,
                    take = leftMakeErc20Asset,
                    maker = rightOrder.makerAddress,
                    taker = leftOrder.makerAddress,
                    makeUsd = null,
                    takeUsd = null,
                    makePriceUsd = null,
                    takePriceUsd = null,
                    makeValue = 1.toBigDecimal(),
                    takeValue = 1.toBigDecimal(),
                    date = date,
                    source = HistorySource.OPEN_SEA,
                    externalOrderExecutedOnRarible = false,
                    adhoc = false,
                    counterAdhoc = false,
                ),
            )
        }
    }

    private object Erc721Data {
        val leftOrderHash: Word = Word.apply("0x935b13465952ccbf981a264761b2edf0c0edf46f2e62a6a57ab37a32b224d6b0")
        val rightOrderHash: Word = Word.apply("0xdcbcc2880a85e7b7e1b80c2fd714dcd6f275ee203298a52a0702b75691aec628")

        val leftOrder = ZeroExOrder(
            // seller of order (he sells erc20 and buys nft), it's buyer of nft in this deal
            makerAddress = Address.apply("0x4d3b39791d9bfe56304b32c35fe8f3d411d85a02"),
            takerAddress = Address.apply("0x0000000000000000000000000000000000000000"),
            feeRecipientAddress = Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"),
            senderAddress = Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"),
            makerAssetAmount = 1520000000000000.toBigInteger(),
            takerAssetAmount = 1.toBigInteger(),
            makerFee = 0.toBigInteger(),
            takerFee = 0.toBigInteger(),
            expirationTimeSeconds = 1650013671.toBigInteger(),
            salt = 97119520864459265.toBigInteger(),
            makerAssetData = Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619"),
            takerAssetData = Binary.apply("0x025717920000000000000000000000002b4a66557a79263275826ad31a4cddc2789334bd000000000000000000000000000000000000000000000000000000000000762b"),
            makerFeeAssetData = Binary.apply("0x"),
            takerFeeAssetData = Binary.apply("0x")
        )
        val rightOrder = ZeroExOrder(
            // seller of order (he sells nft and buys erc20), it's seller of nft in this deal
            makerAddress = Address.apply("0xdfa346c49c159c58d8316978b0f721ecebd10a3c"),
            takerAddress = Address.apply("0x0000000000000000000000000000000000000000"),
            // fee receiver - zero ex fee wrapper
            feeRecipientAddress = Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"),
            // sender - zero ex fee wrapper
            senderAddress = Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"),
            // how much asset he sells
            makerAssetAmount = 1.toBigInteger(),
            // how much asset he buys
            takerAssetAmount = 1520000000000000.toBigInteger(),
            // seller fee
            makerFee = 190000000000000.toBigInteger(),
            // buyer fee
            takerFee = 0.toBigInteger(),
            expirationTimeSeconds = 1650013671.toBigInteger(),
            salt = 71765602873448650.toBigInteger(),
            // selling asset
            makerAssetData = Binary.apply("0x025717920000000000000000000000002b4a66557a79263275826ad31a4cddc2789334bd000000000000000000000000000000000000000000000000000000000000762b"),
            // buying asset - WETH
            takerAssetData = Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619"),
            // seller fee asset - WETH
            makerFeeAssetData = Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619"),
            // buyer fee asset
            takerFeeAssetData = Binary.apply("0x")
        )
        val leftSignature: Binary =
            Binary.apply("0x1c7763a38bdc4a8cc3bfdab4302a54c5c76f0aafb5ff54fafb4a809d34d84277dd3a940201f11fd8bf824c2732af189ef6f922ef351157ea0d3a3c13e6c4543db303")
        val rightSignature: Binary =
            Binary.apply("0x1c40e0c8008601b2df04d165574e899598e63caa7f7a68f2c2ba274bd088c56e8162513d0f1a69a31770981117d8533133295cef7fa4ec5978376caa8e7ff8c0d103")

        // to whom and how much will feeRecipientAddress send fee
        // (zero ex fee wrapper will send received 190000000000000 WETH to these addresses)
        val feeData: List<ZeroExFeeData> = listOf(
            ZeroExFeeData(
                recipient = Address.apply("0x5b3256965e7c3cf26e11fcaf296dfc8807c01073"),
                paymentTokenAmount = 38000000000000.toBigInteger()
            ),
            ZeroExFeeData(
                recipient = Address.apply("0x0bbdd174198c3bafff09f58d62119e680141ab44"),
                paymentTokenAmount = 152000000000000.toBigInteger()
            )
        )
        val paymentTokenAddress: Address = Address.apply("0x7ceb23fd6bc0add59e62ac25578270cff1b9f619")
        val data = ZeroExMatchOrdersData(
            leftOrder = leftOrder,
            takerAddress = null,
            rightOrder = rightOrder,
            leftSignature = leftSignature,
            rightSignature = rightSignature,
            feeData = feeData,
            paymentTokenAddress = paymentTokenAddress,
        )
        val leftMakeErc20Asset = Asset(
            type = Erc20AssetType(token = Address.apply("0x7ceb23fd6bc0add59e62ac25578270cff1b9f619")),
            value = EthUInt256.of(leftOrder.makerAssetAmount)
        )
        val leftTakeErc721Asset = Asset(
            type = Erc721AssetType(
                token = Address.apply("0x2b4a66557a79263275826ad31a4cddc2789334bd"),
                tokenId = EthUInt256.of("0x000000000000000000000000000000000000000000000000000000000000762b")
            ),
            value = EthUInt256.ONE
        )
    }

    @Test
    fun `convert by left order log for buying 1155`() = runBlocking<Unit> {
        with(Erc1155Data) {
            // https://polygonscan.com/tx/0x7a91f7df871fa7718a4057684951d476db3fa1427604c335fd760bb3bc9ac49e
            val from = Address.ONE()
            val date = Instant.now()
            val orderHash = leftOrderHash
            val counterHash = rightOrderHash
            val makerAddress = leftOrder.makerAddress
            val makerAssetFilledAmount = 3251429816261702.toBigInteger()
            val takerAssetFilledAmount = 2000000000000000000.toBigInteger()

            val result = zeroExOrderEventConverter.convert(
                matchOrdersData = data,
                from = from,
                date = date,
                orderHash = orderHash,
                makerAddress = makerAddress,
                makerAssetFilledAmount = makerAssetFilledAmount,
                takerAssetFilledAmount = takerAssetFilledAmount,
                input = WordFactory.create(),
            )

            assertThat(result).containsExactly(
                OrderSideMatch(
                    hash = orderHash,
                    counterHash = counterHash,
                    side = OrderSide.RIGHT,
                    fill = EthUInt256(takerAssetFilledAmount),
                    make = erc20Asset(makerAssetFilledAmount),
                    take = erc1155Asset(takerAssetFilledAmount),
                    maker = leftOrder.makerAddress,
                    taker = rightOrder.makerAddress,
                    makeUsd = null,
                    takeUsd = null,
                    makePriceUsd = null,
                    takePriceUsd = null,
                    makeValue = 1.toBigDecimal(),
                    takeValue = 1.toBigDecimal(),
                    date = date,
                    source = HistorySource.OPEN_SEA,
                    externalOrderExecutedOnRarible = false,
                    adhoc = false,
                    counterAdhoc = false,
                ),
            )
        }
    }

    @Test
    fun `convert by right order log for buying 1155`() = runBlocking<Unit> {
        with(Erc1155Data) {
            val from = Address.ONE()
            val date = Instant.now()
            val orderHash = rightOrderHash
            val counterHash = leftOrderHash
            val makerAddress = rightOrder.makerAddress
            val makerAssetFilledAmount = 2000000000000000000.toBigInteger()
            val takerAssetFilledAmount = 3251429816261702.toBigInteger()

            val result = zeroExOrderEventConverter.convert(
                matchOrdersData = data,
                from = from,
                date = date,
                orderHash = orderHash,
                makerAddress = makerAddress,
                makerAssetFilledAmount = makerAssetFilledAmount,
                takerAssetFilledAmount = takerAssetFilledAmount,
                input = WordFactory.create(),
            )

            assertThat(result).containsExactly(
                OrderSideMatch(
                    hash = orderHash,
                    counterHash = counterHash,
                    side = OrderSide.LEFT,
                    fill = EthUInt256(takerAssetFilledAmount),
                    make = erc1155Asset(makerAssetFilledAmount),
                    take = erc20Asset(takerAssetFilledAmount),
                    maker = rightOrder.makerAddress,
                    taker = leftOrder.makerAddress,
                    makeUsd = null,
                    takeUsd = null,
                    makePriceUsd = null,
                    takePriceUsd = null,
                    makeValue = 1.toBigDecimal(),
                    takeValue = 1.toBigDecimal(),
                    date = date,
                    source = HistorySource.OPEN_SEA,
                    externalOrderExecutedOnRarible = false,
                    adhoc = false,
                    counterAdhoc = false,
                ),
            )
        }
    }

    private object Erc1155Data {
        val leftOrderHash: Word = Word.apply("0xef68293b01ac69bc07565eff24076efaa54d467933fed48f4a5108bf227274f4")
        val rightOrderHash: Word = Word.apply("0xf3c8f29c6bc8c7ae4574de304b42f7e35a38848144c8429ff01ed749e13c6d77")

        val leftOrder = ZeroExOrder(
            // seller of order (he sells erc20 and buys nft), it's buyer of nft in this deal
            makerAddress = Address.apply("0x06737052e87392acad6b5a23c8ded8dd8e4db07d"),
            takerAddress = Address.apply("0x0000000000000000000000000000000000000000"),
            feeRecipientAddress = Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"),
            senderAddress = Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"),
            makerAssetAmount = 3251429816261702.toBigInteger(),
            takerAssetAmount = 2000000000000000000.toBigInteger(),
            makerFee = 0.toBigInteger(),
            takerFee = 0.toBigInteger(),
            expirationTimeSeconds = 1650934720.toBigInteger(),
            salt = 54392042574797132.toBigInteger(),
            makerAssetData = Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619"),
            takerAssetData = Binary.apply("0xa7cb5fb700000000000000000000000022d5f9b75c524fec1d6619787e582644cd4d7422000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000d10000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
            makerFeeAssetData = Binary.apply("0x"),
            takerFeeAssetData = Binary.apply("0x")
        )
        val rightOrder = ZeroExOrder(
            // seller of order (he sells nft and buys erc20), it's seller of nft in this deal
            makerAddress = Address.apply("0xf10fb2fd902cbeb9bccef76cc9f4756eff76c92c"),
            takerAddress = Address.apply("0x0000000000000000000000000000000000000000"),
            // fee receiver - zero ex fee wrapper
            feeRecipientAddress = Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"),
            // sender - zero ex fee wrapper
            senderAddress = Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"),
            // how much asset he sells
            makerAssetAmount = "19000000000000000000".toBigInteger(),
            // how much asset he buys
            takerAssetAmount = 30888583254486169.toBigInteger(),
            // seller fee
            makerFee = 3861072906810771.toBigInteger(),
            // buyer fee
            takerFee = 0.toBigInteger(),
            expirationTimeSeconds = 1650934720.toBigInteger(),
            salt = 98036274358853111.toBigInteger(),
            // selling asset
            makerAssetData = Binary.apply("0xa7cb5fb700000000000000000000000022d5f9b75c524fec1d6619787e582644cd4d7422000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000d10000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
            // buying asset - WETH
            takerAssetData = Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619"),
            // seller fee asset - WETH
            makerFeeAssetData = Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619"),
            // buyer fee asset
            takerFeeAssetData = Binary.apply("0x")
        )
        val leftSignature: Binary =
            Binary.apply("0x1bd8e7a7950db289cf65676124f061995b92bcec6b0310414629be8e0ed480764f6f7d3d12bb8197f102d9d9939cb2f59ecbb2bfaf89a461480f4751a6b4d4633102")
        val rightSignature: Binary =
            Binary.apply("0x1b375de0aa1bf7802aa01cacd70ff7beaaa3ba8aca145802f171d877a09cef18c25cee2b100c15de460db738a30178d9776d33615fec28b6eefd0afd0c2ac942eb02")

        // to whom and how much will feeRecipientAddress send fee
        val feeData = listOf(
            ZeroExFeeData(
                recipient = Address.apply("0x5b3256965e7c3cf26e11fcaf296dfc8807c01073"),
                paymentTokenAmount = 81285745406542.toBigInteger()
            ),
            ZeroExFeeData(
                recipient = Address.apply("0x0bbdd174198c3bafff09f58d62119e680141ab44"),
                paymentTokenAmount = 325142981626170.toBigInteger()
            )
        )
        val paymentTokenAddress: Address = Address.apply("0x7ceb23fd6bc0add59e62ac25578270cff1b9f619")
        val data = ZeroExMatchOrdersData(
            leftOrder = leftOrder,
            takerAddress = null,
            rightOrder = rightOrder,
            leftSignature = leftSignature,
            rightSignature = rightSignature,
            feeData = feeData,
            paymentTokenAddress = paymentTokenAddress,
        )

        fun erc20Asset(amount: BigInteger) = Asset(
            type = Erc20AssetType(token = Address.apply("0x7ceb23fd6bc0add59e62ac25578270cff1b9f619")),
            value = EthUInt256.of(amount)
        )

        fun erc1155Asset(amount: BigInteger) = Asset(
            type = Erc1155AssetType(
                // https://opensea.io/assets/matic/0x22d5f9b75c524fec1d6619787e582644cd4d7422/209
                token = Address.apply("0x22d5f9b75c524fec1d6619787e582644cd4d7422"),
                tokenId = EthUInt256.of("0x00000000000000000000000000000000000000000000000000000000000000d1")
            ),
            value = EthUInt256.of(amount)
        )
    }
}
