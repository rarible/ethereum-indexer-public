package com.rarible.protocol.order.listener.service.opensea

import com.rarible.contracts.erc1155.IERC1155
import com.rarible.contracts.erc721.IERC721
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import io.daonomic.rpc.domain.Binary
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.time.Instant
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

@Component
class OpenSeaOrderEventConverter(
    private val priceUpdateService: PriceUpdateService,
    private val prizeNormalizer: PriceNormalizer
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun convert(openSeaOrders: OpenSeaMatchedOrders, price: BigInteger, date: Instant): List<OrderSideMatch> {
        val externalOrderExecutedOnRarible = openSeaOrders.externalOrderExecutedOnRarible
        val buyOrder = openSeaOrders.buyOrder
        val buyOrderSide = getBuyOrderSide(openSeaOrders)

        val sellOrder = openSeaOrders.sellOrder
        val sellOrderSide = buyOrderSide.revert()

        val buyCallData = Arrays.copyOfRange(buyOrder.callData.bytes(), 0, buyOrder.callData.bytes().size)
        val sellCallData = Arrays.copyOfRange(sellOrder.callData.bytes(), 0, sellOrder.callData.bytes().size)

        if (buyOrder.replacementPattern.length() > 0) {
            applyMask(buyCallData, sellCallData, buyOrder.replacementPattern.bytes())
        }
        if (sellOrder.replacementPattern.length() > 0) {
            applyMask(sellCallData, buyCallData, sellOrder.replacementPattern.bytes())
        }

        require(Arrays.equals(sellCallData, buyCallData)) { "buy and sell call data must equals" }
        require(buyOrder.target == sellOrder.target) { "buy and sell target token must equals" }
        require(buyOrder.paymentToken == sellOrder.paymentToken) { "buy and sell payment token must equals" }

        val transfer = encodeTransfer(Binary.apply(sellCallData)) ?: return emptyList()
        val nftAsset = createNftAsset(sellOrder.target, transfer.tokenId, transfer.value, transfer.type)
        val paymentAsset = createPaymentAsset(price, buyOrder.paymentToken)

        val at = nowMillis()
        val buyUsdValue = priceUpdateService.getAssetsUsdValue(make = paymentAsset, take = nftAsset, at = at)
        val sellUsdValue = priceUpdateService.getAssetsUsdValue(make = nftAsset, take = paymentAsset, at = at)
        val buyAdhoc = EthUInt256.of(buyOrder.salt) == EthUInt256.ZERO
        val sellAdhoc = EthUInt256.of(sellOrder.salt) == EthUInt256.ZERO

        return listOf(
            OrderSideMatch(
                hash = buyOrder.hash,
                counterHash = sellOrder.hash,
                side = buyOrderSide,
                make = paymentAsset,
                take = nftAsset,
                fill = nftAsset.value,
                maker = buyOrder.maker,
                taker = sellOrder.maker,
                makeUsd = buyUsdValue?.makeUsd,
                takeUsd = buyUsdValue?.takeUsd,
                makeValue = prizeNormalizer.normalize(paymentAsset),
                takeValue = prizeNormalizer.normalize(nftAsset),
                makePriceUsd = buyUsdValue?.makePriceUsd,
                takePriceUsd = buyUsdValue?.takePriceUsd,
                source = HistorySource.OPEN_SEA,
                externalOrderExecutedOnRarible = externalOrderExecutedOnRarible,
                date = date,
                adhoc = buyAdhoc,
                counterAdhoc = sellAdhoc
            ),
            OrderSideMatch(
                hash = sellOrder.hash,
                counterHash = buyOrder.hash,
                side = sellOrderSide,
                make = nftAsset,
                take = paymentAsset,
                fill = EthUInt256.of(price),
                maker = sellOrder.maker,
                taker = buyOrder.maker,
                makeUsd = sellUsdValue?.makeUsd,
                takeUsd = sellUsdValue?.takeUsd,
                makeValue = prizeNormalizer.normalize(nftAsset),
                takeValue = prizeNormalizer.normalize(paymentAsset),
                makePriceUsd = sellUsdValue?.makePriceUsd,
                takePriceUsd = sellUsdValue?.takePriceUsd,
                externalOrderExecutedOnRarible = externalOrderExecutedOnRarible,
                date = date,
                source = HistorySource.OPEN_SEA,
                adhoc = sellAdhoc,
                counterAdhoc = buyAdhoc
            )
        )
    }

    suspend fun convert(order: OpenSeaTransactionOrder, date: Instant): List<OrderCancel> {
        val transfer = encodeTransfer(order.callData) ?: return emptyList()
        val nftAsset = createNftAsset(order.target, transfer.tokenId, transfer.value, transfer.type)
        val paymentAsset = createPaymentAsset(order.basePrice, order.paymentToken)
        val (make, take) = when (order.side) {
            OpenSeaOrderSide.SELL -> nftAsset to paymentAsset
            OpenSeaOrderSide.BUY -> paymentAsset to nftAsset
        }
        return listOf(
            OrderCancel(
                hash = order.hash,
                make = make.copy(value = EthUInt256.ZERO),
                take = take.copy(value = EthUInt256.ZERO),
                maker = order.maker,
                date = date,
                source = HistorySource.OPEN_SEA
            )
        )
    }

    private fun getBuyOrderSide(orders: OpenSeaMatchedOrders): OrderSide {
        return when (orders.buyOrder.taker) {
            //This is a case of bid order (usually buying using WETH)
            Address.ZERO() -> {
                require(orders.sellOrder.maker != Address.ZERO())
                OrderSide.LEFT
            }
            //This is a case of sell order (usually buying using ETH)
            else -> {
                require(orders.buyOrder.maker != Address.ZERO())
                OrderSide.RIGHT
            }
        }
    }

    private fun createNftAsset(
        token: Address,
        tokenId: BigInteger,
        value: BigInteger,
        type: NftType
    ): Asset {
        return when (type) {
            NftType.ERC1155 -> Asset(
                type = Erc1155AssetType(
                    token = token,
                    tokenId = EthUInt256.of(tokenId)
                ),
                value = EthUInt256.of(value)
            )
            NftType.ERC721 -> Asset(
                type = Erc721AssetType(
                    token = token,
                    tokenId = EthUInt256.of(tokenId)
                ),
                value = EthUInt256.ONE
            )
        }
    }

    private fun createPaymentAsset(
        price: BigInteger,
        paymentToken: Address
    ): Asset {
        return  if (paymentToken != Address.ZERO()) {
            Asset(
                type = Erc20AssetType(paymentToken),
                value = EthUInt256.of(price)
            )
        } else {
            Asset(
                type = EthAssetType,
                value = EthUInt256.of(price)
            )
        }
    }

    private fun encodeTransfer(callData: Binary): Transfer? {
        return when (callData.slice(0, 4)) {
            IERC1155.safeTransferFromSignature().id() -> {
                val encoded = IERC1155.safeTransferFromSignature().`in`().decode(callData, 4)
                Transfer(
                    type = NftType.ERC1155,
                    from = encoded.value()._1(),
                    to = encoded.value()._2(),
                    tokenId = encoded.value()._3(),
                    value = encoded.value()._4()
                )
            }
            IERC721.transferFromSignature().id(), IERC721.safeTransferFromSignature() -> {
                val encoded = IERC721.safeTransferFromSignature().`in`().decode(callData, 4)
                Transfer(
                    type = NftType.ERC721,
                    from = encoded.value()._1(),
                    to = encoded.value()._2(),
                    tokenId = encoded.value()._3(),
                    value = BigInteger.ONE
                )
            }
            Binary.apply("68f0bcaa") -> {
                null // TODO: Need support
            }
            else -> {
                logger.warn("Unsupported OpenSea order call data: $callData")
                null
            }
        }
    }

    private fun applyMask(array: ByteArray, desired: ByteArray, mask: ByteArray) {
        require(array.size == desired.size) { "array and desired must be the same size" }
        require(array.size == mask.size) { "array and mask must be the same size" }

        for (i in array.indices) {
            array[i] = ((mask[i] xor 0xff.toByte()) and array[i]) or (mask[i] and desired[i])
        }
    }

    private fun OrderSide.revert(): OrderSide {
        return when (this) {
            OrderSide.LEFT -> OrderSide.RIGHT
            OrderSide.RIGHT -> OrderSide.LEFT
        }
    }

    private data class Transfer(
        val type: NftType,
        val from: Address,
        val to: Address,
        val tokenId: BigInteger,
        val value: BigInteger
    )

    private enum class NftType {
        ERC721,
        ERC1155
    }
}
