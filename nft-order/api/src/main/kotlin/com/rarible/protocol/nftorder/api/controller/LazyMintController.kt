package com.rarible.protocol.nftorder.api.controller

import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.NftOrderItemDto
import com.rarible.protocol.nftorder.api.service.LazyMintApiService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class LazyMintController(
    private val lazyMintApiService: LazyMintApiService
) : NftOrderLazyMintControllerApi {

    override suspend fun mintNftOrderAsset(lazyNftDto: LazyNftDto): ResponseEntity<NftOrderItemDto> {
        val result = lazyMintApiService.mintAsset(lazyNftDto)
        return ResponseEntity.ok(result)
    }
}