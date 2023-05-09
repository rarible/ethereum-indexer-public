package com.rarible.protocol.nft.listener.admin

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.nft.core.service.ReindexTokenService
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import com.rarible.protocol.nft.core.service.token.TokenUpdateService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
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
 * RPN-1316: background job that iterates over all LogEntry-s in the database,
 * tries to deserialize them and logs an error if failed.
 */
@Component
class FixTokenStandardTaskHandler(
    private val mongo: ReactiveMongoOperations,
    private val tokenRegistrationService: TokenRegistrationService,
    private val nftHistoryRepository: NftHistoryRepository,
    private val reindexTokenService: ReindexTokenService,
    private val tokenUpdateService: TokenUpdateService,
    nftListenerProperties: NftListenerProperties
) : TaskHandler<Long> {

    val dry = nftListenerProperties.fixTokenStandard.dry

    override val type: String
        get() = "FIX_TOKEN_STANDARD"

    override fun runLongTask(from: Long?, param: String): Flow<Long> {
        val markerDate = Instant.ofEpochSecond((from ?: if (param.isNotBlank()) param.toLong() else now().epochSecond))
        logger.info("Started finding token with none-standard with date <= $markerDate")
        return findTokensWithNoneStandard(markerDate)
            .map {
                val read = it.dbUpdatedAt!!
                fixToken(it.id)
                read.epochSecond
            }
    }

    fun findTokensWithNoneStandard(from: Instant): Flow<Token> {
        val criteria = (Token::standard isEqualTo TokenStandard.NONE)
            .and(Token::scam).isEqualTo(false)
            .and(Token::dbUpdatedAt).lte(from)
        val query = Query.query(criteria).with(Sort.by(Sort.Direction.DESC, Token::dbUpdatedAt.name))
        return mongo.find(query, Token::class.java, Token.COLLECTION).asFlow()
    }

    suspend fun fixToken(address: Address) {
        val standard = tokenRegistrationService.fetchTokenStandard(address)
        if (standard.isNotIgnorable()) {
            logger.info("Found token with NONE standard but must be $standard for $address")
            if (!dry) {
                tokenRegistrationService.update(address)
                tokenUpdateService.update(address)
                val firstLog = nftHistoryRepository.findAllByCollection(address).awaitFirst()
                reindexTokenService.createReindexTokenItemsTask(listOf(address), firstLog.blockNumber, false)
                reindexTokenService.createReduceTokenItemsTask(address, false)
            }
        } else if (standard == TokenStandard.ERC20) {
            tokenRegistrationService.setTokenStandard(address, standard)
        }
    }

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(FixTokenStandardTaskHandler::class.java)
    }

}
