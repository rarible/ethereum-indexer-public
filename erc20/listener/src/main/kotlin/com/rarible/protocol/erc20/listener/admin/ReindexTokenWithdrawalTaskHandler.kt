package com.rarible.protocol.erc20.listener.admin

import com.rarible.contracts.interfaces.weth9.WithdrawalEvent
import com.rarible.core.task.Task
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.ethereum.listener.log.LogListenService
import com.rarible.ethereum.listener.log.ReindexTopicTaskHandler
import com.rarible.protocol.erc20.core.model.ReindexTokenWithdrawalTaskParams
import com.rarible.protocol.erc20.listener.admin.descriptor.AdminWithdrawalLogDescriptor
import com.rarible.protocol.erc20.listener.configuration.Erc20ListenerProperties
import com.rarible.protocol.erc20.listener.service.token.Erc20RegistrationService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum

@Component
@ExperimentalCoroutinesApi
class ReindexTokenWithdrawalTaskHandler(
    private val registrationService: Erc20RegistrationService,
    private val properties: Erc20ListenerProperties,
    private val taskRepository: TaskRepository,
    private val logListenService: LogListenService,
    private val ethereum: MonoEthereum
) : TaskHandler<Long> {

    override val type: String
        get() = ReindexTokenWithdrawalTaskParams.ADMIN_REINDEX_TOKEN_WITHDRAWAL

    override suspend fun isAbleToRun(param: String): Boolean {
        return verifyAllCompleted(
            WithdrawalEvent.id()
        )
    }

    override fun runLongTask(from: Long?, param: String): Flow<Long> {
        val taskParam = ReindexTokenWithdrawalTaskParams.fromParamString(param)

        return fetchNormalBlockNumber()
            .flatMapMany { to -> reindexToken(taskParam, from, to) }
            .map { it.first }
            .asFlow()
    }

    private fun reindexToken(params: ReindexTokenWithdrawalTaskParams, from: Long?, end: Long): Flux<LongRange> {
        val descriptor = AdminWithdrawalLogDescriptor(registrationService, properties, params.token)
        return logListenService.reindexWithDescriptor(descriptor, from ?: 1, end)
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

    private fun fetchNormalBlockNumber(): Mono<Long> {
        return ethereum.ethBlockNumber().map { it.toLong() }
    }

    private suspend fun findTask(topic: Word): Task? {
        return taskRepository.findByTypeAndParam(ReindexTopicTaskHandler.TOPIC, topic.toString()).awaitFirstOrNull()
    }
}
