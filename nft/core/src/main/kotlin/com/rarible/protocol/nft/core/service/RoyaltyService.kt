package com.rarible.protocol.nft.core.service

import com.rarible.protocol.contracts.test.royalties.IRoyaltiesProvider
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.Royalty
import com.rarible.protocol.nft.core.repository.RoyaltyRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger

@Service
class RoyaltyService(
    private val sender: MonoTransactionSender,
    private val nftIndexerProperties: NftIndexerProperties,
    private val royaltyRepository: RoyaltyRepository
) {

    suspend fun getRoyalty(address: Address, tokenId: BigInteger): List<Part> {
        var record = royaltyRepository.findByTokenAndId(address, tokenId).awaitFirstOrNull()
        return when {
            record != null -> record.royalty
            else -> {
                val provider = IRoyaltiesProvider(Address.apply(nftIndexerProperties.royaltyRegistryAddress), sender)
                val answer = provider.getRoyalties(address, tokenId).call().awaitSingle()
                val royalty = answer.map { Part(it._1, it._2.intValueExact()) }.toList()
                royaltyRepository.save(
                    Royalty(
                        address = address,
                        tokenId = tokenId,
                        royalty = royalty
                    )
                ).awaitSingle().royalty
            }
        }
    }
}
