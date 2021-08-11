package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.nft.core.model.ItemTransfer
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object ItemTransferDtoConverter : Converter<ItemTransfer, ItemTransferDto> {

    override fun convert(source: ItemTransfer): ItemTransferDto {
        return ItemTransferDto(
            owner = source.owner,
            contract = source.token,
            tokenId = source.tokenId.value,
            date = source.date,
            value = source.value.value,
            from = source.from
        )
    }
}