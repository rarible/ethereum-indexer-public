package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.task.TaskHandler
import com.rarible.core.telemetry.metrics.RegisteredCounter
import kotlinx.coroutines.flow.Flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemAndOwnershipsCheckTaskHandler(
    private val itemDataQualityService: ItemDataQualityService,
    private val itemDataQualityJobRunRegisteredCounter: RegisteredCounter
) : TaskHandler<String> {

    override val type: String
        get() = ITEM_AND_OWNERSHIPS_CHECK

    override fun runLongTask(from: String?, param: String): Flow<String> {
        logger.info("Start item/ownership data quality check from $from")
        itemDataQualityJobRunRegisteredCounter.increment()
        return itemDataQualityService.checkItems(from)
    }

    companion object {
        const val ITEM_AND_OWNERSHIPS_CHECK = "ITEM_AND_OWNERSHIPS_CHECK"
        private val logger = LoggerFactory.getLogger(ItemAndOwnershipsCheckTaskHandler::class.java)
    }
}