package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.nft.core.model.ExtendedItem
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class ExtendedItemDtoConverter(
    @Value("\${nft.api.item.owners.size.limit:5000}") private val ownersSizeLimit: Int
) : Converter<ExtendedItem, NftItemDto> {
    override fun convert(source: ExtendedItem): NftItemDto {
        val (item, meta) = source
        // TODO: RPN-497: until we've found a better solution, we limit the number of owners in the NftItem
        //  to avoid "too big Kafka message" errors.
        val limitedOwners = item.owners.take(ownersSizeLimit)
        return NftItemDto(
            id = item.id.decimalStringValue,
            contract = item.token,
            tokenId = item.tokenId.value,
            creators = item.creators.map { PartDtoConverter.convert(it) },
            supply = item.supply.value,
            lazySupply = item.lazySupply.value,
            owners = limitedOwners,
            royalties = item.royalties.map { PartDtoConverter.convert(it) },
            date = item.date,
            pending = item.pending.map { ItemTransferDtoConverter.convert(it) },
            deleted = item.deleted,
            meta = NftItemMetaDtoConverter.convert(meta)
        )
    }
}
