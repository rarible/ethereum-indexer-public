package com.rarible.protocol.nft.listener.admin.descriptor

import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import com.rarible.protocol.nft.listener.service.descriptors.erc1155.ERC1155TransferBatchLogDescriptor
import reactor.core.publisher.Mono
import scalether.domain.Address

class AdminErc1155TransferLogDescriptor(
    tokenRegistrationService: TokenRegistrationService,
    private val tokens: List<Address>
) : ERC1155TransferBatchLogDescriptor(tokenRegistrationService) {

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(tokens)
    }
}
