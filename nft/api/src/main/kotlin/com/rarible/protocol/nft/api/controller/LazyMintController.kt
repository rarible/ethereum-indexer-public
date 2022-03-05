package com.rarible.protocol.nft.api.controller

import com.rarible.core.common.convert
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.nft.api.service.mint.LazyNftValidator
import com.rarible.protocol.nft.api.service.mint.MintService
import com.rarible.protocol.nft.core.model.ItemLazyMint
import org.springframework.core.convert.ConversionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class LazyMintController(
    private val mintService: MintService,
    private val lazyNftValidator: LazyNftValidator,
    private val conversionService: ConversionService
) : NftLazyMintControllerApi {

    override suspend fun mintNftAsset(lazyNftDto: LazyNftDto): ResponseEntity<NftItemDto> {
        val result = lazyNftDto
            .apply { lazyNftValidator.validate(this) }
            .let { conversionService.convert<ItemLazyMint>(it) }
            .let { mintService.createLazyNft(it) }
            .let { conversionService.convert<NftItemDto>(it) }
        return ResponseEntity.ok(result)
    }
}
