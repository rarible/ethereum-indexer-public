package com.rarible.protocol.erc20.api.controller.admin

import com.rarible.core.task.Task
import com.rarible.protocol.erc20.api.dto.AdminTaskDto
import com.rarible.protocol.erc20.core.admin.service.Erc20TaskService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import scalether.domain.Address

@RestController
class AdminController(
    private val erc20TaskService: Erc20TaskService
) {

    @PostMapping(
        value = ["/admin/tokens/reindex"],
        produces = ["application/json"]
    )
    suspend fun reindexErc20Token(
        @RequestParam(name = "token", required = true) tokens: List<String>,
        @RequestParam(name = "fromBlock", required = false) fromBlock: Long?,
        @RequestParam(name = "force", required = false) force: Boolean?
    ): ResponseEntity<List<AdminTaskDto>> {
        val tasks = erc20TaskService.createReindexErc20TokenTasks(
            tokens.map { Address.apply(it) },
            fromBlock,
            force ?: false
        )
        return ResponseEntity.ok(tasks.map { convert(it) })
    }

    @PostMapping(
        value = ["/admin/tokens/reduce"],
        produces = ["application/json"]
    )
    suspend fun reduceErc20Token(
        @RequestParam(name = "token", required = true) token: String,
        @RequestParam(name = "force", required = false) force: Boolean?
    ): ResponseEntity<AdminTaskDto> {
        val task = erc20TaskService.createReduceErc20BalanceTask(
            Address.apply(token),
            force ?: false
        )
        return ResponseEntity.ok(convert(task))
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