package com.rarible.protocol.nft.core.service.token

import com.rarible.core.common.retryOptimisticLock
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus.CONFIRMED
import com.rarible.ethereum.listener.log.domain.LogEventStatus.PENDING
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address

@Service
class TokenReduceService(
    private val tokenRepository: TokenRepository,
    private val tokenHistoryRepository: NftHistoryRepository
) {
    fun onTokenHistories(logs: List<LogEvent>): Mono<Void> {
        if (logs.isNotEmpty()) {
            logger.info("onProcessTokenHistory ${logs.size} logs")
        }
        return logs.toFlux()
            .map { it.data as CreateCollection }
            .map { it.id }
            .distinct()
            .concatMap { update(it) }
            .then()
    }

    fun update(address: Address?): Flux<Token> {
        return tokenHistoryRepository.find(address)
            .windowUntilChanged { it.second.id }
            .concatMap { updateOneToken(it) }
    }

    private fun updateOneToken(logs: Flux<Pair<LogEvent, CreateCollection>>): Mono<Token> {
        return logs.reduce(Token.empty()) { token, (log, data) -> reduce(token, log, data) }
            .flatMap {
                logger.info("reduce result: $it")
                if (it.owner != null) {
                    insertOrUpdate(it)
                } else {
                    Mono.empty()
                }
            }
    }

    private fun reduce(token: Token, log: LogEvent, data: CreateCollection): Token {
        val (standard, features) = TokenStandard.CREATE_TOPIC_MAP.getValue(log.topic)
        val status = when (log.status) {
            PENDING -> ContractStatus.PENDING
            CONFIRMED -> ContractStatus.CONFIRMED
            else -> ContractStatus.ERROR
        }
        return token.copy(
            id = data.id,
            owner = data.owner,
            name = data.name,
            symbol = data.symbol,
            features = features,
            standard = standard,
            status = if (status > token.status) status else token.status
        )
    }

    private fun insertOrUpdate(token: Token): Mono<Token> {
        return tokenRepository.findById(token.id)
            .flatMap {
                logger.info("found token with id ${token.id}. updating")
                tokenRepository.save(it.copy(status = token.status))
            }
            .switchIfEmpty {
                logger.info("not found token with id ${token.id}. inserting")
                tokenRepository.save(token)
            }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TokenReduceService::class.java)
    }
}
