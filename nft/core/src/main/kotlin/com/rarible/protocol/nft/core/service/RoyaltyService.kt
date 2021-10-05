package com.rarible.protocol.nft.core.service

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.external.royalties.IRoyaltiesProvider
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.Royalty
import com.rarible.protocol.nft.core.repository.RoyaltyRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender

@Service
class RoyaltyService(
    private val sender: MonoTransactionSender,
    private val nftIndexerProperties: NftIndexerProperties,
    private val royaltyRepository: RoyaltyRepository
) {

    suspend fun getRoyalty(address: Address, tokenId: EthUInt256): List<Part> {
        val cachedRoyalties = royaltyRepository.findByTokenAndId(address, tokenId).awaitFirstOrNull()
        if (cachedRoyalties != null) {
            return cachedRoyalties.royalty
        }
        logger.info("Requesting royalties $address:$tokenId")
        val royalties = try {
            val provider = IRoyaltiesProvider(Address.apply(nftIndexerProperties.royaltyRegistryAddress), sender)
            provider.getRoyalties(address, tokenId.value)
                .call().awaitSingle()
                .map { Part(it._1, it._2.intValueExact()) }.toList()
                .also { logger.info("Got royalties for $address:$tokenId: $it") }
        } catch (e: Exception) {
            logger.error("Failed to request royalties for $address:$tokenId", e)
            emptyList<Part>()
        }
        return royaltyRepository.save(
            Royalty(
                address = address,
                tokenId = tokenId,
                royalty = royalties
            )
        ).awaitSingle().royalty
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RoyaltyService::class.java)
    }
}
