package com.rarible.protocol.nft.listener.service.checker

import com.fasterxml.jackson.databind.node.TextNode
import com.rarible.ethereum.domain.Blockchain
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.nft.core.data.createRandomOwnershipId
import com.rarible.protocol.nft.core.metric.BaseMetrics
import com.rarible.protocol.nft.core.metric.CheckerMetrics
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
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
import java.util.UUID

internal class OwnershipCheckerTest {

    private val ethereum: MonoEthereum = mockk()
    private val registry = SimpleMeterRegistry()
    private val repository: TokenRepository = mockk()
    private val metrics: CheckerMetrics = CheckerMetrics(Blockchain.ETHEREUM, registry)
    private val props = NftListenerProperties()

    private lateinit var ownershipBatchCheckerHandler: OwnershipBatchCheckerHandler

    @BeforeEach
    fun setUp() {
        clearMocks(ethereum)
        clearMocks(repository)
        registry.clear()
        every { ethereum.ethBlockNumber() } returns Mono.just(100.toBigInteger())
        ownershipBatchCheckerHandler = OwnershipBatchCheckerHandler(props, ethereum, repository, metrics)
    }

    @Test
    fun `check erc721 - ok`() = runBlocking<Unit> {
        val ownershipId = createRandomOwnershipId()
        every { ethereum.executeRaw(any())
        } returns Mono.just(Response(1L, TextNode("0x0000000000000000000000${ownershipId.owner}")))
        every { repository.findById(any()) } returns Mono.just(token(AddressFactory.create(), TokenStandard.ERC721))

        ownershipBatchCheckerHandler.handle(listOf(ownershipUpdateEvent(ownershipId, 1, 91)))
        ownershipBatchCheckerHandler.handle(listOf(ownershipUpdateEvent(ownershipId, 1, 92)))
        ownershipBatchCheckerHandler.handle(listOf(ownershipUpdateEvent(ownershipId, 1, 93)))

        checkMetrics(3, 1, 0, 0)
    }

    @Test
    fun `check erc1155 - ok`() = runBlocking<Unit> {
        val ownershipId = createRandomOwnershipId()
        every { ethereum.executeRaw(any())
        } returns Mono.just(Response(1L, TextNode("5")))
        every { repository.findById(any()) } returns Mono.just(token(AddressFactory.create(), TokenStandard.ERC1155))

        ownershipBatchCheckerHandler.handle(listOf(ownershipUpdateEvent(ownershipId, 5, 91)))
        ownershipBatchCheckerHandler.handle(listOf(ownershipUpdateEvent(ownershipId, 1, 92)))
        ownershipBatchCheckerHandler.handle(listOf(ownershipUpdateEvent(ownershipId, 1, 93)))

        checkMetrics(3, 1, 0, 0)
    }

    @Test
    fun `check erc721 - fail`() = runBlocking<Unit> {
        val ownershipId = createRandomOwnershipId()
        every { ethereum.executeRaw(any())
        } returns Mono.just(Response(1L, TextNode("0x0000000000000000000000${AddressFactory.create()}")))
        every { repository.findById(any()) } returns Mono.just(token(AddressFactory.create(), TokenStandard.ERC721))

        ownershipBatchCheckerHandler.handle(listOf(ownershipUpdateEvent(ownershipId, 1, 91)))
        ownershipBatchCheckerHandler.handle(listOf(ownershipUpdateEvent(ownershipId, 1, 92)))
        ownershipBatchCheckerHandler.handle(listOf(ownershipUpdateEvent(ownershipId, 1, 93)))

        checkMetrics(3, 0, 1, 0)
    }

    @Test
    fun `check erc721 - delete`() = runBlocking<Unit> {
        val ownershipId = createRandomOwnershipId()
        every { ethereum.executeRaw(any())
        } returns Mono.just(Response(1L, TextNode("0x0000000000000000000000${AddressFactory.create()}")))
        every { repository.findById(any()) } returns Mono.just(token(AddressFactory.create(), TokenStandard.ERC721))

        ownershipBatchCheckerHandler.handle(listOf(ownershipDeleteEvent(ownershipId, 91)))
        ownershipBatchCheckerHandler.handle(listOf(ownershipUpdateEvent(ownershipId, 1, 92)))
        ownershipBatchCheckerHandler.handle(listOf(ownershipUpdateEvent(ownershipId, 1, 93)))

        checkMetrics(3, 1, 0, 0)
    }

    private fun ownershipUpdateEvent(ownershipId: OwnershipId, value: Int, blockNumber: Long) = NftOwnershipUpdateEventDto(
        eventId = UUID.randomUUID().toString(),
        ownershipId = ownershipId.toString(),
        blockNumber = blockNumber,
        ownership = NftOwnershipDto(
            contract = ownershipId.token,
            creators = emptyList(),
            date = Instant.now(),
            id = ownershipId.toString(),
            lazyValue = EthUInt256.ZERO.value,
            owner = ownershipId.owner,
            pending = emptyList(),
            tokenId = ownershipId.tokenId.value,
            value = value.toBigInteger()
        )
    )

    private fun ownershipDeleteEvent(ownershipId: OwnershipId, blockNumber: Long) = NftOwnershipDeleteEventDto(
        eventId = UUID.randomUUID().toString(),
        ownershipId = ownershipId.toString(),
        blockNumber = blockNumber
    )

    fun token(id: Address, standard: TokenStandard): Token {
        return Token(
            id = id,
            name = id.toString(),
            standard = standard,
        )
    }

    private fun checkMetrics(incoming: Int, success: Int, fail: Int, skipped: Int) {
        assertThat(counterIncoming()).isEqualTo(incoming)
        assertThat(counter(CheckerMetrics.SUCCESS_TAG)).isEqualTo(success)
        assertThat(counter(CheckerMetrics.FAIL_TAG)).isEqualTo(fail)
        assertThat(counter(CheckerMetrics.SKIPPED_TAG)).isEqualTo(skipped)
    }

    private fun counter(name: String): Int {
        return registry.counter(CheckerMetrics.OWNERSHIPS_CHECKED, listOf(
            ImmutableTag(BaseMetrics.BLOCKCHAIN, Blockchain.ETHEREUM.value.lowercase()),
            ImmutableTag(BaseMetrics.STATUS, name))).count().toInt()
    }

    private fun counterIncoming(): Int {
        return registry.counter(CheckerMetrics.OWNERSHIPS_INCOMING, listOf(
            ImmutableTag(BaseMetrics.BLOCKCHAIN, Blockchain.ETHEREUM.value.lowercase()))).count().toInt()
    }
}
