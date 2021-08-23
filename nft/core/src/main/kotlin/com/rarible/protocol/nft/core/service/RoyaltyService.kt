package com.rarible.protocol.nft.core.service

import com.rarible.protocol.contracts.test.royalties.IRoyaltiesProvider
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.Royalty
import com.rarible.protocol.nft.core.repository.RoyaltyRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger

@Service
class RoyaltyService(
    private val sender: MonoTransactionSender,
    private val nftIndexerProperties: NftIndexerProperties,
    private val royaltyRepository: RoyaltyRepository
) {

    fun getRoyalty(address: Address, tokenId: BigInteger): Mono<List<Part>> {
        return royaltyRepository.findByTokenAndId(address, tokenId).map { it.royalty }
            .switchIfEmpty {
                val provider = IRoyaltiesProvider(Address.apply(nftIndexerProperties.royaltyRegistryAddress), sender)
                provider.getRoyalties(address, tokenId).call()
                    .map { royalty -> royalty.map { Part(it._1, it._2.intValueExact()) }.toList() }
                    .flatMap { parts ->
                        royaltyRepository.save(
                            Royalty(
                                address = address,
                                tokenId = tokenId,
                                royalty = parts
                            )
                        )
                    }
                    .flatMap { Mono.just(it.royalty) }
            }
    }
}
