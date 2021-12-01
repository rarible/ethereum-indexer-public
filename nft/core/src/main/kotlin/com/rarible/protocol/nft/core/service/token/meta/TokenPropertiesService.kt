package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.cache.CacheService
import com.rarible.core.cache.get
import com.rarible.core.common.nowMillis
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.service.token.meta.descriptors.TokenPropertiesDescriptor
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.yield
import org.apache.commons.lang3.time.DateUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.time.Instant

@Component
class TokenPropertiesService(
    @Value("\${api.properties.cache-timeout}") private val cacheTimeout: Long,
    @Autowired(required = false) private val cacheService: CacheService?,
    private val descriptors: List<TokenPropertiesDescriptor>
) {

    private data class CachedTokenProperties(
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
            id.hex(),
            cacheDescriptor,
            immediatelyIfCached = false
        ).awaitFirstOrNull()?.properties

    private suspend fun doResolve(id: Address): TokenProperties? {
        val item = descriptors
            .sortedBy(TokenPropertiesDescriptor::order)
            .asFlow()
            .map {
                it.resolve(id)
            }.firstOrNull { it != null }
        return item
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TokenPropertiesService::class.java)
        const val TOKEN_METADATA_COLLECTION = "token_metadata"
    }
}
