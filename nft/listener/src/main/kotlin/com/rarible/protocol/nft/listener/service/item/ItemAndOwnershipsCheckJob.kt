package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.task.TaskService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ItemAndOwnershipsCheckJob(
    private val taskService: TaskService
) {
    @Scheduled(
        fixedDelayString = "\${listener.itemAndOwnershipsCheckRate",
        initialDelay = 60000
    )
    fun itemAndOwnershipsCheckJob() {
        taskService.runTask(ItemAndOwnershipsCheckTaskHandler.ITEM_AND_OWNERSHIPS_CHECK, param = "")
    }
}