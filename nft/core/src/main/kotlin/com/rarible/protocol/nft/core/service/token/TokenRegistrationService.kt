package com.rarible.protocol.nft.core.service.token

import com.google.common.cache.CacheBuilder
import com.rarible.contracts.erc1155.IERC1155
import com.rarible.contracts.erc165.IERC165
import com.rarible.contracts.erc721.IERC721
import com.rarible.contracts.ownable.Ownable
import com.rarible.core.common.component1
import com.rarible.core.common.component2
import com.rarible.core.common.component3
import com.rarible.core.common.component4
import com.rarible.core.common.component5
import com.rarible.core.common.orNull
import com.rarible.core.logging.LoggingUtils
import com.rarible.protocol.contracts.Signatures
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.model.calculateFunctionId
import com.rarible.protocol.nft.core.repository.TokenRepository
import io.daonomic.rpc.RpcCodeException
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender
import java.util.*

@Service
class TokenRegistrationService(
    private val tokenRepository: TokenRepository,
    private val sender: MonoTransactionSender,
    @Value("\${nft.token.cache.max.size:10000}") private val cacheMaxSize: Long
) {
    private val cache = CacheBuilder.newBuilder()
        .maximumSize(cacheMaxSize)
        .build<Address, TokenStandard>()

    fun getTokenStandard(address: Address): Mono<TokenStandard> {
        cache.getIfPresent(address)?.let { return it.toMono() }
        return register(address)
            .map { it.standard }
            .doOnNext { cache.put(address, it) }
    }

    fun register(address: Address): Mono<Token> = getOrSaveToken(address, ::fetchToken)

    fun getOrSaveToken(address: Address, fetchToken: (Address) -> Mono<Token>): Mono<Token> =
        LoggingUtils.withMarker { marker ->
            tokenRepository.findById(address)
                .switchIfEmpty {
                    logger.info(marker, "Token $address not found. fetching")
                    fetchToken(address).flatMap { saveOrReturn(it) }
                }
        }

    private fun saveOrReturn(token: Token): Mono<Token> {
        return tokenRepository.save(token)
            .onErrorResume {
                if (it is DuplicateKeyException) {
                    Mono.justOrEmpty(token)
                } else {
                    Mono.error(it)
                }
            }
    }

    suspend fun setTokenStandard(tokenId: Address, standard: TokenStandard): Token {
        val token = checkNotNull(tokenRepository.findById(tokenId).awaitFirstOrNull()) {
            "Token $tokenId is not found"
        }
        check(token.standard != standard) { "Token standard is already $standard" }
        val savedToken = tokenRepository.save(token.copy(standard = standard)).awaitFirst()
        cache.put(tokenId, standard)
        return savedToken
    }

    fun updateFeatures(token: Token): Mono<Token> {
        logger.info("updateFeatures ${token.id}")
        return fetchFeatures(token.id)
            .map { token.copy(features = it) }
            .flatMap { tokenRepository.save(it) }
    }

    private fun fetchToken(address: Address): Mono<Token> {
        val nft = IERC721(address, sender)
        val ownable = Ownable(address, sender)
        return Mono.zip(
            nft.name().emptyIfError(),
            nft.symbol().emptyIfError(),
            fetchFeatures(address),
            fetchStandard(address),
            ownable.owner().call().emptyIfError()
        ).map { (name, symbol, features, standard, owner) ->
            Token(
                id = address,
                name = name.orElse(""),
                symbol = symbol.orNull(),
                features = features,
                standard = standard,
                owner = owner.orNull()
            )
        }
    }

    private fun <T> Mono<T>.emptyIfError(): Mono<Optional<T>> {
        return this.map { Optional.ofNullable(it) }
            .onErrorResume { Mono.just(Optional.empty()) }
    }

    private suspend fun fetchTokenStandard(address: Address): TokenStandard {
        logStandard(address, "started fetching")
        if (address in WELL_KNOWN_TOKENS_WITHOUT_ERC165) {
            val standard = WELL_KNOWN_TOKENS_WITHOUT_ERC165.getValue(address)
            logStandard(address, "it is well known token with standard $standard")
            return standard
        }
        val contract = IERC165(address, sender)
        for (standard in TokenStandard.values()) {
            if (standard.interfaceId != null) {
                try {
                    val isSupported = contract.supportsInterface(standard.interfaceId.bytes()).awaitFirst()
                    if (isSupported) {
                        return standard
                    }
                } catch (e: Exception) {
                    if (e is RpcCodeException || e is IllegalArgumentException) {
                        logStandard(address, "unable to call supportsInterface: ${e.message}")
                        // Not supported or does not have 'supportsInterface' declared at all.
                        continue
                    }
                    // Could not determine for sure (probably we failed to connect to the node).
                    throw e
                }
            }
        }
        return fetchTokenStandardByFunctionSignatures(sender, address)
    }

    suspend fun fetchTokenStandardByFunctionSignatures(sender: MonoTransactionSender, address: Address): TokenStandard {
        logStandard(address, "determine standard by presence of function signatures")
        val bytecode = try {
            sender.ethereum()
                .ethGetCode(address, "latest")
                .awaitFirstOrNull()
                ?: return TokenStandard.NONE
        } catch (e: Exception) {
            logStandard(address, "failed to get contract bytecode, returning NONE: ${e.message}")
            return TokenStandard.NONE
        }
        val hexBytecode = bytecode.hex()
        for (standard in TokenStandard.values()) {
            if (standard.functionSignatures.isNotEmpty()) {
                val nonMatchingSignatures = standard.functionSignatures.filterNot { signature ->
                    val functionId = calculateFunctionId(signature)
                    hexBytecode.contains(functionId.hex())
                }
                if (nonMatchingSignatures.isEmpty()) {
                    logStandard(
                        address,
                        "determined as $standard: all function signatures are present in bytecode"
                    )
                    return standard
                } else {
                    logStandard(
                        address,
                        "cannot determine as $standard, non existing signatures: " + nonMatchingSignatures.joinToString()
                    )
                }
            }
        }
        return TokenStandard.NONE
    }

    internal fun fetchStandard(address: Address): Mono<TokenStandard> = mono { fetchTokenStandard(address) }

    private fun fetchFeatures(address: Address): Mono<Set<TokenFeature>> {
        return Mono.zip(
            sender.ethereum().ethGetCode(address, "latest")
                .map { it.toString() }
                .map { code ->
                    FEATURES.entries
                        .filter { code.contains(it.key.hex()) }
                        .map { it.value }
                        .toSet()
                },
            fetchErc165Features(address)
        ).map { (l1, l2) -> l1 + l2 }
    }

    private fun fetchErc165Features(address: Address): Mono<Set<TokenFeature>> {
        return TokenFeature.values().toFlux()
            .flatMap { isErc165FeatureEnabled(address, it) }
            .collectList()
            .map { it.toSet() }
    }

    private fun isErc165FeatureEnabled(address: Address, feature: TokenFeature): Mono<TokenFeature> {
        return feature.erc165
            .toFlux()
            .flatMap { isErc165InterfaceSupported(address, it) }
            .filter { it }
            .collectList()
            .flatMap { if (it.isNotEmpty()) Mono.just(feature) else Mono.empty() }
    }

    private fun isErc165InterfaceSupported(address: Address, ifaceId: Binary): Mono<Boolean> {
        val contract = IERC165(address, sender)
        return contract.supportsInterface(ifaceId.bytes())
            .switchIfEmpty { false.toMono() }
            .onErrorResume { false.toMono() }
    }

    companion object {
        val FEATURES = mapOf(
            IERC721.setApprovalForAllSignature().id() to TokenFeature.APPROVE_FOR_ALL,
            Signatures.setTokenURIPrefixSignature().id() to TokenFeature.SET_URI_PREFIX,
            IERC721.burnSignature().id() to TokenFeature.BURN,
            IERC1155.burnSignature().id() to TokenFeature.BURN
        )
        val WELL_KNOWN_TOKENS_WITHOUT_ERC165 = mapOf<Address, TokenStandard>(
            Address.apply("0xf7a6e15dfd5cdd9ef12711bd757a9b6021abf643") to TokenStandard.ERC721 // CryptoBots (CBT)
        )
        private val logger: Logger = LoggerFactory.getLogger(TokenRegistrationService::class.java)

        private fun logStandard(address: Address, message: String) {
            logger.info("Token standard of $address: $message")
        }
    }
}
