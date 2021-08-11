package com.rarible.protocol.nft.core.converters.model

import com.rarible.ethereum.nft.model.LazyERC1155
import com.rarible.ethereum.nft.model.LazyERC721
import com.rarible.ethereum.nft.model.LazyNft
import com.rarible.ethereum.nft.model.Part
import com.rarible.protocol.dto.LazyErc1155Dto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.LazyNftDto
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object LazyNftDtoToDaonomicLazyNftConverter : Converter<LazyNftDto, LazyNft> {
    override fun convert(source: LazyNftDto): LazyNft {
        return when (source) {
            is LazyErc721Dto -> {
                LazyERC721(
                    token = source.contract,
                    tokenId = source.tokenId,
                    uri = source.uri,
                    creators = source.creators.map { Part(it.account, it.value) },
                    signatures = source.signatures,
                    royalties = source.royalties.map { Part(it.account, it.value) }
                )
            }
            is LazyErc1155Dto -> {
                LazyERC1155(
                    token = source.contract,
                    tokenId = source.tokenId,
                    supply = source.supply,
                    uri = source.uri,
                    creators = source.creators.map { Part(it.account, it.value) },
                    signatures = source.signatures,
                    royalties = source.royalties.map { Part(it.account, it.value) }
                )
            }
        }
    }
}
