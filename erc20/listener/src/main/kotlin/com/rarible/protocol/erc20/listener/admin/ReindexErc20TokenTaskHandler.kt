package com.rarible.protocol.erc20.listener.admin

import com.rarible.contracts.erc20.ApprovalEvent
import com.rarible.contracts.erc20.TransferEvent
import com.rarible.contracts.interfaces.weth9.DepositEvent
import com.rarible.contracts.interfaces.weth9.WithdrawalEvent
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskStatus
import com.rarible.ethereum.listener.log.LogListenService
import com.rarible.ethereum.listener.log.ReindexTopicTaskHandler
import com.rarible.protocol.erc20.core.admin.model.ReindexErc20TokenTaskParam
import com.rarible.protocol.erc20.core.admin.repository.Erc20TaskRepository
import com.rarible.protocol.erc20.core.metric.DescriptorMetrics
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.listener.configuration.EnableOnScannerV1
import com.rarible.protocol.erc20.listener.configuration.Erc20ListenerProperties
import com.rarible.protocol.erc20.listener.service.descriptors.Erc20LogEventDescriptor
import com.rarible.protocol.erc20.listener.service.descriptors.erc20.ApprovalLogDescriptor
import com.rarible.protocol.erc20.listener.service.descriptors.erc20.DepositLogDescriptor
import com.rarible.protocol.erc20.listener.service.descriptors.erc20.TransferLogDescriptor
import com.rarible.protocol.erc20.listener.service.descriptors.erc20.WithdrawalLogDescriptor
import com.rarible.protocol.erc20.listener.service.owners.IgnoredOwnersResolver
import com.rarible.protocol.erc20.listener.service.token.Erc20RegistrationService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum

@Component
@EnableOnScannerV1
class ReindexErc20TokenTaskHandler(
    private val taskRepository: Erc20TaskRepository,
    private val logListenService: LogListenService,
    private val registrationService: Erc20RegistrationService,
    private val ethereum: MonoEthereum,
    private val properties: Erc20ListenerProperties,
    private val ignoredOwnersResolver: IgnoredOwnersResolver,
    private val descriptorMetrics: DescriptorMetrics
) : TaskHandler<Long> {

    override val type: String get() = ReindexErc20TokenTaskParam.ADMIN_REINDEX_ERC20_TOKENS

    override suspend fun isAbleToRun(param: String): Boolean {
        return verifyAllReindexingTopicTasksAreCompleted(
            ApprovalEvent.id(),
            DepositEvent.id(),
            TransferEvent.id(),
            WithdrawalEvent.id()
        )
    }

    override fun runLongTask(from: Long?, param: String): Flow<Long> {
        val taskParams = ReindexErc20TokenTaskParam.fromParamString(param)
        return fetchNormalBlockNumber()
            .flatMapMany { to -> reindexTokenItems(taskParams, from, to) }
            .map { it.first }
            .asFlow()
    }

    private fun reindexTokenItems(taskParam: ReindexErc20TokenTaskParam, from: Long?, end: Long): Flux<LongRange> {
        val descriptor = getDescriptor(taskParam)
        return logListenService.reindexWithDescriptor(descriptor, from ?: 1, end)
    }

    private fun getDescriptor(taskParam: ReindexErc20TokenTaskParam): Erc20LogEventDescriptor<Erc20TokenHistory> {
        val props = properties.copy(tokens = taskParam.tokens.map { it.prefixed() })
        return when (taskParam.descriptor) {
            ReindexErc20TokenTaskParam.Descriptor.APPROVAL -> ApprovalLogDescriptor(registrationService, props, ignoredOwnersResolver, descriptorMetrics)
            ReindexErc20TokenTaskParam.Descriptor.DEPOSIT -> DepositLogDescriptor(registrationService, props, ignoredOwnersResolver, descriptorMetrics)
            ReindexErc20TokenTaskParam.Descriptor.TRANSFER -> TransferLogDescriptor(registrationService, props, ignoredOwnersResolver, descriptorMetrics)
            ReindexErc20TokenTaskParam.Descriptor.WITHDRAWAL -> WithdrawalLogDescriptor(registrationService, props, ignoredOwnersResolver, descriptorMetrics)
            else -> throw IllegalArgumentException("Unknown descriptor name ${taskParam.descriptor}")
        }
    }

    private fun fetchNormalBlockNumber(): Mono<Long> {
        return ethereum.ethBlockNumber().map { it.toLong() }
    }

    private suspend fun verifyAllReindexingTopicTasksAreCompleted(vararg topics: Word): Boolean {
        for (topic in topics) {
            val task = taskRepository.findByType(
                type = ReindexTopicTaskHandler.TOPIC,
                param = topic.toString()
            ).firstOrNull()
            if (task?.lastStatus != TaskStatus.COMPLETED) {
                return false
            }
        }
        return true
    }

}
