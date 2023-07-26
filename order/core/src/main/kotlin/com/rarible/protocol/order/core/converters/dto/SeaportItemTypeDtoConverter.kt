package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.SeaportItemTypeDto
import com.rarible.protocol.order.core.model.SeaportItemType

object SeaportItemTypeDtoConverter {
    fun convert(source: SeaportItemType): SeaportItemTypeDto {
        return when (source) {
            SeaportItemType.NATIVE -> SeaportItemTypeDto.NATIVE
            SeaportItemType.ERC20 -> SeaportItemTypeDto.ERC20
            SeaportItemType.ERC721 -> SeaportItemTypeDto.ERC721
            SeaportItemType.ERC1155 -> SeaportItemTypeDto.ERC1155
            SeaportItemType.ERC721_WITH_CRITERIA -> SeaportItemTypeDto.ERC721_WITH_CRITERIA
            SeaportItemType.ERC1155_WITH_CRITERIA -> SeaportItemTypeDto.ERC1155_WITH_CRITERIA
        }
    }
}
