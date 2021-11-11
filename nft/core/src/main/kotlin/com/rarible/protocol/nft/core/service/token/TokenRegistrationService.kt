package com.rarible.protocol.nft.core.service.token

import com.rarible.contracts.erc1155.IERC1155
import com.rarible.contracts.erc165.IERC165
import com.rarible.contracts.erc721.IERC721
import com.rarible.contracts.ownable.Ownable
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.common.*
import com.rarible.core.logging.LoggingUtils
import com.rarible.protocol.contracts.Signatures
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.span.SpanType
import io.daonomic.rpc.RpcCodeException
import io.daonomic.rpc.domain.Binary
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender
import java.util.*

@Service
@CaptureSpan(type = SpanType.SERVICE, subtype = "token-registration")
class TokenRegistrationService(
    private val tokenRepository: TokenRepository,
    private val sender: MonoTransactionSender
) {
    private val map: MutableMap<Address, TokenStandard> = mutableMapOf()

    fun getTokenStandard(address: Address): Mono<TokenStandard> {
        val result = map[address]
        return if (result != null) {
            Mono.just(result)
        } else {
            register(address)
                .map { it.standard }
                .doOnNext { map[address] = it }
        }
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

    internal fun fetchStandard(address: Address): Mono<TokenStandard> {
        val contract = IERC721(address, sender)
        return Flux.fromIterable(TokenStandard.values().filter { it.interfaceId != null })
            .flatMap { checkStandard ->
                contract.supportsInterface(checkStandard.interfaceId!!.bytes())
                    .onErrorResume { error ->
                        if (WELL_KNOWN_TOKENS_WITHOUT_ERC165[address] == checkStandard) {
                            Mono.just(true)
                        } else if (error is RpcCodeException || error is IllegalArgumentException) {
                            Mono.just(false)
                        } else {
                            Mono.error(error)
                        }
                    }
                    .map { supported ->
                        checkStandard to supported
                    }
            }
            .collectList()
            .map { all ->
                all.sortedBy { it.first.ordinal }
                    .firstOrNull { it.second }
                    ?.first
                    ?: TokenStandard.NONE
            }
    }

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
        val logger: Logger = LoggerFactory.getLogger(TokenRegistrationService::class.java)
    }
}
