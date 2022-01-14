package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.ReduceVersion
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class OwnershipDtoConverter(
    private val featureFlags: FeatureFlags
) : Converter<Ownership, NftOwnershipDto> {

    override fun convert(source: Ownership): NftOwnershipDto {
        return NftOwnershipDto(
            id = source.id.decimalStringValue,
            contract = source.token,
            tokenId = source.tokenId.value,
            owner = source.owner,
            creators = source.creators.map { PartDtoConverter.convert(it) },
            value = source.value.value,
            lazyValue = source.lazyValue.value,
            date = source.date,
            pending = convertPending(source)
        )
    }

    private fun convertPending(source: Ownership): List<ItemTransferDto> {
        return if (featureFlags.reduceVersion == ReduceVersion.V1) {
            source.pending.map { ItemTransferDtoConverter.convert(it) }
        } else {
            source.getPendingEvents().mapNotNull { ItemTransferDtoConverter.convert(it) }
        }
    }
}
