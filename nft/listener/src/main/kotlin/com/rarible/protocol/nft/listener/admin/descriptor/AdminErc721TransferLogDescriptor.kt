package com.rarible.protocol.nft.listener.admin.descriptor

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.service.token.TokenService
import com.rarible.protocol.nft.listener.service.descriptors.erc721.ERC721TransferLogDescriptor
import com.rarible.protocol.nft.listener.service.item.CustomMintDetector
import com.rarible.protocol.nft.listener.service.resolver.IgnoredTokenResolver
import reactor.core.publisher.Mono
import scalether.domain.Address

class AdminErc721TransferLogDescriptor(
    tokenService: TokenService,
    customMintDetector: CustomMintDetector,
    ignoredTokenResolver: IgnoredTokenResolver,
    private val tokens: List<Address>
) : ERC721TransferLogDescriptor(
    tokenService,
    customMintDetector,
    ignoredTokenResolver,
    NftIndexerProperties(
        kafkaReplicaSet = "",
        blockchain = Blockchain.ETHEREUM,
        metricRootPath = "",
        cryptoPunksContractAddress = Address.ZERO().hex(),
        ensDomainsContractAddress = Address.ZERO().hex(),
        openseaLazyMintAddress = Address.ZERO().hex(),
        royaltyRegistryAddress = Address.ZERO().hex(),
        ipfs = NftIndexerProperties.IpfsProperties("", "")
    )
) {
    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(tokens)
    }
}
