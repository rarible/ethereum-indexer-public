package com.rarible.protocol.nft.api.service.admin

import com.rarible.core.task.Task
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.model.ReindexTokenItemsTaskParams.Companion.SUPPORTED_REINDEX_TOKEN_STANDARD
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ReindexTokenService(
    private val tokenRegistrationService: TokenRegistrationService,
    private val tokenRepository: TokenRepository,
    private val taskRepository: TempTaskRepository
) {
    suspend fun getToken(token: Address): Token? {
        return tokenRepository.findById(token).awaitFirstOrNull()
    }

    suspend fun removeToken(token: Address) {
        tokenRepository.remove(token).awaitFirstOrNull()
    }

    suspend fun getTokenTasks(): List<Task> {
        return taskRepository.findByType(ReindexTokenItemsTaskParams.ADMIN_REINDEX_TOKEN_ITEMS).toList() +
            taskRepository.findByType(ReduceTokenItemsTaskParams.ADMIN_REDUCE_TOKEN_ITEMS).toList() +
            taskRepository.findByType(ReindexTokenItemRoyaltiesTaskParam.ADMIN_REINDEX_TOKEN_ITEM_ROYALTIES).toList()
    }

    suspend fun createReindexTokenTask(tokens: List<Address>, fromBlock: Long?): Task {
        val params = ReindexTokenTaskParams(tokens)
        checkOtherTasksAreNotProcessingTheSameTokens(params, ReindexTokenTaskParams.ADMIN_REINDEX_TOKEN)
        return saveTask(
            params = params.toParamString(),
            type = ReindexTokenTaskParams.ADMIN_REINDEX_TOKEN,
            state = fromBlock
        )
    }

    suspend fun createReindexTokenItemsTask(tokens: List<Address>, fromBlock: Long?): Task {
        val tokenStandardMap = tokens.toSet().map { it to tokenRegistrationService.getTokenStandard(it).awaitFirst() }
        val standards = tokenStandardMap.map { it.second }.toSet()

        if (standards.size != 1) {
            throw IllegalArgumentException("All tokens must be the same standard: ${formatToString(tokenStandardMap)}")
        }
        if (standards.first() !in SUPPORTED_REINDEX_TOKEN_STANDARD) {
            throw IllegalArgumentException("Reindex support only $SUPPORTED_REINDEX_TOKEN_STANDARD, but you have ${formatToString(tokenStandardMap)}")
        }
        val params = ReindexTokenItemsTaskParams(standards.first(), tokens)
        checkOtherTasksAreNotProcessingTheSameTokens(params, ReindexTokenItemsTaskParams.ADMIN_REINDEX_TOKEN_ITEMS)
        return saveTask(
            params = params.toParamString(),
            type = ReindexTokenItemsTaskParams.ADMIN_REINDEX_TOKEN_ITEMS,
            state = fromBlock
        )
    }

    suspend fun createReduceTokenTask(token: Address): Task {
        val params = ReduceTokenTaskParams(token)
        checkOtherTasksAreNotProcessingTheSameTokens(params, ReduceTokenTaskParams.ADMIN_REDUCE_TOKEN)
        return saveTask(params.toParamString(), ReduceTokenTaskParams.ADMIN_REDUCE_TOKEN, state = null)
    }

    suspend fun createReduceTokenItemsTask(token: Address): Task {
        val params = ReduceTokenItemsTaskParams(token)
        checkOtherTasksAreNotProcessingTheSameTokens(params, ReduceTokenItemsTaskParams.ADMIN_REDUCE_TOKEN_ITEMS)
        return saveTask(params.toParamString(), ReduceTokenItemsTaskParams.ADMIN_REDUCE_TOKEN_ITEMS, state = null)
    }

    suspend fun createReindexTokenItemRoyaltiesTask(token: Address): Task {
        val params = ReindexTokenItemRoyaltiesTaskParam(token)
        checkOtherTasksAreNotProcessingTheSameTokens(
            params,
            ReindexTokenItemRoyaltiesTaskParam.ADMIN_REINDEX_TOKEN_ITEM_ROYALTIES
        )
        return saveTask(
            params = params.toParamString(),
            type = ReindexTokenItemRoyaltiesTaskParam.ADMIN_REINDEX_TOKEN_ITEM_ROYALTIES,
            state = null
        )
    }

    private suspend fun checkOtherTasksAreNotProcessingTheSameTokens(params: TokenTaskParam, type: String) {
        taskRepository.findByType(type).collect { task ->
            if (task.lastStatus != TaskStatus.COMPLETED) {
                val tokensBeingIndexed = TokenTaskParam.fromParamString(params::class, task.param)
                    .tokens.filter { it in params.tokens }
                if (tokensBeingIndexed.isNotEmpty()) {
                    throw IllegalArgumentException("Tokens $tokensBeingIndexed are already being indexed in another task ${task.id}: $task")
                }
            }
        }
    }

    private suspend fun saveTask(params: String, type: String, state: Any?): Task {
        return saveTask(
            Task(
                type = type,
                param = params,
                state = state,
                running = false,
                lastStatus = TaskStatus.NONE
            )
        )
    }

    private suspend fun saveTask(task: Task): Task {
        return try {
            taskRepository.save(task)
        } catch (ex: Exception) {
            when (ex) {
                is OptimisticLockingFailureException, is DuplicateKeyException -> {
                    throw IllegalArgumentException("Reindex task already exist")
                }
                else -> throw ex
            }
        }
    }

    private fun formatToString(tokenStandardMap: List<Pair<Address, TokenStandard>>): String {
        return tokenStandardMap.joinToString(",") { "${it.second}:${it.first}" }
    }
}

