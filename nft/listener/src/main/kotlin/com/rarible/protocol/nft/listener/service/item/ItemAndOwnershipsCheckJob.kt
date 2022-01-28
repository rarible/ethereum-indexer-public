package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.task.TaskService
import org.apache.commons.lang3.time.DateUtils
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ItemAndOwnershipsCheckJob(
    private val taskService: TaskService
) {
    @Scheduled(
        fixedDelayString = "\${checkWrongHashBlocksJobInterval:${DateUtils.MILLIS_PER_HOUR}}",
        initialDelayString = "\${checkWrongHashBlocksJobInterval:${DateUtils.MILLIS_PER_HOUR}}"
    )
    fun itemAndOwnershipsCheckJob() {
        taskService.runTask(ItemAndOwnershipsCheckTaskHandler.ITEM_AND_OWNERSHIPS_CHECK)
    }
}