package com.rarible.protocol.nft.listener.service.ownership

import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.common.nowMillis
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.telemetry.metrics.RegisteredGauge
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.repository.InconsistentItemRepository
import com.rarible.protocol.nft.core.repository.JobStateRepository
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.configuration.OwnershipItemConsistencyProperties
import com.rarible.protocol.nft.listener.data.createRandomItem
import com.rarible.protocol.nft.listener.data.createRandomOwnership
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import com.rarible.protocol.nft.listener.metrics.NftListenerMetricsFactory
import com.rarible.protocol.nft.core.data.createRandomInconsistentItem
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

@IntegrationTest
class OwnershipItemConsistencyJobHandlerTest : AbstractIntegrationTest() {

    private lateinit var handler: OwnershipItemConsistencyJobHandler

    @Autowired
    private lateinit var inconsistentItemRepository: InconsistentItemRepository

    @Autowired
    private lateinit var jobStateRepository: JobStateRepository

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
        every { metricsFactory.ownershipItemConsistencyJobCheckedCounter() } returns checkedCounter
        every { metricsFactory.ownershipItemConsistencyJobFixedCounter() } returns fixedCounter
        every { metricsFactory.ownershipItemConsistencyJobUnfixedCounter() } returns unfixedCounter
        every { metricsFactory.ownershipItemConsistencyJobDelayGauge() } returns delayGauge
    }

    @Test
    fun `should do nothing when there is no ownerships left`() = runBlocking<Unit> {
        // given
        initHandler()

        // when
        handler.handle()

        // then
        verify {
            delayGauge.set(any())
        }
        confirmVerified(checkedCounter, fixedCounter, unfixedCounter, delayGauge)
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
        verify {
            delayGauge.set(any())
        }
        confirmVerified(checkedCounter, fixedCounter, unfixedCounter, delayGauge)
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
        inconsistentItemRepository.save(inconsistentItem)

        // when
        handler.handle()

        // then
        verify {
            delayGauge.set(any())
        }
        confirmVerified(checkedCounter, fixedCounter, unfixedCounter, delayGauge)
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
            .copy(token = item1.token, tokenId = item1.tokenId, date = nowMillis().minusSeconds(500), value = EthUInt256.TEN)
        val ownership2 = createRandomOwnership()
            .copy(token = item2.token, tokenId = item2.tokenId, date = nowMillis().minusSeconds(400), value = EthUInt256.TEN)
        ownershipRepository.save(ownership1).awaitFirst()
        ownershipRepository.save(ownership2).awaitFirst()

        // when
        handler.handle()

        // then
        verify {
            delayGauge.set(any())
            checkedCounter.increment(2)
        }
        confirmVerified(checkedCounter, fixedCounter, unfixedCounter, delayGauge)
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
        verify {
            delayGauge.set(any())
            checkedCounter.increment(2)
            unfixedCounter.increment()
            unfixedCounter.increment()
        }
        confirmVerified(checkedCounter, fixedCounter, unfixedCounter, delayGauge)
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