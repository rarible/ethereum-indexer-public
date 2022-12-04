package com.rarible.protocol.order.core.converters

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.dto.AssetDto
import com.rarible.protocol.dto.Erc721AssetTypeDto
import com.rarible.protocol.dto.EthAssetTypeDto
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.OrderActivityMatchSideDto
import com.rarible.protocol.dto.OrderActivityMatchSideDto.Type
import com.rarible.protocol.order.core.converters.dto.AssetDtoConverter
import com.rarible.protocol.order.core.converters.dto.OrderActivityConverter
import com.rarible.protocol.order.core.data.createLogEvent
import com.rarible.protocol.order.core.data.randomPoolTargetNftIn
import com.rarible.protocol.order.core.data.randomPoolTargetNftOut
import com.rarible.protocol.order.core.data.randomSellOnChainAmmOrder
import com.rarible.protocol.order.core.misc.toReversedEthereumLogRecord
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OrderActivityResult
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.PoolActivityResult
import com.rarible.protocol.order.core.model.toLogEventKey
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.pool.PoolHistoryRepository
import com.rarible.protocol.order.core.service.PriceNormalizer
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import scalether.domain.Address
import java.math.BigInteger
import java.util.stream.Stream

internal class OrderActivityConverterTest {

    private val primeNormalizer = PriceNormalizer(mockk())
    private val assetDtoConverter = AssetDtoConverter(primeNormalizer)
    private val orderRepository: OrderRepository = mockk()
    private val poolHistoryRepository = mockk<PoolHistoryRepository>()
    private val orderActivityConverter = OrderActivityConverter(primeNormalizer, assetDtoConverter, poolHistoryRepository)

    companion object {
        @JvmStatic
        fun logEvents(): Stream<Arguments> = run {
            val eth = Asset(EthAssetType, EthUInt256.ONE)
            val nft = Asset(Erc721AssetType(randomAddress(), EthUInt256.ONE), EthUInt256.ONE)
            val sideMatch = OrderSideMatch(
                hash = Word.apply(RandomUtils.nextBytes(32)),
                counterHash = Word.apply(RandomUtils.nextBytes(32)),
                side = OrderSide.LEFT,
                fill = EthUInt256.ONE,
                make = nft,
                take = eth,
                maker = randomAddress(),
                taker = randomAddress(),
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = null,
                takeValue = null
            )
            val event = LogEvent(
                data = sideMatch,
                address = randomAddress(),
                topic = Word.apply(RandomUtils.nextBytes(32)),
                transactionHash = Word.apply(RandomUtils.nextBytes(32)),
                index = RandomUtils.nextInt(),
                minorLogIndex = 0,
                status = LogEventStatus.CONFIRMED
            )
            Stream.of(
                Arguments.of(
                    event.copy(data = sideMatch.copy(make = nft, take = eth, adhoc = false)),
                    OrderActivityMatchDto.Type.SELL, Type.SELL, Type.BID,
                    false
                ),
                Arguments.of(
                    event.copy(data = sideMatch.copy(make = eth, take = nft, adhoc = false)),
                    OrderActivityMatchDto.Type.ACCEPT_BID, Type.BID, Type.SELL,
                    true
                ),

                Arguments.of(
                    event.copy(data = sideMatch.copy(make = nft, take = eth, adhoc = true)),
                    OrderActivityMatchDto.Type.ACCEPT_BID, Type.BID, Type.SELL,
                    false
                ),
                Arguments.of(
                    event.copy(data = sideMatch.copy(make = eth, take = nft, adhoc = true)),
                    OrderActivityMatchDto.Type.SELL, Type.SELL, Type.BID,
                    true
                )
            )
        }
    }

    @BeforeEach
    fun setup() {
        every { runBlocking { orderRepository.findById(any()) } } returns mockk()
    }

    @ParameterizedTest
    @MethodSource("logEvents")
    fun `should decode order match transaction input`(
        logEvent: LogEvent,
        type: OrderActivityMatchDto.Type,
        leftType: Type,
        rightType: Type,
        reverted: Boolean
    ) = runBlocking<Unit> {
        val ac = OrderActivityResult.History(logEvent)
        val orderDto = orderActivityConverter.convert(ac, reverted) as OrderActivityMatchDto

        assertThat(orderDto.type).isEqualTo(type)
        assertThat(orderDto.left.type).isEqualTo(leftType)
        assertThat(orderDto.right.type).isEqualTo(rightType)
        assertThat(orderDto.reverted).isEqualTo(reverted)
    }

    @Test
    fun `should convert pool nft in event`() = runBlocking<Unit> {
        val pool = randomSellOnChainAmmOrder().copy(currency = Address.ZERO())
        val event = randomPoolTargetNftIn()
        val logEvent = createLogEvent(event).toReversedEthereumLogRecord()
        val expectedCurrency = AssetDto(EthAssetTypeDto(), event.inputValue.value)
        val expectedNft = AssetDto(Erc721AssetTypeDto(event.collection, event.tokenIds.first().value), BigInteger.ONE)

        coEvery { poolHistoryRepository.getPoolCreateEvent(event.hash) } returns createLogEvent(pool)

        val activity = orderActivityConverter.convert(PoolActivityResult.History(logEvent))
        assertThat(activity).isNotNull
        assertThat(activity).isInstanceOf(OrderActivityMatchDto::class.java)

        with(activity as OrderActivityMatchDto) {
            assertThat(this.type).isEqualTo(OrderActivityMatchDto.Type.ACCEPT_BID)

            assertThat(this.left.hash).isEqualTo(pool.hash)
            assertThat(this.left.type).isEqualTo(Type.BID)
            assertThat(this.left.maker).isEqualTo(pool.data.poolAddress)
            assertThat(this.left.asset)
                .usingRecursiveComparison()
                .ignoringFields(AssetDto::valueDecimal.name)
                .isEqualTo(expectedCurrency)

            assertThat(this.right.hash).isEqualTo(Word.apply(ByteArray(32)))
            assertThat(this.right.type).isEqualTo(Type.SELL)
            assertThat(this.right.maker).isEqualTo(event.tokenRecipient)
            assertThat(this.right.asset)
                .usingRecursiveComparison()
                .ignoringFields(AssetDto::valueDecimal.name)
                .isEqualTo(expectedNft)
        }
    }

    @Test
    fun `should convert pool nft out event`() = runBlocking<Unit> {
        val pool = randomSellOnChainAmmOrder().copy(currency = Address.ZERO())
        val event = randomPoolTargetNftOut()
        val logEvent = createLogEvent(event).toReversedEthereumLogRecord()
        val expectedCurrency = AssetDto(EthAssetTypeDto(), event.outputValue.value)
        val expectedNft = AssetDto(Erc721AssetTypeDto(event.collection, event.tokenIds.first().value), BigInteger.ONE)

        coEvery { poolHistoryRepository.getPoolCreateEvent(event.hash) } returns createLogEvent(pool)

        val activity = orderActivityConverter.convert(PoolActivityResult.History(logEvent))
        assertThat(activity).isNotNull
        assertThat(activity).isInstanceOf(OrderActivityMatchDto::class.java)

        with(activity as OrderActivityMatchDto) {
            assertThat(this.type).isEqualTo(OrderActivityMatchDto.Type.SELL)

            assertThat(this.left.hash).isEqualTo(pool.hash)
            assertThat(this.left.type).isEqualTo(Type.SELL)
            assertThat(this.left.maker).isEqualTo(pool.data.poolAddress)
            assertThat(this.left.asset)
                .usingRecursiveComparison()
                .ignoringFields(AssetDto::valueDecimal.name)
                .isEqualTo(expectedNft)

            assertThat(this.right.hash).isEqualTo(Word.apply(ByteArray(32)))
            assertThat(this.right.type).isEqualTo(Type.BID)
            assertThat(this.right.maker).isEqualTo(event.recipient)
            assertThat(this.right.asset)
                .usingRecursiveComparison()
                .ignoringFields(AssetDto::valueDecimal.name)
                .isEqualTo(expectedCurrency)
        }
    }
}
