package com.rarible.protocol.order.core.converters

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.OrderActivityMatchSideDto.Type
import com.rarible.protocol.order.core.converters.dto.AssetDtoConverter
import com.rarible.protocol.order.core.converters.dto.OrderActivityConverter
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.PriceNormalizer
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class OrderActivityConverterTest {

    private val primeNormalizer = PriceNormalizer(mockk())
    private val assetDtoConverter = AssetDtoConverter(primeNormalizer)
    private val orderRepository: OrderRepository = mockk()
    private val orderActivityConverter = OrderActivityConverter(primeNormalizer, assetDtoConverter)

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
                    OrderActivityMatchDto.Type.SELL, Type.SELL, Type.BID
                ),
                Arguments.of(
                    event.copy(data = sideMatch.copy(make = eth, take = nft, adhoc = false)),
                    OrderActivityMatchDto.Type.ACCEPT_BID, Type.BID, Type.SELL
                ),

                Arguments.of(
                    event.copy(data = sideMatch.copy(make = nft, take = eth, adhoc = true)),
                    OrderActivityMatchDto.Type.ACCEPT_BID, Type.BID, Type.SELL
                ),
                Arguments.of(
                    event.copy(data = sideMatch.copy(make = eth, take = nft, adhoc = true)),
                    OrderActivityMatchDto.Type.SELL, Type.SELL, Type.BID
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
        leftType: Type, rightType: Type
    ) = runBlocking {
        val ac = OrderActivityResult.History(logEvent)
        val orderDto = orderActivityConverter.convert(ac) as OrderActivityMatchDto
        assertEquals(type, orderDto.type)
        assertEquals(leftType, orderDto.left.type)
        assertEquals(rightType, orderDto.right.type)
    }
}
