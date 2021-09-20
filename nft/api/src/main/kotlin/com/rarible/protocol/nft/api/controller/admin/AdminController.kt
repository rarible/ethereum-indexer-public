package com.rarible.protocol.nft.api.controller.admin

import com.rarible.core.task.Task
import com.rarible.protocol.nft.api.dto.AdminTaskDto
import com.rarible.protocol.nft.api.dto.TokenDto
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.api.service.admin.ReindexTokenService
import com.rarible.protocol.nft.core.model.Token
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import scalether.domain.Address

@RestController
class AdminController(
    private val reindexTokenService: ReindexTokenService
) {
    @GetMapping(
        value = ["/admin/nft/collections/{collectionId}"],
        produces = ["application/json"]
    )
    suspend fun getTokenById(
        @PathVariable("collectionId") collectionId: Address
    ): ResponseEntity<TokenDto> {
        val token = reindexTokenService.getToken(collectionId)
            ?: throw EntityNotFoundApiException("Collection", collectionId)
        return ResponseEntity.ok().body(convert(token))
    }

    @DeleteMapping(
        value = ["/admin/nft/collections/{collectionId}"],
        produces = ["application/json"]
    )
    suspend fun deleteTokenById(
        @PathVariable("collectionId") collectionId: Address
    ): ResponseEntity<Void> {
        reindexTokenService.removeToken(collectionId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping(
        value = ["/admin/nft/collections/tasks/reindex"],
        produces = ["application/json"]
    )
    suspend fun createReindexTokenTask(
        @RequestParam(value = "collection", required = true) collection: List<Address>,
        @RequestParam(value = "fromBlock", required = false) fromBlock: Long?
    ): ResponseEntity<AdminTaskDto> {
        val task = reindexTokenService.createReindexTokenTask(collection, fromBlock)
        return ResponseEntity.ok().body(convert(task))
    }

    @GetMapping(
        value = ["/admin/nft/collections/tasks"],
        produces = ["application/json"]
    )
    suspend fun getTokenTasks(): ResponseEntity<List<AdminTaskDto>> {
        val tasks = reindexTokenService.getTokenTasks()
        return ResponseEntity.ok().body(tasks.map { convert(it) })
    }

    private fun convert(task: Task): AdminTaskDto {
        return AdminTaskDto(
            id = task.id.toHexString(),
            type = task.type,
            status = task.lastStatus.toString(),
            error = task.lastError,
            params = task.param,
            state = task.state.toString()
        )
    }

    private fun convert(token: Token): TokenDto {
        return TokenDto(
            id = token.id,
            standard = token.standard.name,
            owner = token.owner,
            name = token.name,
            symbol = token.symbol,
            features = token.features.map { it.name }
        )
    }
}
