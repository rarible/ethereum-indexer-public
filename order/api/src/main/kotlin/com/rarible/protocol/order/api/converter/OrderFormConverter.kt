package com.rarible.protocol.order.api.converter

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.LazyErc1155Dto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.OrderFormDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.order.api.form.OrderForm
import com.rarible.protocol.order.api.misc.data
import com.rarible.protocol.order.core.converters.model.AssetConverter
import com.rarible.protocol.order.core.converters.model.OrderDataConverter
import com.rarible.protocol.order.core.converters.model.OrderTypeConverter
import com.rarible.protocol.order.core.exception.EntityNotFoundApiException
import com.rarible.protocol.order.core.exception.OrderDataException
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc1155LazyAssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.Erc721LazyAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.service.CommonSigner
import com.rarible.protocol.order.core.service.nft.NftItemApiService
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger

@Component
class OrderFormConverter(
    private val nftItemApiService: NftItemApiService,
    private val commonSigner: CommonSigner,
) {

    suspend fun convert(formDto: OrderFormDto): OrderForm {
        val maker = formDto.maker
        val make = checkLazyNftMake(maker, AssetConverter.convert(formDto.make))
        val take = checkLazyNft(AssetConverter.convert(formDto.take))
        val data = OrderDataConverter.convert(formDto.data)
        val hash = Order.hashKey(formDto.maker, make.type, take.type, formDto.salt, data)
        val signature = commonSigner.fixSignature(formDto.signature)
        return OrderForm(
            maker = maker,
            make = make,
            take = take,
            taker = formDto.taker,
            type = OrderTypeConverter.convert(formDto),
            salt = formDto.salt,
            start = formDto.start,
            end = formDto.end,
            data = data,
            signature = signature,
            hash = hash,
        )
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

    private fun List<PartDto>.toPartList() =
        map { Part(it.account, EthUInt256.of(it.value.toLong())) }
}
