package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@Component
class OnChainOrderVersionInsertionTaskHandlerInitializer(
    private val taskService: TaskService
) {
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    fun init() {
        taskService.runTask(OnChainOrderVersionInsertionTaskHandler.TYPE, "")
    }
}
