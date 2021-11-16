@file:Suppress("SpellCheckingInspection")

package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.cache.CacheService
import com.rarible.core.common.justOrEmpty
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

@ItemMetaTest
class ItemPropertiesServiceTest : BasePropertiesResolverTest() {
    @Test
    fun `get cached properties`() = runBlocking<Unit> {
        val itemId = ItemId(randomAddress(), EthUInt256(randomBigInt()))
        val resolver = mockk<ItemPropertiesResolver>() {
            coEvery { name } returns "Test"
            coEvery { maxAge } returns null
        }
        val cacheService = createSimpleCacheService<ItemProperties>(ItemPropertiesService.METADATA_COLLECTION)
        val ipfsService = mockk<IpfsService>()
        every { ipfsService.resolveHttpUrl(any()) } answers { firstArg() }
        val service = ItemPropertiesService(
            itemPropertiesResolverProvider = mockk {
                every { orderedResolvers } returns listOf(resolver)
            },
            ipfsService = ipfsService,
            cacheTimeout = 10000,
            cacheService = cacheService
        )
        val itemProperties = randomItemProperties()
        coEvery { resolver.resolve(itemId) } returns itemProperties

        assertThat(service.resolve(itemId)).isEqualTo(itemProperties)
        coVerify(exactly = 1) { cacheService.getCached<ItemProperties>(itemId.decimalStringValue, any(), any()) }
        coVerify(exactly = 1) { resolver.resolve(itemId) }

        assertThat(service.resolve(itemId)).isEqualTo(itemProperties)
        coVerify(exactly = 2) { cacheService.getCached<ItemProperties>(itemId.decimalStringValue, any(), any()) }
        // The result must be taken from the cache and the resolver must not be called again.
        coVerify(exactly = 1) { resolver.resolve(itemId) }
    }

    @Test
    fun `reset cached properties`() = runBlocking<Unit> {
        val itemId = ItemId(randomAddress(), EthUInt256(randomBigInt()))
        val resolver = mockk<ItemPropertiesResolver>() {
            coEvery { name } returns "Test"
            coEvery { maxAge } returns null
        }
        val cacheService = createSimpleCacheService<ItemProperties>(ItemPropertiesService.METADATA_COLLECTION)
        val ipfsService = mockk<IpfsService>()
        every { ipfsService.resolveHttpUrl(any()) } answers { firstArg() }
        val service = ItemPropertiesService(
            itemPropertiesResolverProvider = mockk {
                every { orderedResolvers } returns listOf(resolver)
            },
            ipfsService = ipfsService,
            cacheTimeout = 10000,
            cacheService = cacheService
        )
        val itemProperties = randomItemProperties()
        coEvery { resolver.resolve(itemId) } returns itemProperties

        assertThat(service.resolve(itemId)).isEqualTo(itemProperties)
        coVerify(exactly = 1) { resolver.resolve(itemId) }

        service.resetProperties(itemId)
        coVerify(exactly = 1) { cacheService.reset<ItemProperties>(itemId.decimalStringValue, any()) }

        // Request again.
        assertThat(service.resolve(itemId)).isEqualTo(itemProperties)
        coVerify(exactly = 2) { cacheService.getCached<ItemProperties>(itemId.decimalStringValue, any(), any()) }
        // The result must be resolved again after dropping the cache.
        coVerify(exactly = 2) { resolver.resolve(itemId) }
    }

    @Test
    fun `reset meta for item`() = runBlocking<Unit> {
        val itemId = ItemId(randomAddress(), EthUInt256(randomBigInt()))
        val resolver = mockk<ItemPropertiesResolver>() {
            coEvery { name } returns "Test"
            coEvery { maxAge } returns null
        }
        val cacheService = createSimpleCacheService<ItemProperties>(ItemPropertiesService.METADATA_COLLECTION)
        val ipfsService = mockk<IpfsService>()
        every { ipfsService.resolveHttpUrl(any()) } answers { firstArg() }
        val service = ItemPropertiesService(
            itemPropertiesResolverProvider = mockk {
                every { orderedResolvers } returns listOf(resolver)
            },
            ipfsService = ipfsService,
            cacheTimeout = 10000,
            cacheService = cacheService
        )
        val itemProperties = randomItemProperties()
        coEvery { resolver.resolve(itemId) } returns itemProperties

        assertThat(service.resolve(itemId)).isEqualTo(itemProperties)
        coVerify(exactly = 1) { resolver.resolve(itemId) }

        coJustRun { resolver.reset(itemId) }
        service.resetProperties(itemId)
        coVerify(exactly = 1) { cacheService.reset<ItemProperties>(itemId.decimalStringValue, any()) }
        coVerify(exactly = 1) { resolver.reset(itemId) }
    }

    /**
     * Simple implementation of the [CacheService] that saves cached data in a hash map instead of Mongo.
     */
    @Suppress("SameParameterValue")
    private fun <T : Any> createSimpleCacheService(collectionName: String): CacheService {
        val simpleMap = hashMapOf<String, T>()
        val cacheService = mockk<CacheService>()
        every { cacheService.getCached<T>(any(), any(), any()) } answers {
            val id = firstArg<String>()
            val descriptor = secondArg<CacheDescriptor<T>>()
            check(descriptor.collection == collectionName)
            if (simpleMap.containsKey(id)) {
                return@answers simpleMap[id].justOrEmpty()
            }
            val fetched = descriptor.get(id).block()
            if (fetched != null) {
                simpleMap[id] = fetched
                return@answers fetched.justOrEmpty()
            }
            Mono.empty()
        }
        every { cacheService.reset<T>(any(), any()) } answers {
            val id = firstArg<String>()
            val descriptor = secondArg<CacheDescriptor<T>>()
            check(descriptor.collection == collectionName)
            simpleMap.remove(id)
            Mono.empty()
        }
        return cacheService
    }
}
