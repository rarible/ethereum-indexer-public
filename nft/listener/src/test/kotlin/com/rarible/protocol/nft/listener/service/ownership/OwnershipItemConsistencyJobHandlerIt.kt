package com.rarible.protocol.nft.listener.service.ownership

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomInconsistentItem
import com.rarible.protocol.nft.core.model.InconsistentItemStatus
import com.rarible.protocol.nft.core.repository.InconsistentItemRepository
import com.rarible.protocol.nft.core.repository.JobStateRepository
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.configuration.OwnershipItemConsistencyProperties
import com.rarible.protocol.nft.listener.metrics.NftListenerMetricsFactory
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import com.rarible.protocol.nft.listener.test.data.createRandomItem
import com.rarible.protocol.nft.listener.test.data.createRandomOwnership
import io.micrometer.core.instrument.Counter
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

@IntegrationTest
class OwnershipItemConsistencyJobHandlerIt : AbstractIntegrationTest() {

    private lateinit var handler: OwnershipItemConsistencyJobHandler

    @Autowired
    private lateinit var inconsistentItemRepository: InconsistentItemRepository

    @Autowired
    private lateinit var jobStateRepository: JobStateRepository

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

        checkedCounter = metricsFactory.ownershipItemConsistencyJobCheckedCounter
        fixedCounter = metricsFactory.ownershipItemConsistencyJobFixedCounter
        unfixedCounter = metricsFactory.ownershipItemConsistencyJobUnfixedCounter
        delayGauge = metricsFactory.ownershipItemConsistencyJobDelayGauge

        delayBefore = -1
        delayGauge.set(delayBefore)
        checkedBefore = checkedCounter.count()
        fixedBefore = fixedCounter.count()
        unfixedBefore = unfixedCounter.count()
    }

    @Test
    fun `should do nothing when there is no ownerships left`() = runBlocking<Unit> {
        // given
        initHandler()

        // when
        handler.handle()

        // then
        assertThat(delayGauge.get()).isNotEqualTo(delayBefore)
    }

    @Test
    fun `should do nothing when ownerships are beyond threshold`() = runBlocking<Unit> {
        // given
        initHandler(offset = Duration.ofSeconds(10))
        val ownership = createRandomOwnership().copy(date = nowMillis().minusSeconds(5))
        ownershipRepository.save(ownership).awaitFirst()

        // when
        handler.handle()

        // then
        assertThat(delayGauge.get()).isNotEqualTo(delayBefore)
    }

    @Test
    fun `should do nothing when ownership items are already in inconsistent items repo`() = runBlocking<Unit> {
        // given
        initHandler()
        val item = createRandomItem()
        itemRepository.save(item).awaitFirst()
        val ownership = createRandomOwnership()
            .copy(token = item.token, tokenId = item.tokenId, date = nowMillis().minusSeconds(1000))
        ownershipRepository.save(ownership).awaitFirst()
        val inconsistentItem = createRandomInconsistentItem()
            .copy(token = item.token, tokenId = item.tokenId)
        inconsistentItemRepository.insert(inconsistentItem)

        // when
        handler.handle()

        // then
        assertThat(delayGauge.get()).isNotEqualTo(delayBefore)
    }

    @Test
    fun `should check when ownership items are already inconsistent items repo but fixed`() = runBlocking<Unit> {
        // given
        initHandler()
        val item = createRandomItem()
        itemRepository.save(item).awaitFirst()
        val ownership = createRandomOwnership()
            .copy(token = item.token, tokenId = item.tokenId, date = nowMillis().minusSeconds(1000))
        ownershipRepository.save(ownership).awaitFirst()
        val inconsistentItem = createRandomInconsistentItem()
            .copy(token = item.token, tokenId = item.tokenId, status = InconsistentItemStatus.FIXED)
        inconsistentItemRepository.insert(inconsistentItem)

        // when
        handler.handle()

        // then
        assertThat(delayGauge.get()).isNotEqualTo(delayBefore)
        assertThat(checkedCounter.count()).isEqualTo(checkedBefore + 1)
    }

    @Test
    fun `should check several items that are valid`() = runBlocking<Unit> {
        // given
        initHandler()
        val item1 = createRandomItem().copy(supply = EthUInt256.TEN)
        val item2 = createRandomItem().copy(supply = EthUInt256.TEN)
        itemRepository.save(item1).awaitFirst()
        itemRepository.save(item2).awaitFirst()
        val ownership1 = createRandomOwnership()
            .copy(
                token = item1.token, tokenId = item1.tokenId, date = nowMillis().minusSeconds(500),
                value = EthUInt256.TEN
            )
        val ownership2 = createRandomOwnership()
            .copy(
                token = item2.token, tokenId = item2.tokenId, date = nowMillis().minusSeconds(400),
                value = EthUInt256.TEN
            )
        ownershipRepository.save(ownership1).awaitFirst()
        ownershipRepository.save(ownership2).awaitFirst()

        // when
        handler.handle()

        // then
        assertThat(delayGauge.get()).isNotEqualTo(delayBefore)
        assertThat(checkedCounter.count()).isEqualTo(checkedBefore + 2)
    }

    @Test
    fun `should check and save several items as unfixed`() = runBlocking<Unit> {
        // given
        initHandler()
        val ownership1 = createRandomOwnership().copy(date = nowMillis().minusSeconds(500))
        val ownership2 = createRandomOwnership().copy(date = nowMillis().minusSeconds(400))
        ownershipRepository.save(ownership1).awaitFirst()
        ownershipRepository.save(ownership2).awaitFirst()

        // when
        handler.handle()

        // then
        assertThat(delayGauge.get()).isNotEqualTo(delayBefore)
        assertThat(checkedCounter.count()).isEqualTo(checkedBefore + 2)
        assertThat(unfixedCounter.count()).isEqualTo(unfixedBefore + 2)
    }

    private fun initHandler(offset: Duration = Duration.ofSeconds(30)) {
        handler = OwnershipItemConsistencyJobHandler(
            jobStateRepository,
            ownershipRepository,
            itemRepository,
            inconsistentItemRepository,
            NftListenerProperties().copy(
                elementsFetchJobSize = 2,
                ownershipItemConsistency = OwnershipItemConsistencyProperties().copy(
                    checkTimeOffset = offset
                ),
            ),
            itemOwnershipConsistencyService,
            metricsFactory,
        )
    }
}
