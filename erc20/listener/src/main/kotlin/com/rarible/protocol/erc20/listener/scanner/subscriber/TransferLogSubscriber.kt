package com.rarible.protocol.erc20.listener.scanner.subscriber

import com.rarible.contracts.interfaces.weth9.TransferEvent
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.contract.TransferEventWithFullData
import com.rarible.protocol.erc20.core.metric.DescriptorMetrics
import com.rarible.protocol.erc20.core.model.Erc20IncomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20OutcomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.model.SubscriberGroups
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import com.rarible.protocol.erc20.listener.configuration.Erc20ListenerProperties
import com.rarible.protocol.erc20.listener.service.Erc20RegistrationService
import com.rarible.protocol.erc20.listener.service.IgnoredOwnersResolver
import io.daonomic.rpc.domain.Binary
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.domain.response.Log
import java.util.Date

@Component
class TransferLogSubscriber(
    private val registrationService: Erc20RegistrationService,
    ignoredOwnersResolver: IgnoredOwnersResolver,
    metrics: DescriptorMetrics,
    commonProps: Erc20ListenerProperties,
) : AbstractBalanceLogEventSubscriber(
    ignoredOwnersResolver = ignoredOwnersResolver,
    metrics = metrics,
    group = SubscriberGroups.ERC20_HISTORY,
    topic = TransferEvent.id(),
    collection = Erc20TransferHistoryRepository.COLLECTION,
    tokens = commonProps.tokens,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(log: Log, date: Date): List<Erc20TokenHistory> {
        val erc20Token = registrationService.tryRegister(log.address()) ?: return emptyList()
        val event = try {
            when {
                // Example of incorrect transfer: https://etherscan.io/tx/0x1447f617cc7d318d506a262e099cfc5cf1a5f12ead82f868acc5da0035801f4d
                log.topics().size() == 1 && log.data() != Binary.empty() -> TransferEventWithFullData.apply(log)
                log.topics().size() == 3 -> com.rarible.contracts.erc20.TransferEvent.apply(log)
                else -> {
                    logger.warn("Can't parse TransferEvent from $log")
                    return emptyList()
                }
            }
        } catch (e: Throwable) {
            logger.error("Failed to parse log: $log", e)
            throw e
        }

        val outcome = if (event.from() != Address.ZERO()) {
            Erc20OutcomeTransfer(
                owner = event.from(),
                token = erc20Token.id,
                value = EthUInt256.of(event.value()),
                date = date
            )
        } else null

        val income = if (event.to() != Address.ZERO()) {
            Erc20IncomeTransfer(
                owner = event.to(),
                token = erc20Token.id,
                value = EthUInt256.of(event.value()),
                date = date
            )
        } else null

        return listOfNotNull(outcome, income)
    }
}
