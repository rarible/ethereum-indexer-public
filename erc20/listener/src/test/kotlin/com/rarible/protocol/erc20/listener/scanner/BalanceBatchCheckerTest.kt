package com.rarible.protocol.erc20.listener.scanner

import com.fasterxml.jackson.databind.node.TextNode
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.Erc20BalanceDto
import com.rarible.protocol.dto.Erc20BalanceUpdateEventDto
import com.rarible.protocol.erc20.core.metric.CheckerMetrics
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.listener.configuration.BalanceCheckerProperties
import com.rarible.protocol.erc20.listener.configuration.Erc20ListenerProperties
import io.daonomic.rpc.domain.Response
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.time.Instant
import java.time.Instant.now
import java.util.UUID

internal class BalanceBatchCheckerTest {

    private val registry = SimpleMeterRegistry()
    private val ethereum: MonoEthereum = mockk()
    private val checkerMetrics: CheckerMetrics = CheckerMetrics(Blockchain.ETHEREUM, registry)
    private val props = Erc20ListenerProperties(
        tokens = listOf(AddressFactory.create().prefixed()),
        balanceCheckerProperties = BalanceCheckerProperties(
            enabled = true
        )
    )

    private lateinit var balanceBatchCheckerHandler: BalanceBatchCheckerHandler

    @BeforeEach
    fun setUp() {
        clearMocks(ethereum)
        registry.clear()
        every { ethereum.ethBlockNumber() } returns Mono.just(100.toBigInteger())
        balanceBatchCheckerHandler = BalanceBatchCheckerHandler(ethereum, checkerMetrics, props)
    }

    @Test
    fun `consume first event - ok`() = runBlocking<Unit> {
        every { ethereum.executeRaw(any()) } returns Mono.just(Response(1L, TextNode("5")))

        balanceBatchCheckerHandler.handle(erc20Event(91, 5))
        balanceBatchCheckerHandler.handle(erc20Event(92))
        balanceBatchCheckerHandler.handle(erc20Event(93))

        checkMetrics(3, 1, 0)
    }

    @Test
    fun `deduplicate event - ok`() = runBlocking<Unit> {
        every { ethereum.executeRaw(any()) } returns Mono.just(Response(1L, TextNode("15")))

        val event = erc20Event(91, 5)
        balanceBatchCheckerHandler.handle(event)
        balanceBatchCheckerHandler.handle(
            event.copy(
                balanceId = event.balanceId,
                balance = event.balance.copy(
                    lastUpdatedAt = event.balance.lastUpdatedAt?.minusSeconds(100),
                    balance = 15.toBigInteger()
                )
            )
        )
        balanceBatchCheckerHandler.handle(erc20Event(92))
        balanceBatchCheckerHandler.handle(erc20Event(93))

        checkMetrics(4, 1, 0)
    }

    @Test
    fun `check order - ok`() = runBlocking<Unit> {
        every { ethereum.executeRaw(any()) } returns Mono.just(Response(1L, TextNode("3")))

        balanceBatchCheckerHandler.handle(erc20Event(92))
        balanceBatchCheckerHandler.handle(erc20Event(93))
        balanceBatchCheckerHandler.handle(erc20Event(91, 3))

        checkMetrics(3, 1, 0)
    }

    @Test
    fun `check invalid - ok`() = runBlocking<Unit> {
        every { ethereum.executeRaw(any()) } returns Mono.just(Response(1L, TextNode("3")))

        balanceBatchCheckerHandler.handle(erc20Event(91, 5))
        balanceBatchCheckerHandler.handle(erc20Event(92))
        balanceBatchCheckerHandler.handle(erc20Event(93))

        checkMetrics(3, 1, 1)
    }

    @Test
    fun `check skipping checks - ok`() = runBlocking<Unit> {
        (1..10).forEach { balanceBatchCheckerHandler.handle(erc20Event(it)) }

        checkMetrics(10, 0, 0)
    }

    @Test
    fun `check releasing buffer - ok`() = runBlocking<Unit> {
        every { ethereum.executeRaw(any()) } returns Mono.just(Response(1L, TextNode("1")))
        (80..90).map {
            balanceBatchCheckerHandler.handle(erc20Event(it))
        }
        checkMetrics(11, 8, 0)
    }

    private fun erc20Event(blockNumber: Int, value: Int = 1, updated: Instant = now()) = Erc20BalanceUpdateEventDto(
        eventId = UUID.randomUUID().toString(),
        balanceId = BalanceId(AddressFactory.create(), AddressFactory.create()).toString(),
        balance = Erc20BalanceDto(
            contract = Address.apply(props.tokens.first()),
            owner = AddressFactory.create(),
            balance = value.toBigInteger(),
            lastUpdatedAt = updated,
            blockNumber = blockNumber.toLong()
        ),
        eventTimeMarks = null
    )

    private fun checkMetrics(incoming: Int, check: Int, invalid: Int) {
        assertThat(counter(CheckerMetrics.BALANCE_INCOMING).toInt()).isEqualTo(incoming)
        assertThat(counter(CheckerMetrics.BALANCE_CHECK).toInt()).isEqualTo(check)
        assertThat(counter(CheckerMetrics.BALANCE_INVALID).toInt()).isEqualTo(invalid)
    }

    private fun counter(name: String): Double {
        return registry.counter(name, listOf(ImmutableTag("blockchain", Blockchain.ETHEREUM.value))).count()
    }
}
