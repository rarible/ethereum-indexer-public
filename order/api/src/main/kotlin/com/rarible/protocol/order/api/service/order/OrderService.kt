package com.rarible.protocol.order.api.service.order

import com.rarible.core.common.nowMillis
import com.rarible.core.common.optimisticLock
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.api.exceptions.IncorrectOrderDataException
import com.rarible.protocol.order.api.exceptions.LazyItemNotFoundException
import com.rarible.protocol.order.api.exceptions.OrderNotFoundException
import com.rarible.protocol.order.api.misc.data
import com.rarible.protocol.order.api.service.nft.AssetMakeBalanceProvider
import com.rarible.protocol.order.api.service.order.OrderFilterCriteria.toCriteria
import com.rarible.protocol.order.api.service.order.validation.OrderValidator
import com.rarible.protocol.order.core.converters.model.AssetConverter
import com.rarible.protocol.order.core.converters.model.OrderDataConverter
import com.rarible.protocol.order.core.converters.model.OrderTypeConverter
import com.rarible.protocol.order.core.event.OrderVersionListener
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.provider.ProtocolCommissionProvider
import com.rarible.protocol.order.core.repository.order.OrderFilter
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.nft.NftItemApiService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger
import java.util.*

@Component
class OrderService(
    private val validator: OrderValidator,
    private val orderRepository: OrderRepository,
    private val orderVersionRepository: OrderVersionRepository,
    private val assetMakeBalanceProvider: AssetMakeBalanceProvider,
    private val protocolCommissionProvider: ProtocolCommissionProvider,
    private val priceUpdateService: PriceUpdateService,
    private val nftItemApiService: NftItemApiService,
    private val orderVersionListener: OrderVersionListener,
    private val priceNormalizer: PriceNormalizer
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convertForm(form: OrderFormDto): Order {
        val maker = form.maker
        val make = checkLazyNft(AssetConverter.convert(form.make))
        val take = checkLazyNft(AssetConverter.convert(form.take))
        val data = OrderDataConverter.convert(form.data)
        val makeBalance = assetMakeBalanceProvider.getAssetStock(maker, make.type)
        val protocolCommission = protocolCommissionProvider.get()
        val feeSide = Order.getFeeSide(make.type, take.type)
        val makeStock = Order.calculateMakeStock(
            make.value,
            take.value,
            EthUInt256.ZERO,
            data,
            makeBalance,
            protocolCommission,
            feeSide,
            false
        )

        return Order(
            maker = maker,
            make = make,
            take = take,
            taker = form.taker,
            type = OrderTypeConverter.convert(form),
            salt = EthUInt256.of(form.salt),
            start = form.start,
            end = form.end,
            data = data,
            makeStock = makeStock,
            cancelled = false,
            fill = EthUInt256.ZERO,
            createdAt = nowMillis(),
            lastUpdateAt = nowMillis(),
            signature = form.signature
        ).also {
            logger.info("OrderForm (hash=${it.hash}, makeBalance=$makeBalance, makeStock=$makeStock) was converted")
        }
    }

    suspend fun put(form: OrderFormDto): Order {
        val template = convertForm(form)
        validator.validateOrder(template)

        val saved = optimisticLock {
            val existing = orderRepository.findById(template.hash)

            val order = if (existing != null) {
                validator.validate(existing, template)

                existing.withNewValues(
                    make = template.make.value,
                    take = template.take.value,
                    makeStock = template.makeStock,
                    signature = template.signature,
                    updateAt = nowMillis()
                )
            } else {
                template
            }
            val orderUsdValue = priceUpdateService.getAssetsUsdValue(order.make, order.take, nowMillis())
            val updated = if (orderUsdValue != null) order.withOrderUsdValue(orderUsdValue) else order
            val updatedWithPriceHistory = addPriceHistoryRecord(existing, updated)
            save(updatedWithPriceHistory)
        }
        val orderVersion =  orderVersionRepository.save(OrderVersion(
            hash = saved.hash,
            make = saved.make,
            maker = saved.maker,
            take = saved.take,
            taker = saved.taker,
            makePriceUsd = saved.makePriceUsd,
            takePriceUsd = saved.takePriceUsd,
            takeUsd = saved.takeUsd,
            makeUsd = saved.makeUsd,
            platform = Platform.RARIBLE
        )).awaitFirst()

        orderVersionListener.onOrderVersion(orderVersion)

        return saved
    }

    suspend fun save(order: Order): Order {
        logger.info("Order ${order.hash} was executed")
        return orderRepository.save(order)
    }

    suspend fun get(hash: Word): Order {
        return orderRepository.findById(hash)
            ?: throw OrderNotFoundException(hash)
    }

    suspend fun updateMakeStock(hash: Word): Order {
        val order = get(hash)
        val mackBalance = assetMakeBalanceProvider.getAssetStock(order.maker, order.make.type)
        val protocolCommission = protocolCommissionProvider.get()

        val updatedOrder = order.withMakeBalance(mackBalance, protocolCommission)
        logger.info("Updated order ${updatedOrder.hash}, makeStock=${updatedOrder.makeStock}, makeBalance=$mackBalance")

        return save(updatedOrder)
    }

    suspend fun findOrders(filter: OrderFilter?, legacyFilter: OrderFilterDto, size: Int, continuation: String?): List<Order> {
        return if (filter != null) orderRepository.search(filter) else orderRepository.search(legacyFilter.toCriteria(continuation, size))
    }

    suspend fun findOrders(legacyFilter: OrderFilterDto, size: Int, continuation: String?): List<Order> {
        return findOrders(null, legacyFilter, size, continuation)
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
                    is LazyErc1155Dto -> throw IncorrectOrderDataException("lazy nft is of type ERC1155, not ERC721")
                    else -> assetType
                }
            }
            is Erc1155AssetType -> {
                when (val lazy = getLazyNft(assetType.token, assetType.tokenId)) {
                    is LazyErc721Dto -> throw IncorrectOrderDataException("lazy nft is of type ERC721, not ERC1155")
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
        val itemId = "${token}:${tokenId}"
        val lazySupply = nftItemApiService.getNftItemById(itemId)?.lazySupply ?: BigInteger.ZERO

        return if (lazySupply > BigInteger.ZERO) {
            nftItemApiService.getNftLazyItemById(itemId) ?: throw LazyItemNotFoundException(itemId)
        } else {
            return null
        }
    }

    private suspend fun addPriceHistoryRecord(current: Order?, updated: Order): Order {
        if (current != null
            && current.make == updated.make
            && current.take == updated.take
        ) {
            return updated
        }

        val record = OrderPriceHistoryRecord(
            date = nowMillis(),
            makeValue = priceNormalizer.normalize(updated.make),
            takeValue = priceNormalizer.normalize(updated.take)
        )

        val priceHistory = updated.priceHistory.toCollection(LinkedList())
        priceHistory.addFirst(record)
        if (priceHistory.size > 20) {
            priceHistory.removeLast()
        }
        return updated.copy(priceHistory = priceHistory.toList())
    }
}

private fun List<PartDto>.toPartList() =
    map { Part(it.account, EthUInt256.of(it.value.toLong())) }

