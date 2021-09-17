package com.rarible.protocol.nft.listener.admin.descriptor

import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import com.rarible.protocol.nft.listener.service.descriptors.erc721.TransferLogDescriptor
import reactor.core.publisher.Mono
import scalether.domain.Address

class AdminErc721TransferLogDescriptor(
    tokenRegistrationService: TokenRegistrationService,
    private val tokens: List<Address>
) : TransferLogDescriptor(tokenRegistrationService) {

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(tokens)
    }
}
