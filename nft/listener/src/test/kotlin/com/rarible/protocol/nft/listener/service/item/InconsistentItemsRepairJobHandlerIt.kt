package com.rarible.protocol.nft.listener.service.item

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomInconsistentItem
import com.rarible.protocol.nft.core.misc.RateLimiter
import com.rarible.protocol.nft.core.model.InconsistentItemStatus
import com.rarible.protocol.nft.core.repository.InconsistentItemRepository
import com.rarible.protocol.nft.core.repository.JobStateRepository
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.metrics.NftListenerMetricsFactory
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import com.rarible.protocol.nft.listener.test.data.createRandomItem
import com.rarible.protocol.nft.listener.test.data.createRandomOwnership
import com.rarible.protocol.nft.listener.test.data.createValidLog
import io.micrometer.core.instrument.Counter
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.atomic.AtomicLong

@IntegrationTest
class InconsistentItemsRepairJobHandlerIt : AbstractIntegrationTest() {

    private lateinit var handler: InconsistentItemsRepairJobHandler

    @Autowired
    private lateinit var jobStateRepository: JobStateRepository

    @Autowired
    private lateinit var inconsistentItemRepository: InconsistentItemRepository

    @Autowired
    private lateinit var itemOwnershipConsistencyService: ItemOwnershipConsistencyService

    @Autowired
    private lateinit var metricsFactory: NftListenerMetricsFactory

    private lateinit var checkedCounter: Counter
    private lateinit var fixedCounter: Counter
    private lateinit var unfixedCounter: Counter
    private lateinit var delayGauge: AtomicLong

    private var checkedBefore: Double = -1.0
    private var fixedBefore: Double = -1.0
    private var unfixedBefore: Double = -1.0
    private var delayBefore: Long = -1

    @BeforeEach
    fun prepareMocks() = runBlocking<Unit> {
        itemRepository.createIndexes()
        nftItemHistoryRepository.createIndexes()
        inconsistentItemRepository.createIndexes()

        checkedCounter = metricsFactory.inconsistentItemsRepairJobCheckedCounter
        fixedCounter = metricsFactory.inconsistentItemsRepairJobFixedCounter
        unfixedCounter = metricsFactory.inconsistentItemsRepairJobUnfixedCounter
        delayGauge = metricsFactory.inconsistentItemsRepairJobDelayGauge

        delayBefore = -1
        delayGauge.set(delayBefore)
        checkedBefore = checkedCounter.count()
        fixedBefore = fixedCounter.count()
        unfixedBefore = unfixedCounter.count()
    }

    @Test
    fun `nothing to fix - empty collection`() = runBlocking<Unit> {
        // given
        initHandler()

        // when
        handler.handle()

        // then
        assertThat(delayGauge.get()).isNotEqualTo(delayBefore)
        assertThat(checkedCounter.count()).isEqualTo(checkedBefore)
        assertThat(fixedCounter.count()).isEqualTo(fixedBefore)
        assertThat(unfixedCounter.count()).isEqualTo(unfixedBefore)
    }

    @Test
    fun `nothing to fix - all fixed already`() = runBlocking<Unit> {
        // given
        initHandler()
        inconsistentItemRepository.save(createRandomInconsistentItem().copy(status = InconsistentItemStatus.FIXED))
        inconsistentItemRepository.save(createRandomInconsistentItem().copy(status = InconsistentItemStatus.FIXED))
        inconsistentItemRepository.save(createRandomInconsistentItem().copy(status = InconsistentItemStatus.FIXED))

        // when
        handler.handle()

        // then
        assertThat(delayGauge.get()).isNotEqualTo(delayBefore)
        assertThat(checkedCounter.count()).isEqualTo(checkedBefore + 3)
    }

    @Test
    fun `should try fix new inconsistent item - success`() = runBlocking<Unit> {
        // given
        initHandler()
        val item = itemRepository.save(createRandomItem().copy(supply = EthUInt256.ONE)).awaitFirst()
        val ownerships = listOf(
            createRandomOwnership().copy(token = item.token, tokenId = item.tokenId, value = EthUInt256.TEN),
        )
        ownerships.forEach { ownershipRepository.save(it).awaitFirst() }
        createValidLog(item, listOf(ownerships.first())).forEach {
            nftItemHistoryRepository.save(it).awaitFirst()
        }
        val inconsistentItem = createRandomInconsistentItem().copy(
            token = item.token,
            tokenId = item.tokenId,
            status = InconsistentItemStatus.NEW
        )
        inconsistentItemRepository.save(inconsistentItem)

        // when
        handler.handle()
        val actual = inconsistentItemRepository.get(inconsistentItem.id)!!

        // then
        assertThat(actual.status).isEqualTo(InconsistentItemStatus.FIXED)
        assertThat(actual.fixVersionApplied).isEqualTo(2)
        assertThat(checkedCounter.count()).isEqualTo(checkedBefore + 1)
        assertThat(fixedCounter.count()).isEqualTo(fixedBefore + 1)
    }

    @Test
    fun `should try fix new inconsistent item - failure`() = runBlocking<Unit> {
        // given
        initHandler()
        val item = createRandomItem().copy(supply = EthUInt256.ONE)
        itemRepository.save(item).awaitFirst()
        val ownerships = listOf(
            createRandomOwnership().copy(token = item.token, tokenId = item.tokenId, value = EthUInt256.TEN),
        )
        ownerships.forEach { ownershipRepository.save(it).awaitFirst() }
        val inconsistentItem = createRandomInconsistentItem().copy(
            token = item.token,
            tokenId = item.tokenId,
            status = InconsistentItemStatus.NEW
        )
        inconsistentItemRepository.save(inconsistentItem)

        // when
        handler.handle()
        val actual = inconsistentItemRepository.get(inconsistentItem.id)!!

        // then
        assertThat(actual.status).isEqualTo(InconsistentItemStatus.UNFIXED)
        assertThat(actual.fixVersionApplied).isEqualTo(3)
        assertThat(delayGauge.get()).isNotEqualTo(delayBefore)
        assertThat(checkedCounter.count()).isEqualTo(checkedBefore + 1)
        assertThat(unfixedCounter.count()).isEqualTo(unfixedBefore + 1)
    }

    @Test
    fun `should check unfixed inconsistent item - same version, no try to fix`() = runBlocking<Unit> {
        // given
        initHandler()
        val inconsistentItem = createRandomInconsistentItem().copy(
            status = InconsistentItemStatus.UNFIXED,
            fixVersionApplied = 2,
        )
        inconsistentItemRepository.save(inconsistentItem)

        // when
        handler.handle()
        val actual = inconsistentItemRepository.get(inconsistentItem.id)!!

        // then
        assertThat(actual.status).isEqualTo(InconsistentItemStatus.UNFIXED)
        assertThat(delayGauge.get()).isNotEqualTo(delayBefore)
        assertThat(checkedCounter.count()).isEqualTo(checkedBefore + 1)
    }

    @Test
    fun `should try fix relapsed inconsistent item - success`() = runBlocking<Unit> {
        // given
        initHandler()
        val item = createRandomItem().copy(supply = EthUInt256.ONE)
        itemRepository.save(item).awaitFirst()
        val ownerships = listOf(
            createRandomOwnership().copy(token = item.token, tokenId = item.tokenId, value = EthUInt256.TEN),
        )
        ownerships.forEach { ownershipRepository.save(it).awaitFirst() }
        createValidLog(item, listOf(ownerships.first())).forEach {
            nftItemHistoryRepository.save(it).awaitFirst()
        }
        val inconsistentItem = createRandomInconsistentItem().copy(
            token = item.token,
            tokenId = item.tokenId,
            status = InconsistentItemStatus.RELAPSED,
            relapseCount = 1,
            fixVersionApplied = 1,
        )
        inconsistentItemRepository.save(inconsistentItem)

        // when
        handler.handle()
        val actual = inconsistentItemRepository.get(inconsistentItem.id)!!

        // then
        assertThat(actual.status).isEqualTo(InconsistentItemStatus.FIXED)
        assertThat(actual.fixVersionApplied).isEqualTo(2)

        assertThat(delayGauge.get()).isNotEqualTo(delayBefore)
        assertThat(checkedCounter.count()).isEqualTo(checkedBefore + 1)
        assertThat(fixedCounter.count()).isEqualTo(fixedBefore + 1)
    }

    private fun initHandler() {
        handler = InconsistentItemsRepairJobHandler(
            jobStateRepository,
            NftListenerProperties().copy(elementsFetchJobSize = 2),
            inconsistentItemRepository,
            itemOwnershipConsistencyService,
            RateLimiter(100, 100, "test"),
            metricsFactory,
        )
    }
}
