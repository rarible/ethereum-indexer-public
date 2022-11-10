package com.rarible.protocol.nft.listener.service.item

import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.common.nowMillis
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.telemetry.metrics.RegisteredGauge
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomInconsistentItem
import com.rarible.protocol.nft.core.model.InconsistentItemStatus
import com.rarible.protocol.nft.core.repository.InconsistentItemRepository
import com.rarible.protocol.nft.core.repository.JobStateRepository
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService.Companion.CURRENT_FIX_VERSION
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.data.createRandomItem
import com.rarible.protocol.nft.listener.data.createRandomOwnership
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import com.rarible.protocol.nft.listener.metrics.NftListenerMetricsFactory
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

@IntegrationTest
class InconsistentItemsRepairJobHandlerTest : AbstractIntegrationTest() {

    private lateinit var handler: InconsistentItemsRepairJobHandler

    @Autowired
    private lateinit var jobStateRepository: JobStateRepository

    @Autowired
    private lateinit var inconsistentItemRepository: InconsistentItemRepository

    @Autowired
    private lateinit var itemOwnershipConsistencyService: ItemOwnershipConsistencyService

    @MockkBean(relaxed = true)
    private lateinit var metricsFactory: NftListenerMetricsFactory

    @RelaxedMockK
    private lateinit var checkedCounter: RegisteredCounter
    @RelaxedMockK
    private lateinit var fixedCounter: RegisteredCounter
    @RelaxedMockK
    private lateinit var unfixedCounter: RegisteredCounter
    @RelaxedMockK
    private lateinit var delayGauge: RegisteredGauge<Long>

    @BeforeEach
    fun prepareMocks() = runBlocking<Unit> {
        itemRepository.createIndexes()
        nftItemHistoryRepository.createIndexes()
        inconsistentItemRepository.createIndexes()

        every { metricsFactory.inconsistentItemsRepairJobCheckedCounter() } returns checkedCounter
        every { metricsFactory.inconsistentItemsRepairJobFixedCounter() } returns fixedCounter
        every { metricsFactory.inconsistentItemsRepairJobUnfixedCounter() } returns unfixedCounter
        every { metricsFactory.inconsistentItemsRepairJobDelayGauge() } returns delayGauge
    }

    @Test
    fun `should iterate and fix inconsistent_items - empty`() = runBlocking {
        // given
        initHandler()

        // when
        handler.handle()

        // then
        coVerify {
            delayGauge.set(any())
        }
        confirmVerified(checkedCounter, fixedCounter, unfixedCounter, delayGauge)
    }

    @Test
    fun `should iterate over inconsistent_items - all fixed already`() = runBlocking {
        // given
        initHandler()
        inconsistentItemRepository.save(createRandomInconsistentItem().copy(status = InconsistentItemStatus.FIXED))
        inconsistentItemRepository.save(createRandomInconsistentItem().copy(status = InconsistentItemStatus.FIXED))
        inconsistentItemRepository.save(createRandomInconsistentItem().copy(status = InconsistentItemStatus.FIXED))

        // when
        handler.handle()

        // then
        coVerify {
            delayGauge.set(any())
        }
        coVerify(exactly = 3) {
            checkedCounter.increment(1)
        }
        confirmVerified(checkedCounter, fixedCounter, unfixedCounter, delayGauge)
    }

    @Test
    fun `should try fix new inconsistent item - success`() = runBlocking<Unit> {
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
            status = InconsistentItemStatus.NEW
        )
        inconsistentItemRepository.save(inconsistentItem)

        // when
        handler.handle()
        val actual = inconsistentItemRepository.get(inconsistentItem.id)!!

        // then
        assertThat(actual.status).isEqualTo(InconsistentItemStatus.FIXED)
        assertThat(actual.fixVersionApplied).isEqualTo(CURRENT_FIX_VERSION)
        coVerify {
            delayGauge.set(any())
            checkedCounter.increment(1)
            fixedCounter.increment(1)
        }
        confirmVerified(checkedCounter, fixedCounter, unfixedCounter, delayGauge)
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
        assertThat(actual.fixVersionApplied).isEqualTo(CURRENT_FIX_VERSION)
        coVerify {
            delayGauge.set(any())
            checkedCounter.increment(1)
            unfixedCounter.increment(1)
        }
        confirmVerified(checkedCounter, fixedCounter, unfixedCounter, delayGauge)
    }

    @Test
    fun `should check unfixed inconsistent item - same version, no try to fix`() = runBlocking<Unit> {
        // given
        initHandler()
        val inconsistentItem = createRandomInconsistentItem().copy(
            status = InconsistentItemStatus.UNFIXED,
            fixVersionApplied = CURRENT_FIX_VERSION,
        )
        inconsistentItemRepository.save(inconsistentItem)

        // when
        handler.handle()
        val actual = inconsistentItemRepository.get(inconsistentItem.id)!!

        // then
        assertThat(actual.status).isEqualTo(InconsistentItemStatus.UNFIXED)
        coVerify {
            delayGauge.set(any())
            checkedCounter.increment(1)
        }
        confirmVerified(checkedCounter, fixedCounter, unfixedCounter, delayGauge)
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
            fixVersionApplied = CURRENT_FIX_VERSION - 1,
        )
        inconsistentItemRepository.save(inconsistentItem)

        // when
        handler.handle()
        val actual = inconsistentItemRepository.get(inconsistentItem.id)!!

        // then
        assertThat(actual.status).isEqualTo(InconsistentItemStatus.FIXED)
        assertThat(actual.fixVersionApplied).isEqualTo(CURRENT_FIX_VERSION)
        coVerify {
            delayGauge.set(any())
            checkedCounter.increment(1)
            fixedCounter.increment(1)
        }
        confirmVerified(checkedCounter, fixedCounter, unfixedCounter, delayGauge)
    }

    private fun initHandler() {
        handler = InconsistentItemsRepairJobHandler(
            jobStateRepository,
            NftListenerProperties().copy(elementsFetchJobSize = 2),
            inconsistentItemRepository,
            itemOwnershipConsistencyService,
            metricsFactory,
        )
    }
}
