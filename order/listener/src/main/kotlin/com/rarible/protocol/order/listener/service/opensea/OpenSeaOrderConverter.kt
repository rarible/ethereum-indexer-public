package com.rarible.protocol.order.listener.service.opensea

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.opensea.client.model.AssetSchema
import com.rarible.opensea.client.model.OpenSeaOrder
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.service.PriceUpdateService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger
import com.rarible.opensea.client.model.FeeMethod as ClientOpenSeaFeeMethod
import com.rarible.opensea.client.model.HowToCall as ClientOpenSeaHowToCall
import com.rarible.opensea.client.model.OrderSide as ClientOpenSeaOrderSide
import com.rarible.opensea.client.model.SaleKind as ClientOpenSeaSaleKind

@Component
class OpenSeaOrderConverter(
    private val priceUpdateService: PriceUpdateService,
    private val exchangeContracts: OrderIndexerProperties.ExchangeContractAddresses,
    private val featureFlags: OrderIndexerProperties.FeatureFlags
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(clientOpenSeaOrder: OpenSeaOrder): OrderVersion? {
        val (make, take) = createAssets(clientOpenSeaOrder) ?: return null
        val r = clientOpenSeaOrder.r ?: return null
        val s = clientOpenSeaOrder.s ?: return null
        val v = clientOpenSeaOrder.v ?: return null
        val eip712 = clientOpenSeaOrder.exchange == exchangeContracts.openSeaV2
        val prefixedHaha = clientOpenSeaOrder.prefixedHash

        val maker = clientOpenSeaOrder.maker.address
        val taker = clientOpenSeaOrder.taker.address
        val orderData = createData(clientOpenSeaOrder)
        val nonce = calculateNonce(
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
            logger.error("Can't calculate order none for ${calculateNonce()}")
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
                hash = if (eip712) prefixedHaha else Order.hash(it)
            )
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
            if (calculatedHash == expectedHash) return nonce
        }
        return null
    }

    private data class Assets(
        val make: Asset,
        val take: Asset
    )
}
