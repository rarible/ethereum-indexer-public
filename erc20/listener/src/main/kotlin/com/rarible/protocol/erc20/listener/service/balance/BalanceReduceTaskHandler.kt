package com.rarible.protocol.erc20.listener.service.balance

import com.rarible.contracts.erc20.TransferEvent
import com.rarible.contracts.interfaces.weth9.DepositEvent
import com.rarible.contracts.interfaces.weth9.WithdrawalEvent
import com.rarible.core.task.RunTask
import com.rarible.core.task.Task
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.ethereum.listener.log.ReindexTopicTaskHandler
import com.rarible.protocol.erc20.core.model.BalanceId
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.time.Duration

@Component
class BalanceReduceTaskHandler(
    private val taskRepository: TaskRepository,
    private val erc20BalanceReduceService: Erc20BalanceReduceService
) : TaskHandler<BalanceReduceState> {

    override val type: String
        get() = BALANCE_REDUCE

    override fun getAutorunParams(): List<RunTask> = listOf(RunTask(""))

    override suspend fun isAbleToRun(param: String): Boolean {
        return verifyAllCompleted(
            TransferEvent.id(),
            DepositEvent.id(),
            WithdrawalEvent.id()
        )
    }

    override fun runLongTask(from: BalanceReduceState?, param: String): Flow<BalanceReduceState> {
        return erc20BalanceReduceService.update(token = null, owner = null, from = from?.toBalanceId())
            .map { it.toState() }
            .windowTimeout(Int.MAX_VALUE, Duration.ofSeconds(5))
            .flatMap {
                it.next()
            }
            .asFlow()
    }

    private suspend fun verifyAllCompleted(vararg topics: Word): Boolean {
        for (topic in topics) {
            val task = findTask(topic)
            if (task?.lastStatus != TaskStatus.COMPLETED) {
                return false
            }
        }
        return true
    }

    private suspend fun findTask(topic: Word): Task? {
        return taskRepository.findByTypeAndParam(ReindexTopicTaskHandler.TOPIC, topic.toString()).awaitFirstOrNull()
    }

    companion object {
        const val BALANCE_REDUCE = "BALANCE_REDUCE"
    }
}

data class BalanceReduceState(
    val token: Address,
    val owner: Address
) {
    fun toBalanceId() = BalanceId(token, owner)
}

private fun BalanceId.toState() =
    BalanceReduceState(token, owner)