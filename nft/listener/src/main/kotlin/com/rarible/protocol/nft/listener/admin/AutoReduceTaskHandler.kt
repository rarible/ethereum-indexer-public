package com.rarible.protocol.nft.listener.admin

import com.rarible.core.logging.withTraceId
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.core.model.ADMIN_AUTO_REDUCE_TASK_TYPE
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.AutoReduceRepository
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import com.rarible.protocol.nft.core.service.token.TokenReduceService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class AutoReduceTaskHandler(
    private val taskRepository: TempTaskRepository,
    private val itemReduceService: ItemReduceService,
    private val tokenReduceService: TokenReduceService,
    private val autoReduceRepository: AutoReduceRepository,
) : TaskHandler<String> {

    override val type: String
        get() = ADMIN_AUTO_REDUCE_TASK_TYPE

    override suspend fun isAbleToRun(param: String): Boolean {
        return !isReindexInProgress()
    }

    override fun runLongTask(from: String?, param: String): Flow<String> = flow<String> {
        logger.info("Starting AutoReduceTaskHandler")
        autoReduceRepository.findItems().collect {
            val itemId = ItemId.parseId(it.id)
            itemReduceService.update(
                token = itemId.token,
                tokenId = itemId.tokenId,
                from = itemId,
                to = itemId,
            ).asFlow().collect()
            autoReduceRepository.removeItem(it)
        }
        autoReduceRepository.findTokens().collect {
            val token = Address.apply(it.id)
            tokenReduceService.reduce(token)
            autoReduceRepository.removeToken(it)
        }
        logger.info("Finished AutoReduceTaskHandler")
    }.withTraceId()

    private suspend fun isReindexInProgress(): Boolean {
        return taskRepository
            .findByType("BLOCK_SCANNER_REINDEX_TASK")
            .filter { it.lastStatus != TaskStatus.COMPLETED }
            .firstOrNull() != null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AutoReduceTaskHandler::class.java)
    }
}
