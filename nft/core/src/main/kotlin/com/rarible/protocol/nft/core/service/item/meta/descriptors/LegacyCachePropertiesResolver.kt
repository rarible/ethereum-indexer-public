package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.cache.Cache
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.updateFirst
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.*

/**
 * Resolves properties from accumulated caches 'cache_properties' and 'cache_opensea'.
 * This is needed to reuse our huge accumulated meta caches until they are migrated to a new cache table 'metadata'.
 * Upon returning a cached value it will never return it again.
 */
abstract class BaseLegacyCachePropertiesResolver(
    override val name: String,
    private val collectionName: String,
    private val mongo: ReactiveMongoTemplate
) : ItemPropertiesResolver {

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        val cache = mongo.findById<Cache>(itemId.decimalStringValue, collectionName)
            .onErrorResume { Mono.empty() }
            .awaitFirstOrNull()
            ?: return null
        if (cache.updateDate == UNSET_DATE) {
            return null
        }
        runCatching { reset(itemId) }
        return cache.data as? ItemProperties
    }

    override suspend fun reset(itemId: ItemId) {
        mongo.updateFirst<Cache>(
            Query(Cache::id isEqualTo itemId.decimalStringValue),
            Update().set(Cache::updateDate.name, UNSET_DATE),
            collectionName
        ).awaitFirstOrNull()
    }

    companion object {
        val UNSET_DATE: Date = Date(0)
    }
}

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class OpenSeaLegacyCachePropertiesResolver(
    mongo: ReactiveMongoTemplate
) : BaseLegacyCachePropertiesResolver(NAME, "cache_opensea", mongo) {
    companion object {
        const val NAME: String = "Legacy OpenSea cache"
    }
}

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class RaribleLegacyCachePropertiesResolver(
    mongo: ReactiveMongoTemplate
) : BaseLegacyCachePropertiesResolver("Legacy Rarible cache", "cache_properties", mongo) {
    companion object {
        const val NAME: String = "Legacy Rarible cache"
    }
}
