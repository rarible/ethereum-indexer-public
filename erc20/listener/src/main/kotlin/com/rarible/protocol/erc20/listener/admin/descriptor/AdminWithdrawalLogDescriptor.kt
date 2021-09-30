package com.rarible.protocol.erc20.listener.admin.descriptor

import com.rarible.protocol.erc20.listener.configuration.Erc20ListenerProperties
import com.rarible.protocol.erc20.listener.service.descriptors.erc20.WithdrawalLogDescriptor
import com.rarible.protocol.erc20.listener.service.token.Erc20RegistrationService
import reactor.core.publisher.Mono
import scalether.domain.Address

class AdminWithdrawalLogDescriptor(
    registrationService: Erc20RegistrationService,
    properties: Erc20ListenerProperties,
    private val address: Address
) : WithdrawalLogDescriptor(registrationService, properties) {

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(listOf(address))
    }
}
