package com.rarible.protocol.nft.listener.service.descriptors.erc721

import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.collection.TokenUriRevealEvent
import com.rarible.protocol.contracts.erc721.thirdweb.DropERC721
import com.rarible.protocol.nft.core.model.TokenUriReveal
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.listener.service.resolver.IgnoredTokenResolver
import io.daonomic.rpc.domain.Word
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger

@Component
class TokenUriRevealLogDescriptor(
    ignoredTokenResolver: IgnoredTokenResolver,
    private val sender: MonoTransactionSender
) : LogEventDescriptor<TokenUriReveal> {
    private val skipContracts = ignoredTokenResolver.resolve()
    override val collection: String = NftHistoryRepository.COLLECTION
    override val topic: Word = TokenUriRevealEvent.id()

    override fun convert(
        log: Log,
        transaction: Transaction,
        timestamp: Long,
        index: Int,
        totalLogs: Int
    ): Publisher<TokenUriReveal> {
        if (log.address() in skipContracts) {
            return Mono.empty()
        }
        try {
            val e = TokenUriRevealEvent.apply(log)
            val tokenIdRangeMono = revealTokenRange(log, e)
            return tokenIdRangeMono.map { (fromTokenId, toTokenId) ->
                TokenUriReveal(
                    contract = log.address(),
                    tokenIdFrom = fromTokenId,
                    tokenIdTo = toTokenId,
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to handle reveal event: ${e.message}", e)
        }
        return Mono.empty()
    }

    /**
     * fromItemId: batchId at index - 1 is the itemId of first item in our batch
     * toItemId: our batchId - 1 is last itemId in our batch (and first itemId in next batch)
     * Please refer to LazyMint.sol#lazyMint and BatchMintMetadata.sol for details
     */
    private fun revealTokenRange(log: Log, e: TokenUriRevealEvent): Mono<Pair<BigInteger, BigInteger>> {
        val contract = DropERC721(log.address(), sender)
        val index = e.index()
        // batchId at index - 1 is the tokenId of first item in our batch
        val fromTokenIdMono = if (index > BigInteger.ZERO) {
            contract.getBatchIdAtIndex(index - BigInteger.ONE).call()
        } else {
            Mono.just(BigInteger.ZERO)
        }
        val toTokenIdMono = contract.getBatchIdAtIndex(index).call()
            .map { itemId -> itemId - BigInteger.ONE }
        return fromTokenIdMono.flatMap { fromTokenId ->
            toTokenIdMono.map { toTokenId -> Pair(fromTokenId, toTokenId) }
        }
    }

    override fun getAddresses(): Mono<Collection<Address>> = emptyList<Address>().toMono()

    companion object {
        private val logger = LoggerFactory.getLogger(TokenUriRevealLogDescriptor::class.java)
    }
}
