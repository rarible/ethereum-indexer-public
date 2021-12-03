package com.rarible.protocol.nft.listener.admin

import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.core.model.ReduceTokenTaskParams
import com.rarible.protocol.nft.core.model.ReindexTokenTaskParams
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import com.rarible.protocol.nft.core.service.token.TokenUpdateService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import scalether.domain.Address

/**
 * Background job that reduces token (specified by `param`).
 * This can be run only after [ReindexTokenTaskHandler] is finished for this token.
 */
@Component
class ReduceTokenTaskHandler(
    private val taskRepository: TempTaskRepository,
    private val tokenUpdateService: TokenUpdateService
) : TaskHandler<String> {

    override val type: String
        get() = ReduceTokenTaskParams.ADMIN_REDUCE_TOKEN

    override suspend fun isAbleToRun(param: String): Boolean {
        val taskParams = ReduceTokenTaskParams.fromParamString(param)
        val tokensBeingIndexedNow = findTokensBeingIndexedNow()
        return taskParams.oneToken !in tokensBeingIndexedNow
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val params = ReduceTokenTaskParams.fromParamString(param)
        return flow {
            tokenUpdateService.update(params.oneToken)
            emit(params.oneToken.prefixed())
        }
    }

    private suspend fun findTokensBeingIndexedNow(): List<Address> {
        return taskRepository
            .findByType(ReindexTokenTaskParams.ADMIN_REINDEX_TOKEN)
            .filter { it.lastStatus != TaskStatus.COMPLETED }
            .map { ReindexTokenTaskParams.fromParamString(it.param).tokens }
            .toList()
            .flatten()
    }
}
