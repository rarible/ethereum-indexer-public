package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.common.nowMillis
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemContinuation
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.repository.InconsistentItemRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.data.createRandomItem
import com.rarible.protocol.nft.listener.data.createRandomOwnership
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import scalether.domain.Address

const val OWNERS_NUMBER = 4

@IntegrationTest
@FlowPreview
internal class ItemDataQualityServiceTest : AbstractIntegrationTest() {
    @BeforeEach
    fun setupDbIndexes() = runBlocking {
        itemRepository.createIndexes()
        nftItemHistoryRepository.createIndexes()
    }

    @Autowired
    private lateinit var itemReduceService: ItemReduceService

    @Autowired
    private lateinit var inconsistentItemRepository: InconsistentItemRepository

    @Test
    fun `should alert invalid items`() = runBlocking<Unit> {
        val now = nowMillis()
        val counter = mockk<RegisteredCounter> {
            every { increment() } returns Unit
        }
        val itemDataQualityService = ItemDataQualityService(
            itemRepository = itemRepository,
            ownershipRepository = ownershipRepository,
            itemDataQualityErrorRegisteredCounter = counter,
            nftListenerProperties = NftListenerProperties().copy(elementsFetchJobSize = 2),
            itemReduceService = itemReduceService,
            inconsistentItemRepository = inconsistentItemRepository
        )
        val validItem = createRandomItem().copy(supply = EthUInt256.of(OWNERS_NUMBER), date = now - Duration.ofMinutes(1))
        val fixableItem = createRandomItem().copy(supply = EthUInt256.of(OWNERS_NUMBER), date = now - Duration.ofMinutes(2))
        val invalidItem = createRandomItem().copy(supply = EthUInt256.of(OWNERS_NUMBER), date = now - Duration.ofMinutes(3))
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

        val continuations = itemDataQualityService.checkItems(from = null).toList()

        //assertThat(continuations).hasSize(3)
        assertThat(continuations[0].let { ItemContinuation.parse(it)?.afterId }).isEqualTo(validItem.id)
        assertThat(continuations[1].let { ItemContinuation.parse(it)?.afterId }).isEqualTo(fixableItem.id)
        assertThat(continuations[2].let { ItemContinuation.parse(it)?.afterId }).isEqualTo(invalidItem.id)

        verify(exactly = 1) { counter.increment() }

        val invalidItems = inconsistentItemRepository.findAll().toList()
        assertThat(invalidItems).hasSize(1)
        assertThat(invalidItems.single().id).isEqualTo(invalidItem.id)
    }

    @Test
    internal fun `should fix item`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.ONE)
        itemRepository.save(item).awaitFirst()
        val ownerships = listOf(
            createRandomOwnership().copy(token = item.token, tokenId = item.tokenId, value = EthUInt256.ONE),
            createRandomOwnership().copy(token = item.token, tokenId = item.tokenId, value = EthUInt256.ONE)
        )
        ownerships.forEach { ownershipRepository.save(it).awaitFirst() }
        createValidLog(item, listOf(ownerships.first())).forEach {
            nftItemHistoryRepository.save(it).awaitFirst()
        }

        val itemDataQualityService = ItemDataQualityService(
            itemRepository = itemRepository,
            ownershipRepository = ownershipRepository,
            itemDataQualityErrorRegisteredCounter = mockk {
                every { increment() } returns Unit
            },
            nftListenerProperties = NftListenerProperties().copy(elementsFetchJobSize = 2),
            itemReduceService = itemReduceService,
            inconsistentItemRepository = inconsistentItemRepository
        )

        itemDataQualityService.checkItems(from = null).toList()
        val result = itemDataQualityService.checkItem(item)

        val owners = ownershipRepository.search(Query(where(Ownership::token).isEqualTo(item.token).and(Ownership::tokenId).isEqualTo(item.tokenId)))
        assertThat(owners).hasSize(1)
        assertThat(result).isEqualTo(ItemDataQualityService.CheckResult.Success)
    }

    private fun createValidLog(item: Item, ownerships: List<Ownership>): List<LogEvent> {
        return ownerships.map { ownership ->
            createLog(
                token = item.token,
                tokenId = item.tokenId,
                value = EthUInt256.ONE,
                from = Address.ZERO(),
                owner = ownership.owner
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
        from: Address = Address.ZERO()
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
            logIndex = 1,
            blockNumber = blockNumber,
            minorLogIndex = 0,
            blockTimestamp = nowMillis().epochSecond
        )
    }
}
