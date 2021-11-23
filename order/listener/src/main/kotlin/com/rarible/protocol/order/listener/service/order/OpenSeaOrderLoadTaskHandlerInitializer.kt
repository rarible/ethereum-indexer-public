package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@Component
@Profile("!integration")
class OpenSeaOrderLoadTaskHandlerInitializer(
    private val taskService: TaskService
) {
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    fun init() {
        taskService.runTask(OpenSeaOrderLoadTaskHandler.OPEN_SEA_ORDER_LOAD, "")
    }
}
