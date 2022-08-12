package com.rarible.protocol.order.listener.service.zero.ex

import com.rarible.ethereum.common.keccak256
import com.rarible.ethereum.common.toHexString
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.ZeroExMatchOrdersData
import com.rarible.protocol.order.core.model.ZeroExOrder
import com.rarible.protocol.order.core.model.order.logger
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Bytes
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant

@Component
class ZeroExOrderEventConverter(
    private val priceUpdateService: PriceUpdateService,
    private val priceNormalizer: PriceNormalizer,
    private val properties: OrderListenerProperties
) {
    suspend fun convert(
        matchOrdersData: ZeroExMatchOrdersData,
        from: Address,
        date: Instant,
        orderHash: Word,
        makerAddress: Address,
        makerAssetFilledAmount: BigInteger,
        takerAssetFilledAmount: BigInteger,
        input: Bytes,
    ): List<OrderSideMatch> {
        // filling orders
        val orders = listOfNotNull(matchOrdersData.leftOrder, matchOrdersData.rightOrder)
        // event order
        val order = orders.first { it.makerAddress == makerAddress }
        val secondOrder = (orders - order).firstOrNull()

        require(secondOrder == null || order.makerAssetData == secondOrder.takerAssetData) {
            "make and take assets must be equal"
        }
        require(secondOrder == null || order.takerAssetData == secondOrder.makerAssetData) {
            "take and make assets must be equal"
        }
        val makeAsset = createAsset(assetData = order.makerAssetData, amount = makerAssetFilledAmount) ?: return emptyList()
        val takeAsset = createAsset(assetData = order.takerAssetData, amount = takerAssetFilledAmount) ?: return emptyList()

        val leftOrderSide = getOrderSide(makeAsset, secondOrder)

        val usdValue = priceUpdateService.getAssetsUsdValue(make = makeAsset, take = takeAsset, at = date)

        var adhoc = order.makerAddress == from
        var counterAdhoc = secondOrder?.makerAddress == from
        if (adhoc && counterAdhoc) {
            adhoc = EthUInt256.of(order.salt) == EthUInt256.ZERO
            counterAdhoc = EthUInt256.of(secondOrder!!.salt) == EthUInt256.ZERO
        }

        val secondOrderHash = secondOrder?.orderHash()
        val events = listOf(
            OrderSideMatch(
                hash = orderHash,
                counterHash = secondOrderHash,
                side = leftOrderSide,
                make = makeAsset,
                take = takeAsset,
                fill = EthUInt256(takerAssetFilledAmount),
                maker = order.makerAddress,
                taker = matchOrdersData.takerAddress ?: secondOrder?.makerAddress ?: from,
                makeUsd = usdValue?.makeUsd,
                takeUsd = usdValue?.takeUsd,
                makeValue = priceNormalizer.normalize(makeAsset),
                takeValue = priceNormalizer.normalize(takeAsset),
                makePriceUsd = usdValue?.makePriceUsd,
                takePriceUsd = usdValue?.takePriceUsd,
                source = HistorySource.OPEN_SEA,
                externalOrderExecutedOnRarible = false,
                date = date,
                adhoc = adhoc,
                counterAdhoc = counterAdhoc,
            )
        )
        return OrderSideMatch.addMarketplaceMarker(events, input)
    }

    private fun createAsset(
        assetData: Binary,
        amount: BigInteger,
    ): Asset? {
        val type = assetData.slice(0, 4).toString()
        val token = Address.apply(assetData.slice(16, 36))
        val value = EthUInt256.of(amount)
        return when (type) {
            ERC20_ASSET_TYPE -> {
                Asset(
                    type = Erc20AssetType(token = token),
                    value = value
                )
            }
            ERC721_ASSET_TYPE -> {
                val tokenId = assetData.slice(36, 68).toBigInteger()
                Asset(
                    type = Erc721AssetType(
                        token = token,
                        tokenId = EthUInt256.of(tokenId)
                    ),
                    value = value
                )
            }
            ERC1155_ASSET_TYPE -> {
                val tokenId = assetData.slice(164, 196).toBigInteger()
                Asset(
                    type = Erc1155AssetType(
                        token = token,
                        tokenId = EthUInt256.of(tokenId)
                    ),
                    value = value
                )
            }
            BUNDLE_ASSET_TYPE -> {
                logger.warn("Unsupported asset type $type of zero ex asset, skip it")
                null
            }
            else -> {
                throw IllegalStateException("Unknown asset type $type of zero ex asset")
            }
        }
    }

    private fun getOrderSide(makeAsset: Asset, secondOrder: ZeroExOrder?): OrderSide =
    // when we have 2 orders we can't determine which order was created earlier,
    // because data is the same for buying by sell order and selling by bid order
        // thus we always consider sell order as OrderSide.LEFT type
        if (secondOrder == null || makeAsset.type.nft) {
            // secondOrder == null - there was only one order
            // makeAsset.type.nft - sell order
            OrderSide.LEFT
        } else {
            // bid order
            OrderSide.RIGHT
        }

    /**
     * According to LibEIP712.hashEIP712Message(bytes32 eip712DomainHash, bytes32 hashStruct)
     * and LibOrder.getTypedDataHash(Order memory order, bytes32 eip712ExchangeDomainHash)
     *
     * exchange domain hash could be calculated as
    POST https://polygon-rpc.com/
    Content-Type: application/json

    {
    "jsonrpc": "2.0",
    "id": 1,
    "method": "eth_call",
    "params": [
    {
    "from": "0x0000000000000000000000000000000000000000",
    "data": "0xc26cfecd",
    "to": "0xfede379e48c873c75f3cc0c81f7c784ad730a8f7"
    },
    "latest"
    ]
    }
     * where
     * 0xfede379e48c873c75f3cc0c81f7c784ad730a8f7 - zero ex exchange contract address
     * 0xc26cfecd - read-method for reading constant variable EIP712_EXCHANGE_DOMAIN_HASH of that contract
     */
    private fun ZeroExOrder.orderHash(): Word {
        val orderStructHash = orderStructHash(this)
        return keccak256(
            // EIP191 header
            Binary.apply("1901")
                .add(Binary.apply(properties.zeroExExchangeDomainHash))
                .add(orderStructHash)
        )
    }

    /**
     * According to LibOrder.getStructHash(Order memory order)
     */
    private fun orderStructHash(order: ZeroExOrder): Word = with(order) {
        keccak256(
            Binary.apply(EIP712_ORDER_SCHEMA_HASH)
                .addAsBytes32(makerAddress)
                .addAsBytes32(takerAddress)
                .addAsBytes32(feeRecipientAddress)
                .addAsBytes32(senderAddress)
                .addAsBytes32(makerAssetAmount)
                .addAsBytes32(takerAssetAmount)
                .addAsBytes32(makerFee)
                .addAsBytes32(takerFee)
                .addAsBytes32(expirationTimeSeconds)
                .addAsBytes32(salt)
                .add(keccak256(makerAssetData))
                .add(keccak256(takerAssetData))
                .add(keccak256(makerFeeAssetData))
                .add(keccak256(takerFeeAssetData))
        )
    }

    private fun Binary.addAsBytes32(address: Address): Binary =
        this.add(Binary.apply("000000000000000000000000")).add(address)

    private fun Binary.addAsBytes32(value: BigInteger): Binary =
        this.add(Binary.apply(value.toHexString().substring(2).padStart(64, '0')))

    companion object {
        private const val EIP712_ORDER_SCHEMA_HASH =
            "0xf80322eb8376aafb64eadf8f0d7623f22130fd9491a221e902b713cb984a7534"
        private const val ERC20_ASSET_TYPE = "0xf47261b0"
        private const val ERC721_ASSET_TYPE = "0x02571792"
        private const val ERC1155_ASSET_TYPE = "0xa7cb5fb7"
        private const val BUNDLE_ASSET_TYPE = "0x94cfcdd7"
    }
}
