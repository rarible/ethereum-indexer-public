package com.rarible.protocol.nft.api.controller

import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.nft.api.service.mint.LazyNftValidator
import com.rarible.protocol.nft.api.service.mint.MintService
import com.rarible.protocol.nft.core.converters.dto.ItemDtoConverter
import com.rarible.protocol.nft.core.converters.model.LazyNftDtoToLazyItemHistoryConverter
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class LazyMintController(
    private val mintService: MintService,
    private val lazyNftValidator: LazyNftValidator
) : NftLazyMintControllerApi {

    override suspend fun mintNftAsset(lazyNftDto: LazyNftDto): ResponseEntity<NftItemDto> {
        val result = lazyNftDto
            .apply { lazyNftValidator.validate(this) }
            .let { LazyNftDtoToLazyItemHistoryConverter.convert(it) }
            .let { mintService.createLazyNft(it) }
            .let { ItemDtoConverter.convert(it) }
        return ResponseEntity.ok(result)
    }
}
