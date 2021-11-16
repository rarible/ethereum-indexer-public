package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.cache.Cache
import com.rarible.core.test.containers.MongodbReactiveBaseTest
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.meta.descriptors.BaseLegacyCachePropertiesResolver
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.findById
import java.util.*

@ItemMetaTest
@MongoTest
@MongoCleanup
class LegacyCachePropertiesResolverTest : MongodbReactiveBaseTest() {

    private val mongoTemplate = createReactiveMongoTemplate()
    private val collectionName = "testCollection"
    private val legacyCachePropertiesResolver = object : BaseLegacyCachePropertiesResolver(
        "Legacy Test cache",
        collectionName,
        mongoTemplate
    ) {}

    @Test
    fun `get from cache and reset the date`() = runBlocking<Unit> {
        val token = randomAddress()
        val tokenId = EthUInt256(randomBigInt())
        val itemProperties = randomItemProperties()
        val itemId = ItemId(token, tokenId)

        mongoTemplate.save(
            Cache(
                id = itemId.decimalStringValue,
                data = itemProperties,
                updateDate = Date()
            ),
            collectionName
        ).awaitFirst()
        val properties = legacyCachePropertiesResolver.resolve(itemId)
        assertThat(properties).isEqualTo(itemProperties)

        val updatedCache = mongoTemplate.findById<Cache>(itemId.decimalStringValue, collectionName).awaitFirstOrNull()
        assertThat(updatedCache?.updateDate).isEqualTo(BaseLegacyCachePropertiesResolver.UNSET_DATE)
    }

    @Test
    fun `reset cache`() = runBlocking<Unit> {
        val token = randomAddress()
        val tokenId = EthUInt256(randomBigInt())
        val itemProperties = randomItemProperties()
        val itemId = ItemId(token, tokenId)

        mongoTemplate.save(
            Cache(
                id = itemId.decimalStringValue,
                data = itemProperties,
                updateDate = Date()
            ),
            collectionName
        ).awaitFirst()

        legacyCachePropertiesResolver.reset(itemId)

        val resetCache = mongoTemplate.findById<Cache>(itemId.decimalStringValue, collectionName).awaitFirstOrNull()
        assertThat(resetCache?.updateDate).isEqualTo(BaseLegacyCachePropertiesResolver.UNSET_DATE)
    }

    @Test
    fun `empty cache`() = runBlocking<Unit> {
        val token = randomAddress()
        val tokenId = EthUInt256(randomBigInt())
        val itemId = ItemId(token, tokenId)
        val properties = legacyCachePropertiesResolver.resolve(itemId)
        assertThat(properties).isNull()
    }

}
