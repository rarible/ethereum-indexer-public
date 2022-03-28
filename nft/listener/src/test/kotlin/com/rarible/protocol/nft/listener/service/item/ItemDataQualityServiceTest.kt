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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.math.BigInteger
import java.time.Duration

@IntegrationTest
@FlowPreview
const val OWNERS_NUMBER = 4
internal class ItemDataQualityServiceTest : AbstractIntegrationTest() {
    @BeforeEach
    fun setupDbIndexes() = runBlocking<Unit> {
        itemRepository.createIndexes()
    }

    @Autowired
    private lateinit var itemReduceService: ItemReduceService

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
            itemReduceService = itemReduceService
        )
        val validItem1 = createRandomItem().copy(supply = EthUInt256.of(OWNERS_NUMBER), date = now - Duration.ofMinutes(5))
        val invalidItem1 = createRandomItem().copy(supply = EthUInt256.of(OWNERS_NUMBER), date = now - Duration.ofMinutes(4))
        listOf(validItem1, invalidItem1).forEach { itemRepository.save(it).awaitFirst() }
        listOf(
            createValidOwnerships(validItem1),
            createInvalidValidOwnerships(invalidItem1),
        ).flatten().forEach { ownershipRepository.save(it).awaitFirst() }
        var continuations = itemDataQualityService.checkItems(from = null).toList()
        assertThat(continuations).hasSize(2)
        assertThat(continuations[0].let { ItemContinuation.parse(it)?.afterId }).isEqualTo(invalidItem1.id)
        assertThat(continuations[1].let { ItemContinuation.parse(it)?.afterId }).isEqualTo(validItem1.id)

        verify(exactly = 1) { counter.increment() }

        val validItem2 = createRandomItem().copy(supply = EthUInt256.of(OWNERS_NUMBER), date = now - Duration.ofMinutes(7))
        val invalidItem2 = createRandomItem().copy(supply = EthUInt256.of(OWNERS_NUMBER), date = now - Duration.ofMinutes(6))
        listOf(validItem2, invalidItem2).forEach { itemRepository.save(it).awaitFirst() }
        listOf(
            createValidOwnerships(validItem2),
            createInvalidValidOwnerships(invalidItem2)
        ).flatten().forEach { ownershipRepository.save(it).awaitFirst() }

        continuations = itemDataQualityService.checkItems(from = continuations.last()).toList()
        assertThat(continuations).hasSize(2)
        assertThat(continuations[0].let { ItemContinuation.parse(it)?.afterId }).isEqualTo(invalidItem2.id)
        assertThat(continuations[1].let { ItemContinuation.parse(it)?.afterId }).isEqualTo(validItem2.id)
        verify(exactly = 2) { counter.increment() }

        nftItemHistoryRepository.save(createValidLog(invalidItem1)).awaitFirst()
        nftItemHistoryRepository.save(createValidLog(invalidItem2)).awaitFirst()
        itemDataQualityService.checkItems(null)
        assertThat(itemDataQualityService.checkItem(invalidItem1)).isTrue()
        assertThat(itemDataQualityService.checkItem(invalidItem2)).isTrue()
    }

    private fun createValidLog(item: Item): LogEvent {
        return createLog(
            blockNumber = 1,
            token = item.token,
            tokenId = item.tokenId,
            value = item.supply * EthUInt256.of(OWNERS_NUMBER)
        )
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
        value: EthUInt256 = EthUInt256.ONE
    ): LogEvent {
        val transfer = ItemTransfer(
            owner = randomAddress(),
            token = token,
            tokenId = tokenId,
            date = nowMillis(),
            from = Address.ZERO(),
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