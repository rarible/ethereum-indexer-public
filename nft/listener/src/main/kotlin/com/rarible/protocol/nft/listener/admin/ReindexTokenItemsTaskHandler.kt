package com.rarible.protocol.nft.listener.admin

import com.rarible.contracts.erc1155.TransferBatchEvent
import com.rarible.contracts.erc721.TransferEvent
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.ethereum.listener.log.LogListenService
import com.rarible.ethereum.listener.log.ReindexTopicTaskHandler
import com.rarible.protocol.nft.core.model.ReindexTokenItemsTaskParams
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import com.rarible.protocol.nft.listener.admin.descriptor.AdminErc1155TransferLogDescriptor
import com.rarible.protocol.nft.listener.admin.descriptor.AdminErc721TransferLogDescriptor
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum

/**
 * Background job that re-indexes all items of a token (specified by `param`).
 */
class ReindexTokenItemsTaskHandler(
    private val taskRepository: TaskRepository,
    private val logListenService: LogListenService,
    private val tokenRegistrationService: TokenRegistrationService,
    private val ethereum: MonoEthereum
) : TaskHandler<Long> {

    override val type: String
        get() = ReindexTokenItemsTaskParams.ADMIN_REINDEX_TOKEN_ITEMS

    override suspend fun isAbleToRun(param: String): Boolean {
        return verifyAllReindexingTopicTasksAreCompleted(
            TransferEvent.id(),
            TransferBatchEvent.id()
        )
    }

    override fun runLongTask(from: Long?, param: String): Flow<Long> {
        val taskParam = ReindexTokenItemsTaskParams.fromParamString(param)

        return fetchNormalBlockNumber()
            .flatMapMany { to -> reindexTokenItems(taskParam, from, to) }
            .map { it.first }
            .asFlow()
    }

    private fun reindexTokenItems(params: ReindexTokenItemsTaskParams, from: Long?, end: Long): Flux<LongRange> {
        val descriptor = when (params.standard) {
            TokenStandard.ERC721 -> AdminErc721TransferLogDescriptor(tokenRegistrationService, params.tokens)
            TokenStandard.ERC1155 -> AdminErc1155TransferLogDescriptor(tokenRegistrationService, params.tokens)
            TokenStandard.CRYPTO_PUNKS, TokenStandard.DEPRECATED, TokenStandard.NONE -> return Flux.empty()
        }
        return logListenService.reindexWithDescriptor(descriptor, from ?: 1, end)
    }

    private suspend fun verifyAllReindexingTopicTasksAreCompleted(vararg topics: Word): Boolean {
        for (topic in topics) {
            val task = taskRepository.findByTypeAndParam(
                type = ReindexTopicTaskHandler.TOPIC,
                param = topic.toString()
            ).awaitFirstOrNull()
            if (task?.lastStatus != TaskStatus.COMPLETED) {
                return false
            }
        }
        return true
    }

    private fun fetchNormalBlockNumber(): Mono<Long> {
        return ethereum.ethBlockNumber().map { it.toLong() }
    }

}
