package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.common.nowMillis
import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ItemWithHungPendingTaskHandler(
    private val itemRepository: ItemRepository,
    private val mongo: ReactiveMongoOperations
) : TaskHandler<String> {

    override val type: String
        get() = FIX_HUNG_PENDING


    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask("", null))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val now = nowMillis().epochSecond
        val checkDistance = Duration.ofHours(1).seconds

        val query = Query(
            Criteria().andOperator(
                Criteria.where("${Item::pending.name}.0").exists(true)
            )
        )
        return itemRepository.search(query).map { item ->
            val itemId = item.id.stringValue
            val mintedAt = item.mintedAt?.epochSecond
            if (mintedAt != null && now - mintedAt > checkDistance) {
                logger.info("Found hung item $itemId, mintedAt=${item.mintedAt}. (item=$item)")
            }
            itemId
        }
    }

    companion object {
        const val FIX_HUNG_PENDING = "FIX_HUNG_PENDING"
        private val logger = LoggerFactory.getLogger(ItemWithHungPendingTaskHandler::class.java)
    }
}
