package com.rarible.protocol.nft.core.service.token

import com.rarible.contracts.erc1155.IERC1155
import com.rarible.contracts.erc165.IERC165
import com.rarible.contracts.erc721.IERC721
import com.rarible.contracts.ownable.Ownable
import com.rarible.core.apm.withSpan
import com.rarible.core.common.component1
import com.rarible.core.common.component2
import com.rarible.core.common.component3
import com.rarible.core.common.component4
import com.rarible.core.common.component5
import com.rarible.core.common.orNull
import com.rarible.protocol.contracts.Signatures
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.model.calculateFunctionId
import com.rarible.protocol.nft.core.service.token.filter.TokeByteCodeFilter
import io.daonomic.rpc.RpcCodeException
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender
import java.util.*

@Service
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class TokenProvider(
    private val sender: MonoTransactionSender,
    private val tokenByteCodeProvider: TokenByteCodeProvider,
    private val tokeByteCodeFilters: List<TokeByteCodeFilter>
) {
    fun fetchToken(address: Address): Mono<Token> {
        val nft = IERC721(address, sender)
        val ownable = Ownable(address, sender)
        return Mono.zip(
            nft.name().emptyIfError(),
            nft.symbol().emptyIfError(),
            fetchFeatures(address),
            mono { fetchTokenStandard(address) },
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
        }.flatMap { token ->
            detectScam(token)
        }.withSpan(name = "fetchToken", labels = listOf("address" to address.toString()))
    }

    fun detectScam(token: Token): Mono<Token> = mono {
        val code = tokenByteCodeProvider.fetchByteCode(token.id) ?: return@mono token
        val isValidToken = tokeByteCodeFilters.all { it.isValid(code) }
        if (isValidToken) {
            token
        } else {
            token.copy(
                standard = TokenStandard.NONE,
                scam = true
            )
        }
    }

    /**
     * Evaluates token standard by supported interface
     */
    suspend fun fetchTokenStandard(address: Address): TokenStandard {
        logStandard(address, "started fetching")
        if (address in WELL_KNOWN_TOKENS_WITHOUT_ERC165) {
            val standard = WELL_KNOWN_TOKENS_WITHOUT_ERC165.getValue(address)
            logStandard(address, "it is well known token with standard $standard")
            return standard
        }
        val contract = IERC165(address, sender)
        suspend fun checkStandard(standard: TokenStandard): TokenStandard? {
            return try {
                val isSupported = contract.supportsInterface(standard.interfaceId!!.bytes()).awaitFirst()
                if (isSupported) standard else null
            } catch (e: Exception) {
                if (e is RpcCodeException || e is IllegalArgumentException) {
                    logStandard(address, "unable to call supportsInterface: ${e.message}")
                    // Not supported or does not have 'supportsInterface' declared at all.
                    null
                } else {
                    // Could not determine for sure (probably we failed to connect to the node).
                    throw e
                }
            }
        }
        return coroutineScope {
            TokenStandard
                .values()
                .filter { it.interfaceId != null }
                .map { async { checkStandard(it) } }
                .awaitAll()
                .filterNotNull()
                .firstOrNull()
        } ?: fetchTokenStandardBySignature(address)
    }

    /**
     * Evaluates token standard by function signature
     */
    suspend fun fetchTokenStandardBySignature(address: Address): TokenStandard {
        logStandard(address, "determine standard by presence of function signatures")
        val bytecode = getBytecode(address) ?: return TokenStandard.NONE

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

    private fun fetchFeatures(address: Address): Mono<Set<TokenFeature>> {
        return Mono.zip(
            getBytecodeWithMono(address)
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

    private suspend fun getBytecode(address: Address): Binary? {
        return tokenByteCodeProvider.fetchByteCode(address)
    }

    private fun getBytecodeWithMono(address: Address): Mono<Binary> = mono { getBytecode(address) }

    private fun <T : Any> Mono<T>.emptyIfError(): Mono<Optional<T>> {
        return this
            .map { Optional.ofNullable(it) }
            .onErrorResume { Mono.just(Optional.empty()) }
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
        private val logger: Logger = LoggerFactory.getLogger(TokenProvider::class.java)

        private fun logStandard(address: Address, message: String) {
            logger.info("Token standard of $address: $message")
        }
    }
}