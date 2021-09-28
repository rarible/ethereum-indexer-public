package com.rarible.protocol.nftorder.api.controller

import com.rarible.protocol.dto.NftOrderItemDto
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.service.RefreshService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RefreshController(
    private val refreshService: RefreshService
) {

    @PostMapping(
        value = ["/v0.1/refresh/item/{itemId}"],
        produces = ["application/json"]
    )
    suspend fun getNftOrderItemById(
        @PathVariable("itemId") itemId: String,
        @RequestParam(value = "full", required = false, defaultValue = "false") full: Boolean
    ): ResponseEntity<NftOrderItemDto> {
        val id = ItemId.parseId(itemId)
        val result = if (full) {
            refreshService.refreshItemWithOwnerships(id)
        } else {
            refreshService.refreshItem(id)
        }
        return ResponseEntity.ok(result)
    }

}