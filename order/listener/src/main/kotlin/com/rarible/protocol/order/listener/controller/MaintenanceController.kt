package com.rarible.protocol.order.listener.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.common.ifNotBlank
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskService
import com.rarible.protocol.order.core.model.OrderDataVersion
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.listener.service.order.RemoveOutdatedOrdersTaskHandler
import com.rarible.protocol.order.listener.service.order.RemoveOutdatedOrdersTaskParams
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class MaintenanceController(
    private val taskRepository: TaskRepository,
    private val taskService: TaskService,
    private val objectMapper: ObjectMapper,
) {

    @GetMapping("/maintenance/cancelOrders")
    suspend fun cancelOrders(
        @RequestParam platform: Platform,
        @RequestParam status: OrderStatus,
        @RequestParam type: OrderType,
        @RequestParam version: OrderDataVersion,
        @RequestParam(required = false, defaultValue = "") contractAddress: String?,
    ) {
        val params = objectMapper.writeValueAsString(
            RemoveOutdatedOrdersTaskParams(
                platform = platform,
                status = status,
                type = type,
                version = version,
                contractAddress = contractAddress.ifNotBlank(),
            )
        )
        logger.info("Schedule order cancellation with params: $params")
        val task = taskRepository.findByTypeAndParam(RemoveOutdatedOrdersTaskHandler.TYPE, params).awaitFirstOrNull()
        if (task != null) {
            taskRepository.delete(task).awaitFirstOrNull()
        }
        taskRepository.save(
            Task(
                type = RemoveOutdatedOrdersTaskHandler.TYPE,
                param = params,
                running = false,
            )
        ).awaitFirstOrNull()
        taskService.runTask(RemoveOutdatedOrdersTaskHandler.TYPE, params)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MaintenanceController::class.java)
    }
}
