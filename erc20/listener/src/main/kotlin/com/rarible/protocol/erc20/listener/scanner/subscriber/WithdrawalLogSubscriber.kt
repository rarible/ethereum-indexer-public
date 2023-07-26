package com.rarible.protocol.erc20.listener.scanner.subscriber

import com.rarible.contracts.interfaces.weth9.WithdrawalEvent
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.contract.WithdrawalEventByLogData
import com.rarible.protocol.erc20.core.metric.DescriptorMetrics
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.model.Erc20Withdrawal
import com.rarible.protocol.erc20.core.model.SubscriberGroups
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import com.rarible.protocol.erc20.listener.configuration.Erc20ListenerProperties
import com.rarible.protocol.erc20.listener.service.Erc20RegistrationService
import com.rarible.protocol.erc20.listener.service.IgnoredOwnersResolver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.response.Log
import java.util.Date

@Component
class WithdrawalLogSubscriber(
    private val registrationService: Erc20RegistrationService,
    ignoredOwnersResolver: IgnoredOwnersResolver,
    metrics: DescriptorMetrics,
    commonProps: Erc20ListenerProperties,
) : AbstractBalanceLogEventSubscriber(
    ignoredOwnersResolver = ignoredOwnersResolver,
    metrics = metrics,
    group = SubscriberGroups.ERC20_HISTORY,
    topic = WithdrawalEvent.id(),
    collection = Erc20TransferHistoryRepository.COLLECTION,
    tokens = commonProps.tokens,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(log: Log, date: Date): List<Erc20TokenHistory> {
        val erc20Token = registrationService.tryRegister(log.address()) ?: return emptyList()

        val event: WithdrawalEvent = when {
            log.topics().size() == 1 -> WithdrawalEventByLogData.apply(log)
            log.topics().size() == 2 -> WithdrawalEvent.apply(log)
            else -> {
                logger.warn("Can't parse WithdrawalEvent from $log")
                return emptyList()
            }
        }

        val withdrawal = Erc20Withdrawal(
            owner = event.src(),
            token = erc20Token.id,
            value = EthUInt256.of(event.wad()),
            date = date
        )
        return listOf(withdrawal)
    }
}
