package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.task.TaskService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ItemAndOwnershipsCheckJob(
    private val taskService: TaskService,
    private val properties: NftListenerProperties
) {
    @Scheduled(
        fixedDelayString = "\${listener.itemAndOwnershipsCheckRate}",
        initialDelay = 60000
    )
    fun itemAndOwnershipsCheckJob() {
        if (properties.enableCheckDataQualityJob.not()) return
        taskService.runTask(ItemAndOwnershipsCheckTaskHandler.ITEM_AND_OWNERSHIPS_CHECK, param = "")
    }
}