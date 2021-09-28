package com.rarible.protocol.nft.migration.service

import com.rarible.core.cache.Cache
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.descriptors.OpenSeaCacheDescriptor
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import com.rarible.protocol.nft.migration.mongock.mongo.ChangeLog00015WipeUnknownItemNames
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import java.util.*

@IntegrationTest
class WipeUnknownItemNamesMigrationTest : AbstractIntegrationTest() {

    private val wipeCache = ChangeLog00015WipeUnknownItemNames()

    @Autowired
    private lateinit var opensea: OpenSeaCacheDescriptor

    @Test
    fun `should remove some cache`() = runBlocking {
        mongo.insert(cache("Regular name"), opensea.collection).awaitSingle()
        mongo.insert(cache("Unknown"), opensea.collection).awaitSingle()
        mongo.insert(cache(""), opensea.collection).awaitSingle()
        mongo.insert(cache("Untitled"), opensea.collection).awaitSingle()
        assertEquals(4, mongo.count(Query(), opensea.collection).awaitSingle())

        wipeCache.run(mongo)
        assertEquals(1, mongo.count(Query(), opensea.collection).awaitSingle())
    }

    fun cache(name: String): Cache {
        return Cache(
            UUID.randomUUID().toString(),
            ItemProperties(name, "", "", "", "", "", listOf()), Date()
        )
    }
}
