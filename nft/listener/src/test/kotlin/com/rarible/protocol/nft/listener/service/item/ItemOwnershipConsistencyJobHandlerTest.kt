package com.rarible.protocol.nft.listener.service.item

import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.common.nowMillis
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.repository.InconsistentItemRepository
import com.rarible.protocol.nft.core.repository.JobStateRepository
import com.rarible.protocol.nft.core.service.item.ItemOwnershipConsistencyService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.data.createRandomItem
import com.rarible.protocol.nft.listener.data.createRandomOwnership
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import com.rarible.protocol.nft.listener.metrics.NftListenerMetricsFactory
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import scalether.domain.Address
import java.time.Duration

@IntegrationTest
class ItemOwnershipConsistencyJobHandlerTest : AbstractIntegrationTest() {

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

    @MockkBean(relaxed = true)
    private lateinit var metricsFactory: NftListenerMetricsFactory

    @RelaxedMockK
    private lateinit var checkedCounter: RegisteredCounter
    @RelaxedMockK
    private lateinit var fixedCounter: RegisteredCounter
    @RelaxedMockK
    private lateinit var unfixedCounter: RegisteredCounter

    @Autowired
    private lateinit var itemOwnershipConsistencyService: ItemOwnershipConsistencyService

    @BeforeEach
    fun prepareMocks() {
        every { metricsFactory.itemOwnershipConsistencyJobCheckedCounter() } returns checkedCounter
        every { metricsFactory.itemOwnershipConsistencyJobFixedCounter() } returns fixedCounter
        every { metricsFactory.itemOwnershipConsistencyJobUnfixedCounter() } returns unfixedCounter
    }

    @Test
    fun `should save invalid items that can't be fixed to corresponding repo`() = runBlocking<Unit> {
        val now = nowMillis()

        handler = ItemOwnershipConsistencyJobHandler(
            jobStateRepository,
            itemRepository,
            NftListenerProperties().copy(elementsFetchJobSize = 2),
            itemOwnershipConsistencyService,
            inconsistentItemRepository,
            metricsFactory,
        )

        val validItem =
            createRandomItem().copy(supply = EthUInt256.of(OWNERS_NUMBER), date = now - Duration.ofMinutes(10))
        val fixableItem =
            createRandomItem().copy(supply = EthUInt256.of(OWNERS_NUMBER), date = now - Duration.ofMinutes(20))
        val invalidItem =
            createRandomItem().copy(supply = EthUInt256.of(OWNERS_NUMBER), date = now - Duration.ofMinutes(30))
        listOf(
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

        // when
        handler.handle()

        // then
        val invalidItems = inconsistentItemRepository.findAll().toList()
        assertThat(invalidItems).hasSize(1)
        assertThat(invalidItems.single().id).isEqualTo(invalidItem.id)
        verify {
            checkedCounter.increment(2L)
            // Fixed item got updated, thus gets in second fetch once again, so 1 + 1
            checkedCounter.increment(2L)
        }
        verify { fixedCounter.increment() }
        verify { unfixedCounter.increment() }
        confirmVerified(checkedCounter, fixedCounter, unfixedCounter)
    }

    @Test
    internal fun `should fix item`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.ONE)
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


    private fun createValidLog(item: Item, ownerships: List<Ownership>): List<LogEvent> {
        return ownerships.mapIndexed { index, ownership ->
            createLog(
                token = item.token,
                tokenId = item.tokenId,
                value = EthUInt256.ONE,
                from = Address.ZERO(),
                owner = ownership.owner,
                logIndex = index
            )
        }
    }

    private fun createValidOwnerships(item: Item): List<Ownership> {
        return (1..OWNERS_NUMBER).map {
            createRandomOwnership().copy(
                token = item.token,
                tokenId = item.tokenId,
                value = item.supply / EthUInt256.of(OWNERS_NUMBER)
            )
        }
    }

    private fun createInvalidValidOwnerships(item: Item): List<Ownership> {
        return (1..OWNERS_NUMBER).map {
            createRandomOwnership().copy(
                token = item.token,
                tokenId = item.tokenId,
                value = item.supply
            )
        }
    }

    private fun createLog(
        token: Address = randomAddress(),
        blockNumber: Long = 1,
        tokenId: EthUInt256 = EthUInt256.of(randomBigInt()),
        value: EthUInt256 = EthUInt256.ONE,
        owner: Address = randomAddress(),
        from: Address = Address.ZERO(),
        logIndex: Int
    ): LogEvent {
        val transfer = ItemTransfer(
            owner = owner,
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = from,
            value = value
        )
        return LogEvent(
            data = transfer,
            address = token,
            topic = WordFactory.create(),
            transactionHash = Word.apply(randomWord()),
            status = LogEventStatus.CONFIRMED,
            from = randomAddress(),
            index = 0,
            logIndex = logIndex,
            blockNumber = blockNumber,
            minorLogIndex = 0,
            blockTimestamp = nowMillis().epochSecond,
            createdAt = nowMillis()
        )
    }
}
