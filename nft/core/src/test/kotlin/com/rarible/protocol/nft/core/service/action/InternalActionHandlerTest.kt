package com.rarible.protocol.nft.core.service.action

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomBurnItemAction
import com.rarible.protocol.nft.core.data.createRandomBurnItemActionEvent
import com.rarible.protocol.nft.core.model.Action
import com.rarible.protocol.nft.core.model.ActionState
import com.rarible.protocol.nft.core.model.ActionType
import com.rarible.protocol.nft.core.model.BurnItemAction
import com.rarible.protocol.nft.core.repository.action.NftItemActionEventRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration
import java.time.Instant

internal class InternalActionHandlerTest {
    private val nftItemActionEventRepository = mockk<NftItemActionEventRepository>()
    private val clock = mockk<Clock>()
    private val registeredCounter = mockk<RegisteredCounter> { every { increment() } returns Unit }
    private val internalActionHandler = ActionEventHandler(nftItemActionEventRepository, clock, registeredCounter)

    @Test
    fun `should save a new burn action`() = runBlocking {
        val now = Instant.now()
        val event = createRandomBurnItemActionEvent()

        coEvery { nftItemActionEventRepository.findByItemIdAndType(event.itemId(), ActionType.BURN) } returns emptyList()
        every { nftItemActionEventRepository.save(any()) } answers {
            Mono.just(args.first() as Action)
        }
        every { clock.instant() } returns now
        internalActionHandler.handle(event)
        verify(exactly = 1) {
            nftItemActionEventRepository.save(withArg {
                assertThat(it).isInstanceOf(BurnItemAction::class.java)
                it as BurnItemAction
                assertThat(it.itemId()).isEqualTo(event.itemId())
                assertThat(it.actionAt).isEqualTo(event.burnAt)
                assertThat(it.lastUpdatedAt).isEqualTo(now)
                assertThat(it.createdAt).isEqualTo(now)
                assertThat(it.state).isEqualTo(ActionState.PENDING)
            })
        }
    }

    @Test
    fun `should save update existed action`() = runBlocking {
        val now = Instant.now()
        val token = randomAddress()
        val tokenId = EthUInt256.of(randomBigInt())
        val action = createRandomBurnItemAction().copy(
            state = ActionState.EXECUTED,
            token = token,
            tokenId = tokenId
        )
        val event = createRandomBurnItemActionEvent().copy(
            burnAt = action.actionAt + Duration.ofDays(1),
            token = token,
            tokenId = tokenId
        )
        coEvery { nftItemActionEventRepository.findByItemIdAndType(event.itemId(), ActionType.BURN) } returns listOf(action)
        every { nftItemActionEventRepository.save(any()) } answers {
            Mono.just(args.first() as Action)
        }
        every { clock.instant() } returns now
        internalActionHandler.handle(event)
        verify(exactly = 1) {
            nftItemActionEventRepository.save(withArg {
                assertThat(it).isInstanceOf(BurnItemAction::class.java)
                it as BurnItemAction
                assertThat(it.id).isEqualTo(action.id)
                assertThat(it.version).isEqualTo(action.version)
                assertThat(it.itemId()).isEqualTo(event.itemId())
                assertThat(it.actionAt).isEqualTo(event.burnAt)
                assertThat(it.lastUpdatedAt).isEqualTo(now)
                assertThat(it.createdAt).isEqualTo(action.createdAt)
                assertThat(it.state).isEqualTo(ActionState.PENDING)
            })
        }
    }

    @Test
    fun `should not save existed action`() = runBlocking {
        val now = Instant.now()
        val token = randomAddress()
        val tokenId = EthUInt256.of(randomBigInt())
        val action = createRandomBurnItemAction().copy(
            state = ActionState.EXECUTED,
            token = token,
            tokenId = tokenId
        )
        val event = createRandomBurnItemActionEvent().copy(
            burnAt = action.actionAt,
            token = token,
            tokenId = tokenId
        )
        every { clock.instant() } returns now
        coEvery { nftItemActionEventRepository.findByItemIdAndType(event.itemId(), ActionType.BURN) } returns listOf(action)
        internalActionHandler.handle(event)
        verify(exactly = 0) {
            nftItemActionEventRepository.save(any())
        }
    }
}