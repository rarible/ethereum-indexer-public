package com.rarible.protocol.erc20.listener.service.descriptors.erc20

import com.rarible.contracts.interfaces.weth9.WithdrawalEvent
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.contract.WithdrawalEventByLogData
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.model.Erc20Withdrawal
import com.rarible.protocol.erc20.listener.configuration.Erc20ListenerProperties
import com.rarible.protocol.erc20.listener.service.descriptors.Erc20LogEventDescriptor
import com.rarible.protocol.erc20.listener.service.token.Erc20RegistrationService
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import java.util.*

@Service
class WithdrawalLogDescriptor(
    private val registrationService: Erc20RegistrationService,
    properties: Erc20ListenerProperties
) : Erc20LogEventDescriptor<Erc20TokenHistory> {

    private val addresses = properties.tokens.map { Address.apply(it) }
    override val topic: Word = WithdrawalEvent.id()

    override suspend fun convert(log: Log, date: Date): List<Erc20TokenHistory> {
        val erc20Token = registrationService.tryRegister(log.address()) ?: return emptyList()

        val event: WithdrawalEvent = when {
            log.topics().size() == 1 -> WithdrawalEventByLogData.apply(log)
            else -> WithdrawalEvent.apply(log)
        }

        val withdrawal = Erc20Withdrawal(
            owner = event.src(),
            token = erc20Token.id,
            value = EthUInt256.of(event.wad()),
            date = date
        )
        return listOf(withdrawal)
    }

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(addresses)
    }
}
