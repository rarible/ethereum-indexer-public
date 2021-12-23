package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.task.TaskService
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class ItemReduceTaskHandlerInitializer(
    private val taskService: TaskService
) {
    @PostConstruct
    fun init() {
        taskService.runTask(ItemReduceTaskHandler.ITEM_REDUCE, "")
    }
}
