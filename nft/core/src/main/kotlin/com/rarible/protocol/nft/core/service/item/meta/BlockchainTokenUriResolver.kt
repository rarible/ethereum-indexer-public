package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.contracts.erc1155.v1.rarible.RaribleToken
import com.rarible.protocol.contracts.erc721.v4.rarible.MintableToken
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender
import java.time.Duration

@Component
class BlockchainTokenUriResolver(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val sender: MonoTransactionSender,
    private val tokenRepository: TokenRepository,
    properties: NftIndexerProperties,
) {

    private val timeout = Duration.ofMillis(properties.requestTimeout)

    suspend fun getCollectionName(itemId: ItemId): String? {
        val token = tokenRepository.findById(itemId.token).awaitFirstOrNull() ?: return null
        @Suppress("ReactiveStreamsUnusedPublisher")
        return when (token.standard) {
            TokenStandard.ERC1155 -> RaribleToken(itemId.token, sender).name()
            TokenStandard.ERC721 -> MintableToken(itemId.token, sender).name()
            else -> Mono.empty()
        }.onErrorResume {
            logMetaLoading(itemId, "failed to get name() from contract: ${it.message}", warn = true)
            Mono.empty()
        }.awaitFirstOrNull()
    }

    suspend fun getCollectionUri(id: Address): String? {
        val token = tokenRepository.findById(id).awaitFirstOrNull() ?: return null
        @Suppress("ReactiveStreamsUnusedPublisher")
        return when (token.standard) {
            TokenStandard.ERC1155 -> RaribleToken(id, sender).contractURI()
            TokenStandard.ERC721 -> MintableToken(id, sender).contractURI()
            else -> Mono.empty()
        }.onErrorResume {
            logMetaLoading(id.prefixed(), "failed to get name() from contract: ${it.message}", warn = true)
            Mono.empty()
        }.awaitFirstOrNull()
    }

    suspend fun getUri(itemId: ItemId): String? {
        val token = tokenRepository.findById(itemId.token).awaitFirstOrNull()
        if (token == null) {
            logMetaLoading(itemId, "token is not found", warn = true)
            return null
        }
        val result = when (token.standard) {
            TokenStandard.ERC1155 -> getErc1155TokenUri(itemId)
            TokenStandard.ERC721 -> getErc721TokenUri(itemId)
            else -> null
        }
        return if (result.isNullOrBlank()) null else result
    }

    private suspend fun getErc1155TokenUri(itemId: ItemId): String? {
        return RaribleToken(itemId.token, sender)
            .uri(itemId.tokenId.value)
            .timeout(timeout)
            .onErrorResume {
                logMetaLoading(itemId, "failed to get 'uri' from contract: ${it.message}", warn = true)
                Mono.empty()
            }.awaitFirstOrNull()
    }

    private suspend fun getErc721TokenUri(itemId: ItemId): String? {
        return MintableToken(itemId.token, sender)
            .tokenURI(itemId.tokenId.value)
            .timeout(timeout)
            .onErrorResume {
                logMetaLoading(itemId, "failed get 'tokenURI' from contract: ${it.message}", warn = true)
                Mono.empty()
            }.awaitFirstOrNull()
    }
}
