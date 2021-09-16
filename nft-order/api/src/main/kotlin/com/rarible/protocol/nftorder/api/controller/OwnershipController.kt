package com.rarible.protocol.nftorder.api.controller

import com.rarible.protocol.dto.NftOrderOwnershipDto
import com.rarible.protocol.dto.NftOrderOwnershipsPageDto
import com.rarible.protocol.nftorder.api.service.OwnershipApiService
import com.rarible.protocol.nftorder.core.model.OwnershipId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class OwnershipController(
    val ownershipApiService: OwnershipApiService
) : NftOrderOwnershipControllerApi {

    override suspend fun getNftOrderAllOwnerships(
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftOrderOwnershipsPageDto> {
        val result = ownershipApiService.getAllOwnerships(continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOrderOwnershipById(ownershipId: String): ResponseEntity<NftOrderOwnershipDto> {
        val id = OwnershipId.parseId(ownershipId)
        val result = ownershipApiService.getOwnershipById(id)
        return ResponseEntity.ok(result)
    }

    override suspend fun getNftOrderOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<NftOrderOwnershipsPageDto> {
        val result = ownershipApiService.getOwnershipsByItem(contract, tokenId, continuation, size)
        return ResponseEntity.ok(result)
    }
}