package com.rarible.protocol.erc20.listener.service.balance

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.erc20.core.admin.Erc20ReduceTaskParam
import com.rarible.protocol.erc20.core.model.BalanceId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class BalanceReduceTaskHandler(
    private val erc20BalanceReduceService: Erc20BalanceReduceService
) : TaskHandler<BalanceId> {

    override val type = Erc20ReduceTaskParam.TASK_TYPE

    override fun runLongTask(from: BalanceId?, param: String): Flow<BalanceId> {
        val reduceParam = Erc20ReduceTaskParam.fromString(param)
        return erc20BalanceReduceService.update(
            token = reduceParam.token,
            owner = reduceParam.owner,
            from = from
        ).map { it.id }
            .windowTimeout(Int.MAX_VALUE, Duration.ofSeconds(5))
            .flatMap {
                it.takeLast(1).next()
            }
            .asFlow()
    }
}
