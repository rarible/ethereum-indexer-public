package com.rarible.protocol.erc20.listener.service.descriptors.erc20

import com.rarible.contracts.interfaces.weth9.DepositEvent
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.contract.DepositEventByLogData
import com.rarible.protocol.erc20.core.model.Erc20Deposit
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.listener.configuration.Erc20ListenerProperties
import com.rarible.protocol.erc20.listener.service.descriptors.Erc20LogEventDescriptor
import com.rarible.protocol.erc20.listener.service.owners.IgnoredOwnersResolver
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
class DepositLogDescriptor(
    private val registrationService: Erc20RegistrationService,
    properties: Erc20ListenerProperties,
    ignoredOwnersResolver: IgnoredOwnersResolver
) : Erc20LogEventDescriptor<Erc20TokenHistory> {

    private val addresses = properties.tokens
        .map { Address.apply(it) }
        .also { logger.info("Tokens to observe: ${it.joinToString()}") }
    private val ignoredOwners = ignoredOwnersResolver.resolve()

    override val topic: Word = DepositEvent.id()

    override suspend fun convert(log: Log, date: Date): List<Erc20TokenHistory> {
        val erc20Token = registrationService.tryRegister(log.address()) ?: return emptyList()

        val event = when {
            log.topics().size() == 1 -> DepositEventByLogData.apply(log)
            log.topics().size() == 2 -> DepositEvent.apply(log)
            else -> {
                logger.warn("Can't parse DepositEvent from $log")
                return emptyList()
            }
        }

        val approval = Erc20Deposit(
            owner = event.dst(),
            token = erc20Token.id,
            value = EthUInt256.of(event.wad()),
            date = date
        )
        return listOf(approval).filter { it.owner !in ignoredOwners }
    }

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(addresses)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(DepositLogDescriptor::class.java)
    }
}
