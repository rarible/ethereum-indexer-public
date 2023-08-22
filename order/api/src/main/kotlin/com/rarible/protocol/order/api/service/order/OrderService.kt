package com.rarible.protocol.order.api.service.order

import com.rarible.core.common.nowMillis
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.LazyErc1155Dto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.OrderFormDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.order.api.misc.data
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.converters.model.AssetConverter
import com.rarible.protocol.order.core.converters.model.OrderDataConverter
import com.rarible.protocol.order.core.converters.model.OrderTypeConverter
import com.rarible.protocol.order.core.exception.EntityNotFoundApiException
import com.rarible.protocol.order.core.exception.OrderDataException
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc1155LazyAssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.Erc721LazyAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderAmmData
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.PoolNftItemIds
import com.rarible.protocol.order.core.model.PoolTradePrice
import com.rarible.protocol.order.core.model.currency
import com.rarible.protocol.order.core.model.order.OrderFilter
import com.rarible.protocol.order.core.model.toOrderExactFields
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.CommonSigner
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.approve.ApproveService
import com.rarible.protocol.order.core.service.curve.PoolCurve
import com.rarible.protocol.order.core.service.nft.NftItemApiService
import com.rarible.protocol.order.core.service.pool.PoolInfoProvider
import com.rarible.protocol.order.core.service.pool.PoolOwnershipService
import com.rarible.protocol.order.core.validator.OrderValidator
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger

@Component
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val nftItemApiService: NftItemApiService,
    private val poolOwnershipService: PoolOwnershipService,
    private val priceUpdateService: PriceUpdateService,
    private val raribleOrderSaveMetric: RegisteredCounter,
    private val poolCurve: PoolCurve,
    private val poolInfoProvider: PoolInfoProvider,
    private val approveService: ApproveService,
    private val commonSigner: CommonSigner,
    private val apiOrderValidator: OrderValidator,
    private val coreOrderValidator: OrderValidator,
    private val featureFlags: OrderIndexerProperties.FeatureFlags,
) {
    suspend fun convertFormToVersion(form: OrderFormDto): OrderVersion {
        val maker = form.maker
        val make = checkLazyNftMake(maker, AssetConverter.convert(form.make))
        val take = checkLazyNft(AssetConverter.convert(form.take))
        val data = OrderDataConverter.convert(form.data)
        val hash = Order.hashKey(form.maker, make.type, take.type, form.salt, data)
        val platform = Platform.RARIBLE
        val approved = approveService.checkOnChainApprove(maker, make.type, platform)
        val signature = commonSigner.fixSignature(form.signature)
        return OrderVersion(
            maker = maker,
            make = make,
            take = take,
            taker = form.taker,
            type = OrderTypeConverter.convert(form),
            salt = EthUInt256.of(form.salt),
            start = form.start,
            end = form.end,
            data = data,
            signature = signature,
            platform = platform,
            hash = hash,
            approved = approved,
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        ).run { priceUpdateService.withUpdatedAllPrices(this) }
    }

    suspend fun put(form: OrderFormDto): Order {
        val eventTimeMarks = orderOffchainEventMarks()
        val orderVersion = convertFormToVersion(form)
        apiOrderValidator.validate(orderVersion.toOrderExactFields())
        return orderUpdateService
            .save(orderVersion, eventTimeMarks)
            .also { raribleOrderSaveMetric.increment() }
    }

    suspend fun get(hash: Word): Order {
        return orderRepository.findById(hash)
            ?: throw EntityNotFoundApiException("Order", hash)
    }

    suspend fun validateAndGet(hash: Word): Order {
        val order = orderRepository.findById(hash)
            ?: throw EntityNotFoundApiException("Order", hash)

        if (!featureFlags.enableOrderValidation) {
            return order
        }

        coreOrderValidator.validate(order)
        return order
    }

    fun getAll(hashes: List<Word>): Flow<Order> {
        return orderRepository.findAll(hashes)
    }

    suspend fun updateMakeStock(hash: Word): Order =
        orderUpdateService.updateMakeStock(hash, null, orderOffchainEventMarks())
            ?: throw EntityNotFoundApiException("Order", hash)

    suspend fun findOrders(legacyFilter: OrderFilter, size: Int, continuation: String?): List<Order> {
        return orderRepository.search(legacyFilter.toQuery(continuation, size))
    }

    suspend fun getAmmOrderHoldItemIds(hash: Word, continuation: String?, size: Int): PoolNftItemIds {
        val (order, data) = getAmmOrder(hash)
        val pollAddress = data.poolAddress
        val collection = when {
            order.make.type.nft -> order.make.type.token
            order.take.type.nft -> order.take.type.token
            else -> throw OrderDataException("AMM order $hash has not nft asset")
        }
        return poolOwnershipService.getPoolItemIds(pollAddress, collection, continuation, size)
    }

    suspend fun getAmmBuyInfo(hash: Word, nftCount: Int): List<PoolTradePrice> {
        val (order, _) = getAmmOrder(hash)
        val info = poolInfoProvider.getPollInfo(order) ?: error("Unexpectedly can't get pool info from $hash")
        val inputValues = poolCurve.getBuyInputValues(
            curve = info.curve,
            spotPrice = info.protocolFee,
            delta = info.delta,
            numItems = nftCount,
            feeMultiplier = info.fee,
            protocolFeeMultiplier = info.protocolFee
        )
        return coroutineScope {
            inputValues.map { inputValue ->
                val value = inputValue.value
                val currency = order.currency
                async {
                    PoolTradePrice(
                        price = value,
                        priceValue = priceUpdateService.getAssetValue(currency, value),
                        priceUsd = priceUpdateService.getAssetUsdValue(currency, value, nowMillis()),
                    )
                }
            }.awaitAll()
        }
    }

    suspend fun getAmmOrdersByItemId(
        contract: Address,
        tokenId: EthUInt256,
        continuation: String?,
        size: Int
    ): List<Order> {
        val result = poolOwnershipService.getPoolHashesByItemId(contract, tokenId)
        return orderRepository.findAll(result).toList()
    }

    private suspend fun getAmmOrder(hash: Word): Pair<Order, OrderAmmData> {
        val order = orderRepository.findById(hash) ?: throw EntityNotFoundApiException("Order", hash)
        if (order.type != OrderType.AMM) throw OrderDataException("Order $hash type is not AMM")
        if (order.data !is OrderAmmData) throw OrderDataException("Order $hash data is no AMM")
        return order to order.data as OrderAmmData
    }

    private suspend fun checkLazyNftMake(maker: Address, asset: Asset): Asset {
        val make = checkLazyNft(asset)
        val makeType = make.type
        if (makeType is Erc1155LazyAssetType && makeType.creators.first().account == maker) {
            return make
        }
        if (makeType is Erc721LazyAssetType && makeType.creators.first().account == maker) {
            return make
        }
        return asset
    }

    private suspend fun checkLazyNft(asset: Asset): Asset {
        return Asset(checkLazyNft(asset.type), asset.value)
    }

    private suspend fun checkLazyNft(assetType: AssetType): AssetType {
        return when (assetType) {
            is Erc721AssetType -> {
                when (val lazy = getLazyNft(assetType.token, assetType.tokenId)) {
                    is LazyErc721Dto -> {
                        Erc721LazyAssetType(
                            lazy.contract,
                            EthUInt256(lazy.tokenId),
                            lazy.uri,
                            lazy.creators.toPartList(),
                            lazy.royalties.toPartList(),
                            lazy.signatures
                        )
                    }
                    is LazyErc1155Dto -> throw OrderDataException("lazy nft is of type ERC1155, not ERC721")
                    else -> assetType
                }
            }
            is Erc1155AssetType -> {
                when (val lazy = getLazyNft(assetType.token, assetType.tokenId)) {
                    is LazyErc721Dto -> throw OrderDataException("lazy nft is of type ERC721, not ERC1155")
                    is LazyErc1155Dto -> {
                        Erc1155LazyAssetType(
                            lazy.contract,
                            EthUInt256(lazy.tokenId),
                            lazy.uri,
                            EthUInt256(lazy.supply),
                            lazy.creators.toPartList(),
                            lazy.royalties.toPartList(),
                            lazy.signatures
                        )
                    }
                    else -> assetType
                }
            }
            else -> assetType
        }
    }

    private suspend fun getLazyNft(token: Address, tokenId: EthUInt256): LazyNftDto? {
        val itemId = "$token:$tokenId"
        val lazySupply = nftItemApiService.getNftItemById(itemId)?.lazySupply ?: BigInteger.ZERO

        return if (lazySupply > BigInteger.ZERO) {
            nftItemApiService.getNftLazyItemById(itemId) ?: throw EntityNotFoundApiException("Lazy Item", itemId)
        } else {
            return null
        }
    }
}

private fun List<PartDto>.toPartList() =
    map { Part(it.account, EthUInt256.of(it.value.toLong())) }
