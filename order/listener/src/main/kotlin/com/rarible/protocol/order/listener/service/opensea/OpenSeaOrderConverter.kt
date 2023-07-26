package com.rarible.protocol.order.listener.service.opensea

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.opensea.client.model.v1.AssetSchema
import com.rarible.opensea.client.model.v1.OpenSeaOrder
import com.rarible.opensea.client.model.v2.Consideration
import com.rarible.opensea.client.model.v2.ItemType
import com.rarible.opensea.client.model.v2.Offer
import com.rarible.opensea.client.model.v2.SeaportItem
import com.rarible.opensea.client.model.v2.SeaportOrder
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OpenSeaOrderFeeMethod
import com.rarible.protocol.order.core.model.OpenSeaOrderHowToCall
import com.rarible.protocol.order.core.model.OpenSeaOrderSaleKind
import com.rarible.protocol.order.core.model.OpenSeaOrderSide
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderBasicSeaportDataV1
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.SeaportConsideration
import com.rarible.protocol.order.core.model.SeaportItemType
import com.rarible.protocol.order.core.model.SeaportItemType.ERC1155
import com.rarible.protocol.order.core.model.SeaportItemType.ERC1155_WITH_CRITERIA
import com.rarible.protocol.order.core.model.SeaportItemType.ERC20
import com.rarible.protocol.order.core.model.SeaportItemType.ERC721
import com.rarible.protocol.order.core.model.SeaportItemType.ERC721_WITH_CRITERIA
import com.rarible.protocol.order.core.model.SeaportItemType.NATIVE
import com.rarible.protocol.order.core.model.SeaportOffer
import com.rarible.protocol.order.core.model.SeaportOrderType
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.protocol.order.listener.misc.seaportInfo
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger
import com.rarible.opensea.client.model.v1.FeeMethod as ClientOpenSeaFeeMethod
import com.rarible.opensea.client.model.v1.HowToCall as ClientOpenSeaHowToCall
import com.rarible.opensea.client.model.v1.OrderSide as ClientOpenSeaOrderSide
import com.rarible.opensea.client.model.v1.SaleKind as ClientOpenSeaSaleKind
import com.rarible.opensea.client.model.v2.OrderType as ClientOrderType
import com.rarible.opensea.client.model.v2.SeaportOrderType as ClientSeaportOrderType

@Component
class OpenSeaOrderConverter(
    private val priceUpdateService: PriceUpdateService,
    private val exchangeContracts: OrderIndexerProperties.ExchangeContractAddresses,
    private val featureFlags: OrderIndexerProperties.FeatureFlags,
    private val metrics: ForeignOrderMetrics,
    properties: OrderListenerProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val openSeaExchangeDomainHashV2 = Word.apply(properties.openSeaExchangeDomainHashV2)
    private val seaportLoadProperties = properties.seaportLoad

    suspend fun convert(clientSeaportOrder: SeaportOrder): OrderVersion? {
        if (clientSeaportOrder.taker != null) return null
        val orderHash = clientSeaportOrder.orderHash
        val maker = clientSeaportOrder.protocolData.parameters.offerer
        val offer = clientSeaportOrder.protocolData.parameters.offer
        val consideration = clientSeaportOrder.protocolData.parameters.consideration
        val protocolAddress = clientSeaportOrder.protocolAddress
        val currentPrice = EthUInt256.of(clientSeaportOrder.currentPrice)
        val salt = clientSeaportOrder.protocolData.parameters.salt
        val startTime = clientSeaportOrder.protocolData.parameters.startTime
        val endTime = clientSeaportOrder.protocolData.parameters.endTime
        val createdAt = clientSeaportOrder.createdAt
        val signature = clientSeaportOrder.protocolData.signature
        val zone = clientSeaportOrder.protocolData.parameters.zone
        val zoneHash = clientSeaportOrder.protocolData.parameters.zoneHash
        val conduitKey = clientSeaportOrder.protocolData.parameters.conduitKey
        val counter = clientSeaportOrder.protocolData.parameters.counter
        val orderType = clientSeaportOrder.protocolData.parameters.orderType

        val (make, take, data) = when (clientSeaportOrder.orderType) {
            ClientSeaportOrderType.BASIC -> {
                if (offer.size != 1) {
                    logger.seaportInfo("Unexpected seaport offer size (${offer.size}), for basic order $clientSeaportOrder")
                    metrics.onDownloadedOrderError(Platform.OPEN_SEA, "offer_size")
                    return null
                }
                if (consideration.isEmpty()) {
                    logger.seaportInfo("Must contains at least one consideration, for basic order $clientSeaportOrder")
                    metrics.onDownloadedOrderError(Platform.OPEN_SEA, "missing_consideration")
                    return null
                }
                val offererConsiderationItemType = consideration.first().itemType

                val make = convertToAsset(offer.single())
                val take = convertToAsset(consideration.filter { it.itemType == offererConsiderationItemType })

                if (take.value != currentPrice) {
                    logger.seaportInfo("Protocol total amount must be equal currentPrice: $clientSeaportOrder")
                    metrics.onDownloadedOrderError(Platform.OPEN_SEA, "invalid_price")
                    return null
                }
                if (make.type.token in seaportLoadProperties.ignoredSellTokens) {
                    logger.seaportInfo("Skip order $orderHash with token ${make.type.token}")
                    metrics.onDownloadedOrderSkipped(Platform.OPEN_SEA, "ignored_sell_token")
                    return null
                }
                val data = OrderBasicSeaportDataV1(
                    protocol = protocolAddress,
                    orderType = convert(orderType),
                    offer = offer.map { convert(it) },
                    consideration = consideration.map { convert(it) },
                    zone = zone,
                    zoneHash = zoneHash,
                    conduitKey = conduitKey,
                    counter = counter.toLong(),
                    counterHex = EthUInt256(counter)
                )
                Triple(make, take, data)
            }
            ClientSeaportOrderType.ENGLISH_AUCTION, ClientSeaportOrderType.DUTCH_AUCTION -> {
                logger.info("Unsupported seaport order type ${clientSeaportOrder.orderType}")
                metrics.onDownloadedOrderSkipped(Platform.OPEN_SEA, "unsupported_type")
                return null
            }
        }
        return OrderVersion(
            hash = orderHash,
            maker = maker,
            taker = null,
            make = make,
            take = take,
            type = OrderType.SEAPORT_V1,
            salt = EthUInt256.of(salt),
            start = startTime.toLong(),
            end = endTime.toLong(),
            data = data,
            createdAt = createdAt,
            signature = signature,
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null,
            platform = Platform.OPEN_SEA
        ).let {
            priceUpdateService.withUpdatedPrices(it)
        }
    }

    suspend fun convert(clientOpenSeaOrder: OpenSeaOrder): OrderVersion? {
        val (make, take) = createAssets(clientOpenSeaOrder) ?: return null
        val r = clientOpenSeaOrder.r ?: return null
        val s = clientOpenSeaOrder.s ?: return null
        val v = clientOpenSeaOrder.v ?: return null
        val eip712 = clientOpenSeaOrder.exchange == exchangeContracts.openSeaV2
        val prefixedHash = clientOpenSeaOrder.prefixedHash

        val maker = clientOpenSeaOrder.maker.address
        val taker = clientOpenSeaOrder.taker.address
        val orderData = createData(clientOpenSeaOrder)
        val nonce = if (eip712) {
            calculateNonce(
                expectedHash = clientOpenSeaOrder.orderHash,
                maker = maker,
                taker = taker,
                paymentToken = clientOpenSeaOrder.paymentToken,
                basePrice = clientOpenSeaOrder.basePrice,
                salt = clientOpenSeaOrder.salt,
                start = clientOpenSeaOrder.listingTime,
                end = clientOpenSeaOrder.expirationTime,
                data = orderData
            ) ?: return run {
                logger.error("Can't calculate order none for ${clientOpenSeaOrder.orderHash}")
                null
            }
        } else {
            null
        }
        return OrderVersion(
            maker = maker,
            taker = if (taker != Address.ZERO()) taker else null,
            make = make,
            take = take,
            type = OrderType.OPEN_SEA_V1,
            salt = EthUInt256.of(clientOpenSeaOrder.salt),
            start = clientOpenSeaOrder.listingTime,
            end = clientOpenSeaOrder.expirationTime,
            data = orderData.copy(nonce = nonce),
            createdAt = clientOpenSeaOrder.createdAt,
            signature = joinSignaturePart(r = r, s = s, v = v),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null,
            platform = Platform.OPEN_SEA
        ).let {
            priceUpdateService.withUpdatedPrices(it).copy(
                // Recalculate OpenSea's specific hash.
                hash = if (eip712) prefixedHash else Order.hash(it)
            )
        }
    }

    fun convertToAsset(seaportItem: SeaportItem): Asset {
        return Asset(convertToAssetType(seaportItem), EthUInt256.of(seaportItem.startAmount))
    }

    fun convertToAssetType(seaportItem: SeaportItem): AssetType {
        return when (seaportItem.itemType) {
            ItemType.NATIVE -> EthAssetType
            ItemType.ERC20 -> Erc20AssetType(seaportItem.token)
            ItemType.ERC721 -> Erc721AssetType(seaportItem.token, EthUInt256.of(seaportItem.identifierOrCriteria))
            ItemType.ERC1155 -> Erc1155AssetType(seaportItem.token, EthUInt256.of(seaportItem.identifierOrCriteria))
            ItemType.ERC721_WITH_CRITERIA,
            ItemType.ERC1155_WITH_CRITERIA -> throw UnsupportedOperationException("Unsupported seaport item type ${seaportItem.itemType}")
        }
    }

    fun convertToAsset(seaportItems: List<SeaportItem>): Asset {
        val assetType = seaportItems.map { convertToAssetType(it) }
        val amount = seaportItems.sumOf { it.startAmount }

        require(assetType.toSet().size == 1) {
            "all seaport items must be the same type"
        }
        return Asset(assetType.first(), EthUInt256.of(amount))
    }

    fun convert(offer: Offer): SeaportOffer {
        return SeaportOffer(
            itemType = convert(offer.itemType),
            token = offer.token,
            identifier = offer.identifierOrCriteria,
            startAmount = offer.startAmount,
            endAmount = offer.endAmount
        )
    }

    fun convert(consideration: Consideration): SeaportConsideration {
        return SeaportConsideration(
            itemType = convert(consideration.itemType),
            token = consideration.token,
            identifier = consideration.identifierOrCriteria,
            startAmount = consideration.startAmount,
            endAmount = consideration.endAmount,
            recipient = consideration.recipient
        )
    }

    fun convert(itemType: ItemType): SeaportItemType {
        return when (itemType) {
            ItemType.NATIVE -> NATIVE
            ItemType.ERC20 -> ERC20
            ItemType.ERC721 -> ERC721
            ItemType.ERC1155 -> ERC1155
            ItemType.ERC721_WITH_CRITERIA -> ERC721_WITH_CRITERIA
            ItemType.ERC1155_WITH_CRITERIA -> ERC1155_WITH_CRITERIA
        }
    }

    fun convert(orderType: ClientOrderType): SeaportOrderType {
        return when (orderType) {
            ClientOrderType.FULL_OPEN -> SeaportOrderType.FULL_OPEN
            ClientOrderType.PARTIAL_OPEN -> SeaportOrderType.PARTIAL_OPEN
            ClientOrderType.FULL_RESTRICTED -> SeaportOrderType.FULL_RESTRICTED
            ClientOrderType.PARTIAL_RESTRICTED -> SeaportOrderType.PARTIAL_RESTRICTED
            ClientOrderType.CONTRACT -> SeaportOrderType.CONTRACT
        }
    }

    private fun joinSignaturePart(r: Word, s: Word, v: Byte): Binary {
        return r.add(s).add(byteArrayOf(v))
    }

    private fun createData(clientOpenSeaOrder: OpenSeaOrder): OrderOpenSeaV1DataV1 {
        return OrderOpenSeaV1DataV1(
            exchange = clientOpenSeaOrder.exchange,
            makerRelayerFee = clientOpenSeaOrder.makerRelayerFee,
            takerRelayerFee = clientOpenSeaOrder.takerRelayerFee,
            makerProtocolFee = clientOpenSeaOrder.makerProtocolFee,
            takerProtocolFee = clientOpenSeaOrder.takerProtocolFee,
            feeRecipient = clientOpenSeaOrder.feeRecipient.address,
            feeMethod = convert(clientOpenSeaOrder.feeMethod),
            side = convert(clientOpenSeaOrder.side),
            saleKind = convert(clientOpenSeaOrder.saleKind),
            howToCall = convert(clientOpenSeaOrder.howToCall),
            callData = clientOpenSeaOrder.callData,
            replacementPattern = clientOpenSeaOrder.replacementPattern,
            staticTarget = clientOpenSeaOrder.staticTarget,
            staticExtraData = clientOpenSeaOrder.staticExtraData,
            extra = clientOpenSeaOrder.extra,
            target = clientOpenSeaOrder.target,
            nonce = null,
        )
    }

    private fun convert(source: ClientOpenSeaHowToCall): OpenSeaOrderHowToCall {
        return when (source) {
            ClientOpenSeaHowToCall.CALL -> OpenSeaOrderHowToCall.CALL
            ClientOpenSeaHowToCall.DELEGATE_CALL -> OpenSeaOrderHowToCall.DELEGATE_CALL
        }
    }

    private fun convert(source: ClientOpenSeaSaleKind): OpenSeaOrderSaleKind {
        return when (source) {
            ClientOpenSeaSaleKind.FIXED_PRICE -> OpenSeaOrderSaleKind.FIXED_PRICE
            ClientOpenSeaSaleKind.DUTCH_AUCTION -> OpenSeaOrderSaleKind.DUTCH_AUCTION
        }
    }

    private fun convert(source: ClientOpenSeaFeeMethod): OpenSeaOrderFeeMethod {
        return when (source) {
            ClientOpenSeaFeeMethod.PROTOCOL_FEE -> OpenSeaOrderFeeMethod.PROTOCOL_FEE
            ClientOpenSeaFeeMethod.SPLIT_FEE -> OpenSeaOrderFeeMethod.SPLIT_FEE
        }
    }

    private fun convert(source: ClientOpenSeaOrderSide): OpenSeaOrderSide {
        return when (source) {
            ClientOpenSeaOrderSide.SELL -> OpenSeaOrderSide.SELL
            ClientOpenSeaOrderSide.BUY -> OpenSeaOrderSide.BUY
        }
    }

    private fun createAssets(openSeaOrder: OpenSeaOrder): Assets? {
        val asset = openSeaOrder.asset ?: return null

        val nftAsset = Asset(
            type = when (asset.assetContract.schemaName) {
                AssetSchema.ERC721 -> Erc721AssetType(
                    token = asset.assetContract.address,
                    tokenId = EthUInt256.of(asset.tokenId)
                )
                AssetSchema.ERC1155 -> Erc1155AssetType(
                    token = asset.assetContract.address,
                    tokenId = EthUInt256.of(asset.tokenId)
                )
                AssetSchema.ERC20 -> {
                    logger.info("Unsupported OpenSea order: $openSeaOrder")
                    return null
                }
                else -> {
                    logger.info("Can't detect asset type: $openSeaOrder")
                    return null
                }
            },
            value = EthUInt256.of(openSeaOrder.quantity)
        )
        val paymentAsset = Asset(
            type = when {
                openSeaOrder.paymentToken != Address.ZERO() -> Erc20AssetType(
                    token = openSeaOrder.paymentToken
                )
                else -> EthAssetType
            },
            value = EthUInt256.of(openSeaOrder.basePrice)
        )
        return when (openSeaOrder.side) {
            ClientOpenSeaOrderSide.SELL -> Assets(nftAsset, paymentAsset)
            ClientOpenSeaOrderSide.BUY -> Assets(paymentAsset, nftAsset)
        }
    }

    private fun calculateNonce(
        expectedHash: Word,
        maker: Address,
        taker: Address?,
        paymentToken: Address,
        basePrice: BigInteger,
        salt: BigInteger,
        start: Long?,
        end: Long?,
        data: OrderOpenSeaV1DataV1
    ): Long? {
        (0L..featureFlags.maxOpenSeaNonceCalculation).forEach { nonce ->
            val calculatedHash = Order.openSeaV1EIP712Hash(
                maker = maker,
                taker = taker,
                paymentToken = paymentToken,
                basePrice = basePrice,
                salt = salt,
                start = start,
                end = end,
                data = data.copy(nonce = nonce)
            )
            if (calculatedHash == expectedHash ||
                Order.openSeaV1EIP712HashToSign(
                    hash = calculatedHash,
                    domain = openSeaExchangeDomainHashV2
                ) == expectedHash
            ) {
                logger.info("Calculated nonce $nonce for $expectedHash")
                return nonce
            }
        }
        metrics.onDownloadedOrderError(Platform.OPEN_SEA, "hash_mismatch")
        return null
    }

    private data class Assets(
        val make: Asset,
        val take: Asset
    )
}
