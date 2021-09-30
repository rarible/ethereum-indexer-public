package com.rarible.protocol.erc20.api.controller.admin

import com.rarible.core.task.Task
import com.rarible.protocol.erc20.api.dto.AdminTaskDto
import com.rarible.protocol.erc20.api.service.admin.AdminService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import scalether.domain.Address

@RestController
class AdminController(
    private val adminService: AdminService
) {
    @GetMapping(
        value = ["/admin/erc20/tokens/tasks/reindex"],
        produces = ["application/json"]
    )
    suspend fun createReindexTokenTask(
        @RequestParam(value = "token", required = true) collection: Address,
        @RequestParam(value = "fromBlock", required = false) fromBlock: Long?
    ): ResponseEntity<List<AdminTaskDto>> {
        val tasks = adminService.createReindexTokenTask(collection, fromBlock)
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
