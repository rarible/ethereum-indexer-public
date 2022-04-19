package com.rarible.protocol.order.listener.service.zero.ex

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
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
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import scalether.domain.Address
import java.time.Instant

@ExtendWith(MockKExtension::class)
class ZeroExOrderEventConverterTest {

    private lateinit var zeroExOrderEventConverter: ZeroExOrderEventConverter

    @MockK
    private lateinit var priceUpdateService: PriceUpdateService

    @MockK
    private lateinit var priceNormalizer: PriceNormalizer

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
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
            exchangeDomainHash = "0x17068c8fc502c4938835d37c402e7c17f51ec6895246726893d5fe3198085a67"
        )
    }

    @Test
    fun `convert by left order log`() = runBlocking<Unit> {
        val from = Address.ONE()
        val date = Instant.now()
        val orderHash = Word.apply("0x935b13465952ccbf981a264761b2edf0c0edf46f2e62a6a57ab37a32b224d6b0")
        val makerAddress = leftOrder.makerAddress
        val takerAssetFilledAmount = 50.toBigInteger()

        val result = zeroExOrderEventConverter.convert(
            matchOrdersData = data,
            from = from,
            date = date,
            orderHash = orderHash,
            makerAddress = makerAddress,
            takerAssetFilledAmount = takerAssetFilledAmount,
        )

        assertThat(result).containsExactly(
            OrderSideMatch(
                hash = orderHash,
                counterHash = Word.apply("0xdcbcc2880a85e7b7e1b80c2fd714dcd6f275ee203298a52a0702b75691aec628"),
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

    @Test
    fun `convert by right order log`() = runBlocking<Unit> {
        val from = Address.ONE()
        val date = Instant.now()
        val orderHash = Word.apply("0xdcbcc2880a85e7b7e1b80c2fd714dcd6f275ee203298a52a0702b75691aec628")
        val makerAddress = rightOrder.makerAddress
        val takerAssetFilledAmount = 50.toBigInteger()

        val result = zeroExOrderEventConverter.convert(
            matchOrdersData = data,
            from = from,
            date = date,
            orderHash = orderHash,
            makerAddress = makerAddress,
            takerAssetFilledAmount = takerAssetFilledAmount,
        )

        assertThat(result).containsExactly(
            OrderSideMatch(
                hash = orderHash,
                counterHash = Word.apply("0x935b13465952ccbf981a264761b2edf0c0edf46f2e62a6a57ab37a32b224d6b0"),
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

    private companion object {
        val leftOrder = ZeroExOrder(
            // продавец в ордере (он продавец валюты за nft), покупатель в сделке - покупатель nft
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
            // продавец в ордере (он продавец nft за валюту), продавец в сделке - продавец nft
            makerAddress = Address.apply("0xdfa346c49c159c58d8316978b0f721ecebd10a3c"),
            takerAddress = Address.apply("0x0000000000000000000000000000000000000000"),
            // получатель комиссии - это zero ex fee wrapper
            feeRecipientAddress = Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"),
            // отправитель - это zero ex fee wrapper
            senderAddress = Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"),
            // кол-во продаваемого asset
            makerAssetAmount = 1.toBigInteger(),
            // кол-во покупаемого asset
            takerAssetAmount = 1520000000000000.toBigInteger(),
            // комиссия продавца
            makerFee = 190000000000000.toBigInteger(),
            // комиссия покупателя
            takerFee = 0.toBigInteger(),
            expirationTimeSeconds = 1650013671.toBigInteger(),
            salt = 71765602873448650.toBigInteger(),
            // asset продаваемый
            makerAssetData = Binary.apply("0x025717920000000000000000000000002b4a66557a79263275826ad31a4cddc2789334bd000000000000000000000000000000000000000000000000000000000000762b"),
            // asset покупаемый - WETH
            takerAssetData = Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619"),
            // валюта комиссии продавца - WETH
            makerFeeAssetData = Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619"),
            // валюта комиссии покупателя
            takerFeeAssetData = Binary.apply("0x")
        )
        val leftSignature: Binary =
            Binary.apply("0x1c7763a38bdc4a8cc3bfdab4302a54c5c76f0aafb5ff54fafb4a809d34d84277dd3a940201f11fd8bf824c2732af189ef6f922ef351157ea0d3a3c13e6c4543db303")
        val rightSignature: Binary =
            Binary.apply("0x1c40e0c8008601b2df04d165574e899598e63caa7f7a68f2c2ba274bd088c56e8162513d0f1a69a31770981117d8533133295cef7fa4ec5978376caa8e7ff8c0d103")

        // кому перечислит fee feeRecipientAddress и в каком размере
        // (zero ex fee wrapper передаст полученные 190000000000000 WETH этим адресам)
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
}