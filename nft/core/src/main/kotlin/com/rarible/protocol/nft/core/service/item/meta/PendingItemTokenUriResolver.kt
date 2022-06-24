package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.common.nowMillis
import com.rarible.protocol.nft.core.model.ItemId
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.stereotype.Component

@Component
class PendingItemTokenUriResolver(
    private val mongo: ReactiveMongoOperations
) {

    suspend fun save(itemId: ItemId, tokenUri: String): PendingItemTokenUri {
        val entity = PendingItemTokenUri(itemId.decimalStringValue, tokenUri, nowMillis())
        return mongo.save(entity).awaitFirst()
    }

    suspend fun get(itemId: ItemId): String? {
        return mongo.findById(itemId.decimalStringValue, PendingItemTokenUri::class.java)
            .awaitSingleOrNull()?.tokenUri
    }
}