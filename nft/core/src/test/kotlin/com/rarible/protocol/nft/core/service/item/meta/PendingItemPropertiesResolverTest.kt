package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.test.data.randomString
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomItemTransfer
import com.rarible.protocol.nft.core.data.randomItemProperties
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.service.item.meta.descriptors.PendingItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RariblePropertiesResolver
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class PendingItemPropertiesResolverIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var pendingItemTokenUriResolver: PendingItemTokenUriResolver

    val rariblePropertiesResolver: RariblePropertiesResolver = mockk()

    lateinit var pendingItemPropertiesResolver: PendingItemPropertiesResolver

    @BeforeEach
    fun beforeEach() {
        clearMocks(rariblePropertiesResolver)
        pendingItemPropertiesResolver = PendingItemPropertiesResolver(
            rariblePropertiesResolver,
            pendingItemTokenUriResolver,
            itemRepository,
            FeatureFlags()
        )
    }

    @Test
    fun `resolve - ok`() = runBlocking<Unit> {
        val item = createRandomItem().copy(
            supply = EthUInt256.ZERO,
            pending = listOf(createRandomItemTransfer())
        )
        itemRepository.save(item).awaitSingle()

        val tokenUri = randomString()
        val itemProperties = randomItemProperties()

        pendingItemTokenUriResolver.save(item.id, tokenUri)

        coEvery { rariblePropertiesResolver.resolveByTokenUri(item.id, tokenUri) } returns itemProperties

        val result = pendingItemPropertiesResolver.resolve(item.id)

        assertThat(result).isEqualTo(itemProperties)
    }

    @Test
    fun `resolve - supply is not 0`() = runBlocking<Unit> {
        val item = createRandomItem().copy(
            supply = EthUInt256.ONE,
            pending = listOf(createRandomItemTransfer())
        )
        itemRepository.save(item).awaitSingle()

        val result = pendingItemPropertiesResolver.resolve(item.id)

        assertThat(result).isNull()
    }

    @Test
    fun `resolve - no pending logs`() = runBlocking<Unit> {
        val item = createRandomItem().copy(
            supply = EthUInt256.ZERO,
            pending = listOf()
        )
        itemRepository.save(item).awaitSingle()

        val result = pendingItemPropertiesResolver.resolve(item.id)

        assertThat(result).isNull()
    }

    @Test
    fun `resolve - item not found`() = runBlocking<Unit> {
        // Not saved in repo
        val item = createRandomItem()

        val result = pendingItemPropertiesResolver.resolve(item.id)

        assertThat(result).isNull()
    }

    @Test
    fun `resolve - tokenUri not found`() = runBlocking<Unit> {
        val item = createRandomItem().copy(
            supply = EthUInt256.ZERO,
            pending = listOf(createRandomItemTransfer())
        )
        itemRepository.save(item).awaitSingle()

        val result = pendingItemPropertiesResolver.resolve(item.id)

        assertThat(result).isNull()
    }
}