package com.rarible.protocol.nft.listener.admin

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.service.ReindexTokenService
import com.rarible.protocol.nft.core.service.token.TokenProvider
import com.rarible.protocol.nft.core.service.token.TokenReduceService
import com.rarible.protocol.nft.core.service.token.TokenService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.time.Instant
import java.time.Instant.now

/**
 * PT-1723: background job that iterates over all collections in the database,
 * tries to set standard if it was missed.
 */
@Component
class FixTokenStandardTaskHandler(
    private val mongo: ReactiveMongoOperations,
    private val tokenProvider: TokenProvider,
    private val nftHistoryRepository: NftHistoryRepository,
    private val reindexTokenService: ReindexTokenService,
    private val tokenService: TokenService,
    private val tokenReduceService: TokenReduceService
) : TaskHandler<Long> {

    override val type: String
        get() = "FIX_TOKEN_STANDARD"

    override fun runLongTask(from: Long?, param: String): Flow<Long> {
        val parsed = FixTokenStandardParam.fromParamString(param)
        val markerDate = Instant.ofEpochSecond(from ?: parsed.mark)
        logger.info("Started finding token with none-standard with date <= $markerDate")
        return findTokensWithNoneStandard(markerDate)
            .map {
                val read = it.dbUpdatedAt!!
                fixToken(it.id, parsed.dry)
                read.epochSecond
            }
    }

    fun findTokensWithNoneStandard(from: Instant): Flow<Token> {
        val criteria = (Token::standard isEqualTo TokenStandard.NONE)
            .and(Token::dbUpdatedAt).lte(from)
        val query = Query.query(criteria).with(Sort.by(Sort.Direction.DESC, Token::dbUpdatedAt.name))
        return mongo.find(query, Token::class.java, Token.COLLECTION).asFlow()
    }

    suspend fun fixToken(address: Address, dry: Boolean) {
        val standard = tokenProvider.fetchTokenStandard(address)
        if (standard.isNotIgnorable()) {
            logger.info("Found token with NONE standard but must be $standard for $address")
            if (!dry) {
                // TODO 2 events will be emitted (not critical)
                tokenReduceService.reduce(address)
                tokenService.update(address)
                val firstLog = nftHistoryRepository.findAllByCollection(address).awaitFirst()
                reindexTokenService.createReindexTokenItemsTask(listOf(address), firstLog.blockNumber, false)
                reindexTokenService.createReduceTokenItemsTask(address, false)
            }
        } else if (standard == TokenStandard.ERC20) {
            tokenService.updateStandard(address, standard)
        }
    }

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(FixTokenStandardTaskHandler::class.java)
    }

    data class FixTokenStandardParam(
        val dry: Boolean,
        val mark: Long
    ) {

        override fun toString(): String {
            return "$dry:$mark"
        }

        companion object {
            fun fromParamString(param: String): FixTokenStandardParam {
                return if (param.contains(":")) {
                    val parts = param.split(":")
                    val mark = if (parts.last().isNotBlank()) parts.last().toLong() else now().epochSecond
                    FixTokenStandardParam(parts.first().toBoolean(), mark)
                } else FixTokenStandardParam(param.toBoolean(), now().epochSecond)
            }
        }
    }
}
