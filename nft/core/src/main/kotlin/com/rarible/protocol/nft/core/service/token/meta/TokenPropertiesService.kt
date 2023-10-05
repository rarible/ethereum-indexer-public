package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.token.meta.descriptors.TokenPropertiesResolver
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.util.concurrent.atomic.AtomicReference

@Component
class TokenPropertiesService(
    resolvers: List<TokenPropertiesResolver>,
    private val tokenRepository: TokenRepository,
) {
    private val resolvers = resolvers.sortedBy(TokenPropertiesResolver::order)

    suspend fun resolve(id: Address): TokenProperties? {
        val lastException = AtomicReference<Exception>()
        val properties = resolvers
            .map {
                try {
                    it.resolve(id)
                } catch (ex: Exception) {
                    logger.error(
                        "Unexpected exception during getting meta for token: $id with ${it::class.java} resolver", ex
                    )
                    lastException.set(ex)
                    null
                }
            }.firstOrNull { it != null && !it.isEmpty() }
            ?: if (lastException.get() != null) {
                throw lastException.get()
            } else {
                return null
            }

        // In some cases there is no name in meta, but we can take it from token
        if (properties.name.isBlank() || properties.name == TokenProperties.EMPTY.name) {
            tokenRepository.findById(id).awaitFirstOrNull()?.let { token ->
                return properties.copy(name = token.name)
            }
        }

        return properties
    }

    companion object {

        val logger: Logger = LoggerFactory.getLogger(TokenPropertiesService::class.java)
        fun logProperties(id: Address, message: String, warn: Boolean = false) = logger.logMetaLoading(
            id.prefixed(), message, warn
        )
    }
}
