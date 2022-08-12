package com.rarible.protocol.order.listener.service.opensea

import com.rarible.contracts.erc1155.IERC1155
import com.rarible.contracts.erc721.IERC721
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.common.opensea.merkle.MerkleValidator
import com.rarible.protocol.contracts.exchange.wyvern.OrderCancelledEvent
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OpenSeaMatchedOrders
import com.rarible.protocol.order.core.model.OpenSeaOrderSide
import com.rarible.protocol.order.core.model.OpenSeaTransactionOrder
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.Transfer
import com.rarible.protocol.order.core.service.CallDataEncoder
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Bytes
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

@Component
class OpenSeaOrderEventConverter(
    private val priceUpdateService: PriceUpdateService,
    private val prizeNormalizer: PriceNormalizer,
    private val callDataEncoder: CallDataEncoder
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun convert(
        openSeaOrders: OpenSeaMatchedOrders,
        from: Address,
        price: BigInteger,
        date: Instant,
        input: Bytes,
    ): List<OrderSideMatch> {
        val externalOrderExecutedOnRarible = openSeaOrders.origin == Platform.RARIBLE.id
        val origin = openSeaOrders.origin
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

        val transfer = encodeTransfer(Binary.apply(sellCallData)) ?: return run {
            logger.warn("Can't parse transfer for orders $openSeaOrders")
            emptyList()
        }
        val nftAsset = createNftAsset(sellOrder.target, transfer)
        val paymentAsset = createPaymentAsset(price, buyOrder.paymentToken)

        val buyUsdValue = priceUpdateService.getAssetsUsdValue(make = paymentAsset, take = nftAsset, at = date)
        val sellUsdValue = priceUpdateService.getAssetsUsdValue(make = nftAsset, take = paymentAsset, at = date)

        var buyAdhoc = buyOrder.maker == from
        var sellAdhoc = sellOrder.maker == from

        if (buyAdhoc && sellAdhoc) {
            buyAdhoc = EthUInt256.of(buyOrder.salt) == EthUInt256.ZERO
            sellAdhoc = EthUInt256.of(sellOrder.salt) == EthUInt256.ZERO
        }

        val events = listOf(
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
                origin = origin,
                externalOrderExecutedOnRarible = externalOrderExecutedOnRarible,
                date = date,
                adhoc = buyAdhoc,
                counterAdhoc = sellAdhoc,
                originFees = buyOrder.originFees,
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
                origin = origin,
                externalOrderExecutedOnRarible = externalOrderExecutedOnRarible,
                date = date,
                source = HistorySource.OPEN_SEA,
                adhoc = sellAdhoc,
                counterAdhoc = buyAdhoc,
                originFees = sellOrder.originFees,
            )
        )
        return OrderSideMatch.addMarketplaceMarker(events, input)
    }

    suspend fun convert(order: OpenSeaTransactionOrder, date: Instant, event: OrderCancelledEvent, eip712: Boolean): List<OrderCancel> {
        val transfer = encodeTransfer(order.callData) ?: return emptyList()
        val nftAsset = createNftAsset(order.target, transfer)
        val paymentAsset = createPaymentAsset(order.basePrice, order.paymentToken)
        val (make, take) = when (order.side) {
            OpenSeaOrderSide.SELL -> nftAsset to paymentAsset
            OpenSeaOrderSide.BUY -> paymentAsset to nftAsset
        }
        return listOf(
            OrderCancel(
                hash = if (eip712) Word.apply(event.hash()) else order.hash,
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

    private fun createNftAsset(target: Address, transfer: Transfer): Asset {
        return when (transfer) {
            is Transfer.Erc721Transfer -> {
                Asset(
                    type = Erc721AssetType(
                        token = target,
                        tokenId = EthUInt256.of(transfer.tokenId)
                    ),
                    value = EthUInt256.ONE
                )
            }
            is Transfer.Erc1155Transfer -> {
                Asset(
                    Erc1155AssetType(
                        token = target,
                        tokenId = EthUInt256.of(transfer.tokenId)
                    ),
                    value = EthUInt256.of(transfer.value)
                )
            }
            is Transfer.MerkleValidatorErc721Transfer-> {
                Asset(
                    type = Erc721AssetType(
                        token = transfer.token,
                        tokenId = EthUInt256.of(transfer.tokenId)
                    ),
                    value = EthUInt256.ONE
                )
            }
            is Transfer.MerkleValidatorErc1155Transfer -> {
                Asset(
                    Erc1155AssetType(
                        token = transfer.token,
                        tokenId = EthUInt256.of(transfer.tokenId)
                    ),
                    value = EthUInt256.of(transfer.value)
                )
            }
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

    fun encodeTransfer(callData: Binary): Transfer? {
        return when (callData.methodSignatureId()) {
            IERC1155.safeTransferFromSignature().id(),
            IERC721.transferFromSignature().id(),
            IERC721.safeTransferFromSignature(),
            MerkleValidator.matchERC721UsingCriteriaSignature().id(),
            MerkleValidator.matchERC721WithSafeTransferUsingCriteriaSignature().id(),
            MerkleValidator.matchERC1155UsingCriteriaSignature().id() -> {
                callDataEncoder.decodeTransfer(callData)
            }
            else -> {
                logger.info("OpenSea order call data was ignored: $callData")
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
}