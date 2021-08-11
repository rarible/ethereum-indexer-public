package com.rarible.protocol.nftorder.core.converter

import com.rarible.protocol.dto.ItemHistoryDto
import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.dto.NftOrderOwnershipDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.nftorder.core.model.ItemTransfer
import com.rarible.protocol.nftorder.core.model.Ownership
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OwnershipToDtoConverter : Converter<Ownership, NftOrderOwnershipDto> {
    override fun convert(source: Ownership): NftOrderOwnershipDto {
        return NftOrderOwnershipDto(
            id = source.id.decimalStringValue,
            contract = source.contract,
            tokenId = source.tokenId.value,
            owner = source.owner,
            creators = source.creators.map { PartDto(it.account, it.value) },
            value = source.value.value,
            lazyValue = source.lazyValue.value,
            date = source.date,
            pending = source.pending.map { itemHistory(it) },
            bestSellOrder = source.bestSellOrder
        )
    }

    private fun itemHistory(source: ItemTransfer): ItemHistoryDto =
        ItemTransferDto(
            owner = source.owner,
            contract = source.token,
            tokenId = source.tokenId.value,
            date = source.date,
            value = source.value.value,
            from = source.from
        )
}