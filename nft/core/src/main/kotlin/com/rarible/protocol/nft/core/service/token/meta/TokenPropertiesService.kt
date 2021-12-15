package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.cache.CacheService
import com.rarible.core.cache.get
import com.rarible.core.common.nowMillis
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.TOKEN_META_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.token.meta.descriptors.StandardTokenPropertiesResolver
import com.rarible.protocol.nft.core.service.token.meta.descriptors.TokenPropertiesResolver
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.apache.commons.lang3.time.DateUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import scalether.domain.Address
import java.time.Instant

@Component
@CaptureSpan(type = TOKEN_META_CAPTURE_SPAN_TYPE)
class TokenPropertiesService(
    @Value("\${api.properties.cache-timeout}") private val cacheTimeout: Long,
    @Autowired(required = false) private val cacheService: CacheService?,
    private val resolvers: List<TokenPropertiesResolver>
) {

    data class CachedTokenProperties(
        val properties: TokenProperties,
        val fetchAt: Instant
    )

    private val cacheDescriptor = object : CacheDescriptor<CachedTokenProperties> {
        override val collection get() = TOKEN_METADATA_COLLECTION

        override fun get(id: String) = mono {
            val resolveResult = doResolve(Address.apply(id)) ?: return@mono null
            CachedTokenProperties(resolveResult, nowMillis())
        }

        override fun getMaxAge(value: CachedTokenProperties?): Long =
            if (value != null) cacheTimeout else DateUtils.MILLIS_PER_MINUTE * 5
    }

    suspend fun resolve(id: Address): TokenProperties? =
        cacheService.get(
            id.prefixed(),
            cacheDescriptor,
            immediatelyIfCached = false
        ).awaitFirstOrNull()?.properties

    suspend fun reset(id: Address) {
        logger.info("Resetting properties for $id")
        cacheService?.reset(id.prefixed(), cacheDescriptor)
            ?.onErrorResume { Mono.empty() }
            ?.awaitFirstOrNull()
    }

    protected suspend fun doResolve(id: Address): TokenProperties? {
        val item = resolvers
            .sortedBy(TokenPropertiesResolver::order)
            .asFlow()
            .map {
                try {
                    it.resolve(id)
                } catch (ex: Exception) {
                    logger.error("Unexpected exception during getting meta for token: $id with ${it::class.java} resolver", ex)
                    null
                }
            }.firstOrNull { it != null }
        return item
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TokenPropertiesService::class.java)
        fun logProperties(id: Address, message: String, warn: Boolean = false) {
            val logMessage = "Meta of ${id.prefixed()}: $message"
            if (warn) {
                logger.warn(logMessage)
            } else {
                logger.info(logMessage)
            }
        }
        const val TOKEN_METADATA_COLLECTION = "token_metadata"
    }
}
