package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskService
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("!integration")
class LoadOpenSeaOrdersTaskHandlerInitializer(
    private val taskService: TaskService
) {
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    fun init() {
        taskService.runTask(LoadOpenSeaOrdersTaskHandler.LOAD_OPEN_SEA_ORDERS, "")
    }
}
