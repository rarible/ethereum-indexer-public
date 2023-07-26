package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@FlowPreview
@ExperimentalCoroutinesApi
@Component
@Profile("!integration")
class OrderReduceTaskHandlerInitializer(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val taskService: TaskService
) {
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    fun init() {
        taskService.runTask(OrderReduceTaskHandler.ORDER_REDUCE, "")
    }
}
