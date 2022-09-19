package com.rarible.protocol.nft.listener.admin.descriptor

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.service.descriptors.erc721.TransferLogDescriptor
import reactor.core.publisher.Mono
import scalether.domain.Address

class AdminErc721TransferLogDescriptor(
    tokenRegistrationService: TokenRegistrationService,
    private val tokens: List<Address>
) : TransferLogDescriptor(
    tokenRegistrationService,
    NftIndexerProperties(
        basePublicApiUrl = "",
        kafkaReplicaSet = "",
        blockchain = Blockchain.ETHEREUM,
        metricRootPath = "",
        cryptoPunksContractAddress = Address.ZERO().hex(),
        ensDomainsContractAddress = Address.ZERO().hex(),
        openseaLazyMintAddress = Address.ZERO().hex(),
        royaltyRegistryAddress = Address.ZERO().hex(),
        ipfs = NftIndexerProperties.IpfsProperties("", "")
    ),
    NftListenerProperties()
) {
    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(tokens)
    }
}