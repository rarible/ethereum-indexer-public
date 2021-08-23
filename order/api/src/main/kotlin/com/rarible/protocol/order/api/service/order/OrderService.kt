package com.rarible.protocol.order.api.service.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.api.exceptions.*
import com.rarible.protocol.order.api.misc.data
import com.rarible.protocol.order.api.service.order.OrderFilterCriteria.toCriteria
import com.rarible.protocol.order.core.converters.model.AssetConverter
import com.rarible.protocol.order.core.converters.model.OrderDataConverter
import com.rarible.protocol.order.core.converters.model.OrderTypeConverter
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import com.rarible.protocol.order.core.service.nft.NftItemApiService
import com.rarible.protocol.order.core.service.validation.LazyAssetValidator
import com.rarible.protocol.order.core.service.validation.OrderSignatureValidator
import com.rarible.protocol.order.core.service.validation.OrderValidator
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger

@Component
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderReduceService: OrderReduceService,
    private val nftItemApiService: NftItemApiService
) {

    suspend fun convertForm(form: OrderFormDto): Order = convertFormToVersion(form).toOrderExactFields()

    suspend fun convertFormToVersion(form: OrderFormDto): OrderVersion {
        val maker = form.maker
        val make = checkLazyNft(AssetConverter.convert(form.make))
        val take = checkLazyNft(AssetConverter.convert(form.take))
        val hash = Order.hashKey(form.maker, make.type, take.type, form.salt)
        val data = OrderDataConverter.convert(form.data)
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
            makeStock = EthUInt256.ZERO,
            signature = form.signature,
            platform = Platform.RARIBLE,
            hash = hash,
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null
        )
    }

    suspend fun put(form: OrderFormDto): Order {
        val orderVersion = convertFormToVersion(form)
        return try {
            orderReduceService.addOrderVersion(orderVersion)
        } catch (e: Exception) {
            throw e.toApiException()
        }
    }

    suspend fun get(hash: Word): Order {
        return orderRepository.findById(hash)
            ?: throw OrderNotFoundException(hash)
    }

    suspend fun updateMakeStock(hash: Word): Order = try {
        orderReduceService.updateOrderMakeStock(hash)
    } catch (e: Exception) {
        throw e.toApiException()
    }

    suspend fun findOrders(filter: OrderFilterDto, size: Int, continuation: String? = null): List<Order> {
        return orderRepository.search(filter.toCriteria(continuation, size))
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

    private fun Exception.toApiException(): Exception = when (this) {
        is OrderReduceService.OrderUpdateError -> OrderUpdateError(
            when (this.reason) {
                OrderReduceService.OrderUpdateError.OrderUpdateErrorReason.CANCELLED -> OrderUpdateErrorReason.CANCELLED
                OrderReduceService.OrderUpdateError.OrderUpdateErrorReason.INVALID_UPDATE -> OrderUpdateErrorReason.INVALID_UPDATE
                OrderReduceService.OrderUpdateError.OrderUpdateErrorReason.MAKE_VALUE_ERROR -> OrderUpdateErrorReason.MAKE_VALUE_ERROR
                OrderReduceService.OrderUpdateError.OrderUpdateErrorReason.TAKE_VALUE_ERROR -> OrderUpdateErrorReason.TAKE_VALUE_ERROR
            }
        )
        is OrderValidator.IncorrectOrderDataException -> IncorrectOrderDataException(message ?: "")
        is OrderSignatureValidator.IncorrectSignatureException -> IncorrectSignatureException(message ?: "")
        is LazyAssetValidator.InvalidLazyAssetException -> InvalidLazyAssetException(message ?: "")
        else -> this
    }
}

private fun List<PartDto>.toPartList() =
    map { Part(it.account, EthUInt256.of(it.value.toLong())) }

