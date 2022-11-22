package com.rarible.protocol.erc20.listener.admin

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.erc20.core.admin.model.ReduceErc20BalanceTaskParam
import com.rarible.protocol.erc20.core.admin.model.ReindexErc20TokenTaskParam
import com.rarible.protocol.erc20.core.admin.repository.Erc20TaskRepository
import com.rarible.protocol.erc20.listener.configuration.EnableOnScannerV1
import com.rarible.protocol.erc20.listener.service.balance.BalanceReduceState
import com.rarible.protocol.erc20.listener.service.balance.Erc20BalanceReduceServiceV1
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.time.Duration

@Component
@EnableOnScannerV1
class ReduceErc20BalanceTaskHandler(
    private val taskRepository: Erc20TaskRepository,
    @Qualifier("tokenBalanceReduceService")
    private val balanceReduceService: Erc20BalanceReduceServiceV1
) : TaskHandler<BalanceReduceState> {

    override val type: String get() = ReduceErc20BalanceTaskParam.ADMIN_BALANCE_REDUCE

    override fun getAutorunParams(): List<RunTask> = emptyList()

    override suspend fun isAbleToRun(param: String): Boolean {
        val reduceParam = ReduceErc20BalanceTaskParam.fromParamString(param)
        return reduceParam.token !in findTokensBeingIndexedNow()
    }

    override fun runLongTask(from: BalanceReduceState?, param: String): Flow<BalanceReduceState> {
        val current = from ?: BalanceReduceState(Address.apply(param), Address.ZERO())
        return balanceReduceService.update(key = current.toBalanceId(), minMark = Long.MIN_VALUE)
            .map { BalanceReduceState(it.token, it.owner) }
            .windowTimeout(Int.MAX_VALUE, Duration.ofSeconds(5))
            .flatMap {
                it.next()
            }.asFlow()
    }

    private suspend fun findTokensBeingIndexedNow(): List<Address> {
        return taskRepository
            .findByType(ReindexErc20TokenTaskParam.ADMIN_REINDEX_ERC20_TOKENS)
            .filter { it.lastStatus != TaskStatus.COMPLETED }
            .map { ReindexErc20TokenTaskParam.fromParamString(it.param).tokens }
            .toList()
            .flatten()
    }
}