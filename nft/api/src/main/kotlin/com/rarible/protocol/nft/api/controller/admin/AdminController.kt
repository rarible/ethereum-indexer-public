package com.rarible.protocol.nft.api.controller.admin

import com.rarible.core.task.Task
import com.rarible.protocol.nft.api.dto.AdminTaskDto
import com.rarible.protocol.nft.api.service.admin.ReindexTokenService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import scalether.domain.Address

@RestController
class AdminController(
    private val reindexTokenService: ReindexTokenService
) {
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
}
