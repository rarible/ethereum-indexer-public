package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.InconsistentItem
import com.rarible.protocol.nft.core.model.InconsistentItemStatus
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.repository.InconsistentItemRepository
import com.rarible.protocol.nft.core.repository.JobStateRepository
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.metrics.NftListenerMetricsFactory
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import com.rarible.protocol.nft.listener.test.data.OWNERS_NUMBER
import com.rarible.protocol.nft.listener.test.data.createInvalidValidOwnerships
import com.rarible.protocol.nft.listener.test.data.createRandomItem
import com.rarible.protocol.nft.listener.test.data.createRandomOwnership
import com.rarible.protocol.nft.listener.test.data.createValidLog
import com.rarible.protocol.nft.listener.test.data.createValidOwnerships
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import java.time.Duration

@IntegrationTest
class ItemOwnershipConsistencyJobHandlerIt : AbstractIntegrationTest() {

    private lateinit var handler: ItemOwnershipConsistencyJobHandler

    @BeforeEach
    fun setupDbIndexes() = runBlocking {
        itemRepository.createIndexes()
        nftItemHistoryRepository.createIndexes()
    }

    @Autowired
    private lateinit var inconsistentItemRepository: InconsistentItemRepository

    @Autowired
    private lateinit var jobStateRepository: JobStateRepository

    @Autowired
    private lateinit var itemOwnershipConsistencyService: ItemOwnershipConsistencyService

    @Autowired
    private lateinit var metricsFactory: NftListenerMetricsFactory

    @Test
    fun `should save invalid items that can't be fixed to corresponding repo`() = runBlocking<Unit> {
        val now = nowMillis()

        val checkedCounter = metricsFactory.itemOwnershipConsistencyJobCheckedCounter
        val fixedCounter = metricsFactory.itemOwnershipConsistencyJobFixedCounter
        val unfixedCounter = metricsFactory.itemOwnershipConsistencyJobUnfixedCounter
        val delayGauge = metricsFactory.itemOwnershipConsistencyJobDelayGauge

        handler = ItemOwnershipConsistencyJobHandler(
            jobStateRepository,
            itemRepository,
            NftListenerProperties().copy(elementsFetchJobSize = 2),
            itemOwnershipConsistencyService,
            inconsistentItemRepository,
            metricsFactory,
        )

        val supply = EthUInt256.of(OWNERS_NUMBER)
        val tooFreshItem = createRandomItem().copy(supply = supply, date = now)
        val validItem = createRandomItem().copy(supply = supply, date = now - Duration.ofMinutes(10))
        val fixableItem = createRandomItem().copy(supply = supply, date = now - Duration.ofMinutes(20))
        val invalidItem = createRandomItem().copy(supply = supply, date = now - Duration.ofMinutes(30))

        listOf(
            tooFreshItem,
            validItem,
            fixableItem,
            invalidItem
        ).forEach { itemRepository.save(it).awaitFirst() }

        val fixableOwnership = createInvalidValidOwnerships(fixableItem)
        val invalidOwnership = createInvalidValidOwnerships(invalidItem)

        listOf(
            createValidOwnerships(validItem),
            fixableOwnership,
            invalidOwnership
        ).flatten().forEach { ownershipRepository.save(it).awaitFirst() }

        listOf(
            createValidLog(fixableItem, fixableOwnership)
        ).flatten().forEach { nftItemHistoryRepository.save(it).awaitFirst() }

        val delayBefore = delayGauge.set(-1)
        val checkedBefore = checkedCounter.count()
        val fixedBefore = fixedCounter.count()
        val unfixedBefore = unfixedCounter.count()

        // when
        handler.handle()

        // then
        val invalidItems = inconsistentItemRepository.findAll().toList()
        assertThat(invalidItems).hasSize(1)
        assertThat(invalidItems.single().id).isEqualTo(invalidItem.id)

        assertThat(delayGauge.get()).isNotEqualTo(delayBefore)
        assertThat(checkedCounter.count()).isEqualTo(checkedBefore + 3)
        assertThat(fixedCounter.count()).isEqualTo(fixedBefore + 1)
        assertThat(unfixedCounter.count()).isEqualTo(unfixedBefore + 1)
    }

    @Test
    internal fun `should fix item`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.ONE, date = nowMillis() - Duration.ofMinutes(10))
        itemRepository.save(item).awaitFirst()
        val ownerships = listOf(
            createRandomOwnership().copy(token = item.token, tokenId = item.tokenId, value = EthUInt256.TEN),
        )
        ownerships.forEach { ownershipRepository.save(it).awaitFirst() }
        createValidLog(item, listOf(ownerships.first())).forEach {
            nftItemHistoryRepository.save(it).awaitFirst()
        }

        handler = ItemOwnershipConsistencyJobHandler(
            jobStateRepository,
            itemRepository,
            NftListenerProperties().copy(elementsFetchJobSize = 2),
            itemOwnershipConsistencyService,
            inconsistentItemRepository,
            metricsFactory,
        )

        handler.handle()
        val result = itemOwnershipConsistencyService.checkItem(item)

        val owners = ownershipRepository.search(
            Query(
                where(Ownership::token).isEqualTo(item.token).and(Ownership::tokenId).isEqualTo(item.tokenId)
            )
        )
        assertThat(owners).hasSize(1)
        assertThat(result).isEqualTo(ItemOwnershipConsistencyService.CheckResult.Success)
    }

    @Test
    internal fun `should not fix item if it is already in inconsistent_items col`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.ONE, date = nowMillis() - Duration.ofMinutes(10))
        itemRepository.save(item).awaitFirst()
        val ownerships = listOf(
            createRandomOwnership().copy(token = item.token, tokenId = item.tokenId, value = EthUInt256.TEN),
        )
        ownerships.forEach { ownershipRepository.save(it).awaitFirst() }
        createValidLog(item, listOf(ownerships.first())).forEach {
            nftItemHistoryRepository.save(it).awaitFirst()
        }
        inconsistentItemRepository.insert(
            InconsistentItem(
                token = item.token,
                tokenId = item.tokenId,
                supply = null,
                lastUpdatedAt = nowMillis(),
                ownerships = null,
                ownershipsValue = null,
                supplyValue = null
            )
        )

        handler = ItemOwnershipConsistencyJobHandler(
            jobStateRepository,
            itemRepository,
            NftListenerProperties().copy(elementsFetchJobSize = 2),
            itemOwnershipConsistencyService,
            inconsistentItemRepository,
            metricsFactory,
        )

        handler.handle()
        val result = itemOwnershipConsistencyService.checkItem(item)

        val owners = ownershipRepository.search(
            Query(
                where(Ownership::token).isEqualTo(item.token).and(Ownership::tokenId).isEqualTo(item.tokenId)
            )
        )
        assertThat(owners).hasSize(1)
        assertThat(result).isExactlyInstanceOf(ItemOwnershipConsistencyService.CheckResult.Failure::class.java)
    }

    @Test
    internal fun `should fix item if it is in inconsistent_items but fixed`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.ONE, date = nowMillis() - Duration.ofMinutes(10))
        itemRepository.save(item).awaitFirst()
        val ownerships = listOf(
            createRandomOwnership().copy(token = item.token, tokenId = item.tokenId, value = EthUInt256.TEN),
        )
        ownerships.forEach { ownershipRepository.save(it).awaitFirst() }
        createValidLog(item, listOf(ownerships.first())).forEach {
            nftItemHistoryRepository.save(it).awaitFirst()
        }
        inconsistentItemRepository.insert(
            InconsistentItem(
                token = item.token,
                tokenId = item.tokenId,
                status = InconsistentItemStatus.FIXED,
                supply = null,
                lastUpdatedAt = nowMillis(),
                ownerships = null,
                ownershipsValue = null,
                supplyValue = null
            )
        )

        handler = ItemOwnershipConsistencyJobHandler(
            jobStateRepository,
            itemRepository,
            NftListenerProperties().copy(elementsFetchJobSize = 2),
            itemOwnershipConsistencyService,
            inconsistentItemRepository,
            metricsFactory,
        )

        handler.handle()
        val result = itemOwnershipConsistencyService.checkItem(item)

        val owners = ownershipRepository.search(
            Query(
                where(Ownership::token).isEqualTo(item.token).and(Ownership::tokenId).isEqualTo(item.tokenId)
            )
        )
        assertThat(owners).hasSize(1)
        assertThat(result).isExactlyInstanceOf(ItemOwnershipConsistencyService.CheckResult.Success::class.java)
    }
}
