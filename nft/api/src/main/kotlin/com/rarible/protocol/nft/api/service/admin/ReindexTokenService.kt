package com.rarible.protocol.nft.api.service.admin

import com.rarible.core.task.Task
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import scalether.domain.Address
import com.rarible.protocol.nft.api.exceptions.IllegalArgumentException
import com.rarible.protocol.nft.core.model.ReindexTokenTaskParams
import com.rarible.protocol.nft.core.model.TokenStandard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Service

@Component
class ReindexTokenService(
    private val tokenRegistrationService: TokenRegistrationService,
    private val taskRepository: TempTaskRepository
) {
    suspend fun getTokenTasks(): List<Task> {
        return taskRepository.findByType(ReindexTokenTaskParams.ADMIN_REINDEX_TOKEN).toList()
    }

    suspend fun createReindexTokenTask(tokens: List<Address>, fromBlock: Long?): Task {
        val tokenStandardMap = tokens.toSet().map { it to tokenRegistrationService.getTokenStandard(it).awaitFirst() }
        val standards = tokenStandardMap.map { it.second }.toSet()

        if (standards.size != 1) {
            throw IllegalArgumentException("All tokens must be the same standard: ${formatToString(tokenStandardMap)}")
        }
        if (standards.first() !in SUPPORTED_REINDEX_TOKEN_STANDARD) {
            throw IllegalArgumentException("Reindex support only $SUPPORTED_REINDEX_TOKEN_STANDARD, but you have ${formatToString(tokenStandardMap)}")
        }
        val params = ReindexTokenTaskParams(standards.first(), tokens)
        checkOtherReindexTokenTasks(params)

        val task = Task(
            type = ReindexTokenTaskParams.ADMIN_REINDEX_TOKEN,
            param = params.toParamString(),
            state = fromBlock,
            running = false,
            lastStatus = TaskStatus.NONE
        )
        return saveTask(task)
    }

    private suspend fun checkOtherReindexTokenTasks(params: ReindexTokenTaskParams) {
        taskRepository.findByType(ReindexTokenTaskParams.ADMIN_REINDEX_TOKEN).collect { task ->
            if (task.lastStatus != TaskStatus.COMPLETED) {
                val existedReindexTokens = ReindexTokenTaskParams
                    .fromParamString(task.param)
                    .tokens
                    .filter { it in params.tokens }

                if (existedReindexTokens.isNotEmpty()) {
                    throw IllegalArgumentException("Tokens $existedReindexTokens already reindexing in other task ${task.id}")
                }
            }
        }
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

    private companion object {
        val SUPPORTED_REINDEX_TOKEN_STANDARD: Set<TokenStandard> = setOf(TokenStandard.ERC721, TokenStandard.ERC1155)
    }
}

@Service
class TempTaskRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun save(task: Task): Task {
        return template.save(task).awaitFirst()
    }

    suspend fun findByType(type: String): Flow<Task> {
        val criteria = Task::type isEqualTo type
        return template.find<Task>(Query.query(criteria)).asFlow()
    }
}
