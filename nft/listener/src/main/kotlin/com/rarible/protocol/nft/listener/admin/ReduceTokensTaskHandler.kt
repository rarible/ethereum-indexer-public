package com.rarible.protocol.nft.listener.admin

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.core.service.token.TokenUpdateService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ReduceTokensTaskHandler(
    private val tokenUpdateService: TokenUpdateService,
    private val tokenRepository: TokenRepository
) : TaskHandler<String> {

    override val type: String
        get() = "ADMIN_REDUCE_ALL_TOKENS"

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val params = from?.let { Address.apply(it) }
        return tokenRepository.findAllFrom(params).map { token ->
            tokenUpdateService.update(token.id)
            logger.info("Token ${token.id} was reduced")
            token.id.prefixed()
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(ReduceTokensTaskHandler::class.java)
    }
}
