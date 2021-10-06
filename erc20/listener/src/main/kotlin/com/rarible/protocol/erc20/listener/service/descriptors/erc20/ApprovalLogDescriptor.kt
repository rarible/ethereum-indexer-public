package com.rarible.protocol.erc20.listener.service.descriptors.erc20

import com.rarible.contracts.erc20.ApprovalEvent
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.contract.ApprovalEventByLogData
import com.rarible.protocol.erc20.core.model.Erc20TokenApproval
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.repository.Erc20ApprovalHistoryRepository
import com.rarible.protocol.erc20.listener.configuration.Erc20ListenerProperties
import com.rarible.protocol.erc20.listener.service.descriptors.Erc20LogEventDescriptor
import com.rarible.protocol.erc20.listener.service.token.Erc20RegistrationService
import io.daonomic.rpc.domain.Word
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import java.util.*

@Service
class ApprovalLogDescriptor(
    private val registrationService: Erc20RegistrationService,
    properties: Erc20ListenerProperties
) : Erc20LogEventDescriptor<Erc20TokenHistory> {

    private val addresses = properties.tokens.map { Address.apply(it) }

    override val collection: String
        get() = Erc20ApprovalHistoryRepository.COLLECTION

    override val topic: Word = ApprovalEvent.id()

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

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(addresses)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ApprovalLogDescriptor::class.java)
    }
}
