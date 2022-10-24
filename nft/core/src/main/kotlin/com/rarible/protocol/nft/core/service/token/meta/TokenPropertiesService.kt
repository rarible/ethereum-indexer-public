package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.cache.Cache
import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.cache.CacheService
import com.rarible.core.cache.get
import com.rarible.core.common.nowMillis
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.core.service.item.meta.TOKEN_META_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.token.TokenEventListener
import com.rarible.protocol.nft.core.service.token.meta.descriptors.TokenPropertiesResolver
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import scalether.domain.Address
import java.time.Instant

@Component
@CaptureSpan(type = TOKEN_META_CAPTURE_SPAN_TYPE)
class TokenPropertiesService(
    @Value("\${api.properties.cache-timeout}") private val cacheTimeout: Long,
    @Autowired(required = false) private val cacheService: CacheService?,
    private val resolvers: List<TokenPropertiesResolver>,
    // TODO hack, remove later when meta will be implemented at Union
    private val mongo: ReactiveMongoOperations,
    private val tokenRepository: TokenRepository,
    private val tokenEventListener: TokenEventListener,
    private val ff: FeatureFlags
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
            if (value != null) cacheTimeout else DateUtils.MILLIS_PER_MINUTE * 60
    }

    suspend fun resolve(id: Address): TokenProperties? {
        return if (ff.enableTokenMetaSelfRepair) {
            selfRepairResolve(id)
        } else {
            regularResolve(id)
        }
    }

    // TODO Get rid of this after collection's meta implementation in Union
    private suspend fun selfRepairResolve(id: Address): TokenProperties? = coroutineScope {
        val cachedDeferred = async {
            mongo.findById<Cache>(id, cacheDescriptor.collection).awaitFirstOrNull()
        }

        val result = regularResolve(id)

        val current = cachedDeferred.await()?.data as CachedTokenProperties?
        if (result != null && current?.properties != result) {
            logger.info("Meta of collection [{}] changed, trigger update event", id.prefixed())
            tokenRepository.findById(id).awaitFirstOrNull()?.let {
                tokenEventListener.onTokenChanged(it)
            }
        }

        result
    }

    private suspend fun regularResolve(id: Address): TokenProperties? =
        cacheService.get(
            id.prefixed(),
            cacheDescriptor,
            immediatelyIfCached = false
        ).awaitFirstOrNull()?.properties

    suspend fun reset(id: Address) {
        logger.info("Resetting properties for collection $id")
        cacheService?.reset(id.prefixed(), cacheDescriptor)
            ?.onErrorResume { Mono.empty() }
            ?.awaitFirstOrNull()
    }

    protected suspend fun doResolve(id: Address): TokenProperties? {
        val properties = resolvers
            .sortedBy(TokenPropertiesResolver::order)
            .asFlow()
            .map {
                try {
                    it.resolve(id)
                } catch (ex: Exception) {
                    logger.error(
                        "Unexpected exception during getting meta for token: $id with ${it::class.java} resolver", ex
                    )
                    null
                }
            }.firstOrNull { it != null && !it.isEmpty() } ?: return null

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

        const val TOKEN_METADATA_COLLECTION = "token_metadata"
    }
}
