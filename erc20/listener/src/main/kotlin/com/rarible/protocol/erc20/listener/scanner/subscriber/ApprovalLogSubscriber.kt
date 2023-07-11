package com.rarible.protocol.erc20.listener.scanner.subscriber

import com.rarible.contracts.erc20.ApprovalEvent
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.contract.ApprovalEventByLogData
import com.rarible.protocol.erc20.core.metric.DescriptorMetrics
import com.rarible.protocol.erc20.core.model.Erc20TokenApproval
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.model.SubscriberGroups
import com.rarible.protocol.erc20.core.repository.Erc20ApprovalHistoryRepository
import com.rarible.protocol.erc20.listener.configuration.Erc20ListenerProperties
import com.rarible.protocol.erc20.listener.service.Erc20RegistrationService
import com.rarible.protocol.erc20.listener.service.IgnoredOwnersResolver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.response.Log
import java.util.Date

@Component
class ApprovalLogSubscriber(
    ignoredOwnersResolver: IgnoredOwnersResolver,
    metrics: DescriptorMetrics,
    private val registrationService: Erc20RegistrationService,
    commonProps: Erc20ListenerProperties
) : AbstractBalanceLogEventSubscriber(
    ignoredOwnersResolver = ignoredOwnersResolver,
    metrics = metrics,
    group = SubscriberGroups.ERC20_HISTORY,
    topic = ApprovalEvent.id(),
    collection = Erc20ApprovalHistoryRepository.COLLECTION,
    tokens = commonProps.tokens,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(log: Log, date: Date): List<Erc20TokenHistory> {
        val erc20Token = registrationService.tryRegister(log.address()) ?: return emptyList()

        val event: ApprovalEvent = when {
            log.topics().size() == 1 -> ApprovalEventByLogData.apply(log)
            log.topics().size() == 3 -> ApprovalEvent.apply(log)
            else -> {
                logger.warn("Can't parse ApprovalEvent from $log")
                return emptyList()
            }
        }

        val approval = Erc20TokenApproval(
            owner = event.owner(),
            spender = event.spender(),
            token = erc20Token.id,
            value = EthUInt256.of(event.value()),
            date = date
        )
        return listOf(approval)
    }
}