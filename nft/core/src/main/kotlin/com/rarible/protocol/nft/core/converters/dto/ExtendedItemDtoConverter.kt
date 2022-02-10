package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.nft.core.model.ExtendedItem
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ReduceVersion
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ExtendedItemDtoConverter(
    @Value("\${nft.api.item.owners.size.limit:5000}") private val ownersSizeLimit: Int,
    private val featureFlags: FeatureFlags,
    private val nftItemMetaDtoConverter: NftItemMetaDtoConverter
) : Converter<ExtendedItem, NftItemDto> {
    override fun convert(source: ExtendedItem): NftItemDto {
        val (item, meta) = source
        val itemIdDecimalValue = item.id.decimalStringValue
        return NftItemDto(
            id = itemIdDecimalValue,
            contract = item.token,
            tokenId = item.tokenId.value,
            creators = item.creators.map { PartDtoConverter.convert(it) },
            supply = item.supply.value,
            lazySupply = item.lazySupply.value,
            // TODO: RPN-497: until we've found a better solution, we limit the number of owners in the NftItem
            //  to avoid "too big Kafka message" errors.
            owners = convertOwnership(item).take(ownersSizeLimit),
            royalties = item.royalties.map { PartDtoConverter.convert(it) },
            lastUpdatedAt = item.date,
            mintedAt = item.mintedAt,
            pending = convertPending(item),
            deleted = item.deleted,
            meta = nftItemMetaDtoConverter.convert(meta, itemIdDecimalValue)
        )
    }

    private fun convertOwnership(item: Item): Collection<Address> {
        return if (featureFlags.reduceVersion == ReduceVersion.V1) item.owners else emptyList()
    }

    private fun convertPending(item: Item): List<ItemTransferDto> {
        return if (featureFlags.reduceVersion == ReduceVersion.V1) {
            item.pending.map { ItemTransferDtoConverter.convert(it) }
        } else {
            item.getPendingEvents().mapNotNull { ItemTransferDtoConverter.convert(it) }
        }
    }
}
