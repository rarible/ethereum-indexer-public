package com.rarible.protocol.nft.core.service.token

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class TokenService(
    private val tokenRepository: TokenRepository,
    private val tokenProvider: TokenProvider,
    private val tokenEventListener: TokenEventListener,
    private val tokenStandardCache: TokenStandardCache
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getToken(address: Address): Token? {
        return tokenRepository.findById(address).awaitFirstOrNull()
    }

    suspend fun getTokens(address: Collection<Address>): List<Token> {
        return tokenRepository.findByIds(address).toList()
    }

    suspend fun saveToken(token: Token, event: TokenEvent? = null): Token {
        val saved = tokenRepository.save(token).awaitFirst()
        tokenStandardCache.set(token.id, token.standard)
        tokenEventListener.onTokenChanged(token, event)
        return saved
    }

    suspend fun insertToken(token: Token): Token {
        return try {
            saveToken(token)
        } catch (e: DuplicateKeyException) {
            logger.warn("Can't insert token, already exists: {}", token)
            // Token already exists, inserted from somewhere else
            token
        }
    }

    suspend fun getTokenStandard(address: Address): TokenStandard {
        logger.info("Resolving token standard for {}", address)

        tokenStandardCache.get(address)?.let {
            logger.info("Resolved token standard for {} from cache: {}", address, it)
            return it
        }

        val result = register(address).standard
        logger.info("Resolved Token standard for {} from blockchain: {}", address, result)
        return result
    }

    suspend fun removeToken(tokenId: Address) {
        // TODO: we need to send "token deleted" Kafka event.
        tokenRepository.remove(tokenId).awaitFirstOrNull()
    }

    suspend fun register(address: Address): Token = register(address) {
        tokenProvider.fetchToken(address).awaitFirst()
    }

    suspend fun register(address: Address, fetchToken: suspend (Address) -> Token): Token {
        val exist = getToken(address)
        if (exist != null) {
            tokenStandardCache.set(address, exist.standard)
            return exist
        }

        logger.info("Token {} not found, fetching it", address)
        val token = fetchToken(address)

        logger.info("Token {} fetched from blockchain: {}", address, token)
        return insertToken(token)
    }

    suspend fun update(address: Address): Token? = optimisticLock {
        val exists = tokenRepository.findById(address).awaitFirstOrNull() ?: return@optimisticLock null
        val fetched = tokenProvider.fetchToken(address).awaitFirstOrNull()

        if (fetched == null || exists.copy(version = null) == fetched) {
            logger.info("Token {} not changed, update skipped: {}", address, fetched)
            return@optimisticLock exists
        }

        // Such update allows to keep isRaribleContract flag obtained from reducer
        val updatedToken = exists.copy(
            name = fetched.name,
            symbol = fetched.symbol,
            features = fetched.features,
            standard = fetched.standard,
            owner = fetched.owner,
            scam = fetched.scam,
        )

        val result = saveToken(updatedToken)
        logger.info("Token {} has been updated: {}", address, fetched)
        result
    }

    suspend fun updateStandard(address: Address, standard: TokenStandard): Token = optimisticLock {
        val token = checkNotNull(tokenRepository.findById(address).awaitFirstOrNull()) {
            "Token $address is not found"
        }
        if (token.standard == standard) {
            logger.info("Token {} standard not changed, skip this update: standard={}", address, standard)
            return@optimisticLock token
        }

        val result = saveToken(token.copy(standard = standard))
        logger.info("Token {} standard updated from {} to {}", address, token.standard, standard)
        result
    }
}
