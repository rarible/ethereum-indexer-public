package com.rarible.protocol.nft.core.service

import com.rarible.blockchain.scanner.ethereum.task.EthereumReindexParam
import com.rarible.blockchain.scanner.reindex.BlockRange
import com.rarible.core.task.Task
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.core.misc.splitToRanges
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ReduceTokenItemsTaskParams
import com.rarible.protocol.nft.core.model.ReduceTokenRangeTaskParams
import com.rarible.protocol.nft.core.model.ReduceTokenTaskParams
import com.rarible.protocol.nft.core.model.ReindexCryptoPunksTaskParam
import com.rarible.protocol.nft.core.model.ReindexTokenItemRoyaltiesTaskParam
import com.rarible.protocol.nft.core.model.ReindexTokenItemsTaskParams
import com.rarible.protocol.nft.core.model.ReindexTokenItemsTaskParams.Companion.SUPPORTED_REINDEX_TOKEN_STANDARD
import com.rarible.protocol.nft.core.model.ReindexTokenTaskParams
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.model.TokenTaskParam
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ReindexTokenService(
    private val tokenRegistrationService: TokenRegistrationService,
    private val taskRepository: TempTaskRepository,
    private val nftHistoryRepository: NftHistoryRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getTokenTasks(): List<Task> {
        return taskRepository.findByType(ReindexTokenItemsTaskParams.ADMIN_REINDEX_TOKEN_ITEMS).toList() +
            taskRepository.findByType(ReduceTokenItemsTaskParams.ADMIN_REDUCE_TOKEN_ITEMS).toList() +
            taskRepository.findByType(ReindexTokenItemRoyaltiesTaskParam.ADMIN_REINDEX_TOKEN_ITEM_ROYALTIES).toList()
    }

    suspend fun createReindexTokenTask(tokens: List<Address>, fromBlock: Long?, force: Boolean): Task {
        val params = ReindexTokenTaskParams(tokens)
        if (!force) checkOtherTasksAreNotProcessingTheSameTokens(params, ReindexTokenTaskParams.ADMIN_REINDEX_TOKEN)
        return saveTask(
            param = params.toParamString(),
            type = ReindexTokenTaskParams.ADMIN_REINDEX_TOKEN,
            state = fromBlock,
            force = force
        )
    }

    suspend fun createReindexTokenItemsTask(tokens: List<Address>, fromBlock: Long?, force: Boolean): Task {
        val tokenStandardMap = tokens.toSet().map { it to tokenRegistrationService.getTokenStandard(it).awaitFirst() }
        val standards = tokenStandardMap.map { it.second }.toSet()

        if (standards.size != 1) {
            throw IllegalArgumentException("All tokens must be the same standard: ${formatToString(tokenStandardMap)}")
        }
        if (standards.first() !in SUPPORTED_REINDEX_TOKEN_STANDARD) {
            throw IllegalArgumentException(
                "Reindex support only $SUPPORTED_REINDEX_TOKEN_STANDARD, but you have ${
                    formatToString(
                        tokenStandardMap
                    )
                }"
            )
        }
        val params = ReindexTokenItemsTaskParams(standards.first(), tokens)
        if (!force) checkOtherTasksAreNotProcessingTheSameTokens(
            params, ReindexTokenItemsTaskParams.ADMIN_REINDEX_TOKEN_ITEMS
        )
        return saveTask(
            param = params.toParamString(),
            type = ReindexTokenItemsTaskParams.ADMIN_REINDEX_TOKEN_ITEMS,
            state = fromBlock,
            force = force
        )
    }

    suspend fun createReduceTokenTask(token: Address, force: Boolean): Task {
        val params = ReduceTokenTaskParams(token)
        if (!force) checkOtherTasksAreNotProcessingTheSameTokens(params, ReduceTokenTaskParams.ADMIN_REDUCE_TOKEN)
        return saveTask(
            param = params.toParamString(),
            type = ReduceTokenTaskParams.ADMIN_REDUCE_TOKEN,
            state = null, force = force
        )
    }

    suspend fun createReduceTokenItemsTask(token: Address, force: Boolean): Task {
        val params = ReduceTokenItemsTaskParams(token)
        if (!force) checkOtherTasksAreNotProcessingTheSameTokens(
            params, ReduceTokenItemsTaskParams.ADMIN_REDUCE_TOKEN_ITEMS
        )
        return saveTask(
            param = params.toParamString(),
            type = ReduceTokenItemsTaskParams.ADMIN_REDUCE_TOKEN_ITEMS,
            state = null,
            force = force
        )
    }

    suspend fun createReduceTokenRangeTask(from: ItemId, to: ItemId, taskCount: Int, force: Boolean = false) {
        val parent = "${from.stringValue}..${to.stringValue}"

        val withSameParent = taskRepository.findByType(ReduceTokenRangeTaskParams.ADMIN_REDUCE_TOKEN_RANGE)
            .filter { ReduceTokenRangeTaskParams.parse(it.param).parent == parent }
            .toList()

        val notCompleted = withSameParent.filter { it.lastStatus != TaskStatus.COMPLETED }
        if (notCompleted.isNotEmpty() && !force) {
            throw java.lang.IllegalArgumentException(
                "Can't start ReduceTokenRangeTasks with range $parent," +
                    " there are still ${notCompleted.size} not completed tasks"
            )
        }

        withSameParent.forEach { taskRepository.delete(it.id) }

        splitToRanges(from, to, taskCount)
            .map { ReduceTokenRangeTaskParams(parent, it.first.stringValue, it.second.stringValue) }
            .forEach {
                saveTask(
                    param = it.toParamString(),
                    type = ReduceTokenRangeTaskParams.ADMIN_REDUCE_TOKEN_RANGE,
                    state = null,
                    force = false
                )
            }
    }

    suspend fun createReindexTokenItemRoyaltiesTask(token: Address, force: Boolean): Task {
        val params = ReindexTokenItemRoyaltiesTaskParam(token)
        if (!force) checkOtherTasksAreNotProcessingTheSameTokens(
            params,
            ReindexTokenItemRoyaltiesTaskParam.ADMIN_REINDEX_TOKEN_ITEM_ROYALTIES
        )
        return saveTask(
            param = params.toParamString(),
            type = ReindexTokenItemRoyaltiesTaskParam.ADMIN_REINDEX_TOKEN_ITEM_ROYALTIES,
            state = null,
            force = force
        )
    }

    suspend fun createReindexCryptoPunksTasks(currentBlock: Long): List<Task> {
        val startBlock = 1L
        val step = 500_000L

        val createdTasks = mutableListOf<Task>()
        ReindexCryptoPunksTaskParam.PunkEvent.values().forEach { event ->
            var from = startBlock
            while (from < currentBlock) {
                val task = saveTask(
                    param = ReindexCryptoPunksTaskParam(event = event, from = from).toParamString(),
                    type = ReindexCryptoPunksTaskParam.ADMIN_REINDEX_CRYPTO_PUNKS,
                    state = null,
                    force = false
                )
                createdTasks.add(task)
                from += step
            }
        }
        return createdTasks
    }

    suspend fun createReindexTokenTask(tokens: List<Address>, force: Boolean? = null) {
        val startBlock: Long? = tokens.mapNotNull {
            val history = nftHistoryRepository.findAllByCollection(it).awaitFirstOrNull()
            history?.blockNumber
        }.minOrNull()
        if (startBlock != null) {
            saveTask(
                param = EthereumReindexParam(
                    range = BlockRange(startBlock, null, 250),
                    topics = emptyList(),
                    addresses = tokens
                ).toString(),
                type = "BLOCK_SCANNER_REINDEX_TASK",
                state = "NONE",
                force = false
            )
        } else {
            logger.warn("Log with block number wasn't found for ${tokens}")
        }
    }

    private suspend fun checkOtherTasksAreNotProcessingTheSameTokens(params: TokenTaskParam, type: String) {
        taskRepository.findByType(type).collect { task ->
            if (task.lastStatus != TaskStatus.COMPLETED) {
                val tokensBeingIndexed = TokenTaskParam.fromParamString(params::class, task.param)
                    .tokens.filter { it in params.tokens }
                if (tokensBeingIndexed.isNotEmpty()) {
                    throw IllegalArgumentException(
                        "Tokens $tokensBeingIndexed are already being indexed in another task ${task.id}: $task"
                    )
                }
            }
        }
    }

    private suspend fun saveTask(
        param: String,
        type: String,
        state: Any?,
        force: Boolean
    ): Task {
        return try {
            val newTask = if (force) {
                taskRepository.findByType(type, param).firstOrNull()?.copy(
                    state = state,
                    running = false,
                    lastStatus = TaskStatus.NONE
                )
            } else {
                null
            } ?: Task(
                type = type,
                param = param,
                state = state,
                running = false,
                lastStatus = TaskStatus.NONE
            )
            taskRepository.save(newTask)
        } catch (ex: Exception) {
            when (ex) {
                is OptimisticLockingFailureException, is DuplicateKeyException -> {
                    throw IllegalArgumentException("Reindex task already exists")
                }
                else -> throw ex
            }
        }
    }

    private fun formatToString(tokenStandardMap: List<Pair<Address, TokenStandard>>): String {
        return tokenStandardMap.joinToString(",") { "${it.second}:${it.first}" }
    }
}
