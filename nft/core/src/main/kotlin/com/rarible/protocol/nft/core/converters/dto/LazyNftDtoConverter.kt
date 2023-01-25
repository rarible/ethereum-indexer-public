package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.LazyErc1155Dto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.TokenStandard

object LazyNftDtoConverter {

    fun convert(source: ItemLazyMint): LazyNftDto {
        return when (source.standard) {
            TokenStandard.ERC721 -> LazyErc721Dto(
                contract = source.token,
                tokenId = source.tokenId.value,
                uri = source.uri,
                creators = source.creators.map { PartDtoConverter.convert(it) },
                royalties = source.royalties.map { PartDtoConverter.convert(it) },
                signatures = source.signatures
            )

            TokenStandard.ERC1155 -> LazyErc1155Dto(
                contract = source.token,
                tokenId = source.tokenId.value,
                supply = source.value.value,
                uri = source.uri,
                creators = source.creators.map { PartDtoConverter.convert(it) },
                royalties = source.royalties.map { PartDtoConverter.convert(it) },
                signatures = source.signatures
            )
            else -> throw IllegalArgumentException("Unexpected lazy item type ${source.standard}")
        }
    }
}
