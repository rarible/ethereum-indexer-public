package com.rarible.protocol.nft.listener.admin

import com.rarible.core.task.TaskService
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.core.model.ReduceTokenItemsTaskParams.Companion.ADMIN_REDUCE_TOKEN_ITEMS
import com.rarible.protocol.nft.core.model.ReduceTokenTaskParams.Companion.ADMIN_REDUCE_TOKEN
import com.rarible.protocol.nft.core.model.ReindexTokenItemRoyaltiesTaskParam.Companion.ADMIN_REINDEX_TOKEN_ITEM_ROYALTIES
import com.rarible.protocol.nft.core.model.ReindexTokenItemsTaskParams.Companion.ADMIN_REINDEX_TOKEN_ITEMS
import com.rarible.protocol.nft.core.model.ReindexTokenTaskParams.Companion.ADMIN_REINDEX_TOKEN
import com.rarible.protocol.nft.core.model.ReindexCryptoPunksTaskParam.Companion.ADMIN_REINDEX_CRYPTO_PUNKS
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@FlowPreview
@ExperimentalCoroutinesApi
@Profile("!integration")
class AdminTaskHandlersInitializer(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val taskService: TaskService,
    private val taskRepository: TempTaskRepository
) {
    init {
        logger.info("Configured task handlers: ${taskService.handlersMap.keys}")
    }

    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    fun init() = runBlocking<Unit> {
        listOf(
            ADMIN_REINDEX_TOKEN,
            ADMIN_REDUCE_TOKEN,
            ADMIN_REINDEX_TOKEN_ITEMS,
            ADMIN_REDUCE_TOKEN_ITEMS,
            ADMIN_REINDEX_TOKEN_ITEM_ROYALTIES,
            ADMIN_REINDEX_CRYPTO_PUNKS
        ).forEach { type -> run(type) }
    }

    private suspend fun run(type: String) {
        taskRepository
            .findByType(type)
            .filter { it.lastStatus != TaskStatus.COMPLETED }
            .collect { taskService.runTask(type, it.param) }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(AdminTaskHandlersInitializer::class.java)
    }
}
