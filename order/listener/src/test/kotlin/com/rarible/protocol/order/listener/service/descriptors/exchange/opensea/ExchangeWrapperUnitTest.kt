package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.toBinary
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.service.CallDataEncoder
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderEventConverter
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderParser
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import scala.collection.JavaConverters
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

class ExchangeWrapperUnitTest {

    private val traceCallService: TraceCallService = mockk()
    private val priceUpdateService: PriceUpdateService = mockk()
    private val prizeNormalizer: PriceNormalizer = mockk()

    private val openSeaOrderParser = OpenSeaOrderParser(
        traceCallService = traceCallService,
        callDataEncoder = CallDataEncoder()
    )

    private val descriptor = WyvernExchangeOrderMatchDescriptor(
        exchangeContractAddresses = OrderIndexerProperties.ExchangeContractAddresses(
            v1 = Address.ZERO(),
            v1Old = null,
            v2 = Address.ZERO(),
            openSeaV1 = Address.ZERO(),
            openSeaV2 = Address.ZERO(),
            cryptoPunks = Address.ZERO(),
            zeroEx = Address.ZERO()
        ),
        openSeaOrdersSideMatcher = OpenSeaOrderEventConverter(
            priceUpdateService = priceUpdateService,
            prizeNormalizer = prizeNormalizer,
            callDataEncoder = CallDataEncoder()
        ),
        openSeaOrderParser = openSeaOrderParser
    )

    @Test
    fun `recover buy maker and origin fees`() = runBlocking {

        val feeSdkString = "0x7ad2607e00000000000000000000000000000000000000000000000000000000000000400000000000000000000000000000000000000000000000000000000000000c40000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000005af3107a400000000000000000000000000000000000000000000000000000000000000000600000000000000000000000000000000000000000000000000000000000000b64ab834bab000000000000000000000000dd54d660178b28f6033a953b0e55073cfa7e374400000000000000000000000092ce36ceae648d6a57316cb67bd40199737c17a40000000000000000000000002cf5490f75d96bd8a0aefee96c1ea383d3409efc000000000000000000000000000000000000000000000000000000000000000000000000000000000000000045b594792a5cdc008d0de1c1d69faa3d16b3ddc100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000dd54d660178b28f6033a953b0e55073cfa7e37440000000000000000000000002cf5490f75d96bd8a0aefee96c1ea383d3409efc00000000000000000000000000000000000000000000000000000000000000000000000000000000000000005b3256965e7c3cf26e11fcaf296dfc8807c0107300000000000000000000000045b594792a5cdc008d0de1c1d69faa3d16b3ddc10000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000fa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005af3107a4000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000625c10520000000000000000000000000000000000000000000000000000000062839db0053ff234dd34ea72f99c7cbc09a8156bd01b93b385f19721cea64969c2d2c88800000000000000000000000000000000000000000000000000000000000000fa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005af3107a4000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000625c10520000000000000000000000000000000000000000000000000000000062839db0053ff234dd34ea72f99c7cbc09a8156bd01b93b385f19721cea64969c2d2c8880000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000006a000000000000000000000000000000000000000000000000000000000000007c000000000000000000000000000000000000000000000000000000000000008e00000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000b200000000000000000000000000000000000000000000000000000000000000b40000000000000000000000000000000000000000000000000000000000000001b000000000000000000000000000000000000000000000000000000000000001c00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000abd3f4535786b9a88c30f42d860988bb2d1dd46dd2d46a384d0551fd4f8169e627fe8b95335d9048f073ce4c853589d36d4280d9bc10d072b8ecbdbf305f99bbb4771912bfddf60f2d7afaf79821605a9782933ab226beea4e3fbe6e0c81c58400000000000000000000000000000000000000000000000000000000000000e4fb16a59500000000000000000000000000000000000000000000000000000000000000000000000000000000000000004c9d38c11c1c72bdcb71199b82e8ba869599e099000000000000000000000000c8f3e6f0391c51ca58a72487171ff99adeb8d15a0000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e4fb16a5950000000000000000000000002cf5490f75d96bd8a0aefee96c1ea383d3409efc0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c8f3e6f0391c51ca58a72487171ff99adeb8d15a0000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e400000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e4000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000012c4c9d38c11c1c72bdcb71199b82e8ba869599e099"

        coEvery { traceCallService.findAllRequiredCallInputs(any(), any(), any(), any()) } returns listOf(feeSdkString.toBinary())
        coEvery { priceUpdateService.getAssetsUsdValue(any(), any(), any()) } returns null
        coEvery { priceUpdateService.getAssetsUsdValue(any(), any(), any()) } returns null
        coEvery { prizeNormalizer.normalize(any()) } returns BigDecimal.ZERO
        coEvery { prizeNormalizer.normalize(any()) } returns BigDecimal.ZERO

        val log = Log.apply(
            /* logIndex = */ BigInteger.valueOf(4),
            /* transactionIndex = */ BigInteger.valueOf(0),
            /* transactionHash = */ Word.apply("0x3859cb27661715d82eb0917f5b75eb7bcb328645d96dcaf23432a898928f332f"),
            /* blockHash = */ Word.apply("0xd2a24c50832fa2a9d1e5909723266e53eba890c8d1c5aa7bada07bf521c5c32f"),
            /* blockNumber = */ BigInteger.valueOf(176),
            /* address = */ Address.apply("0x1e47a27e285d82b308c60328c6e0530d9c24f26d"),
            /* data = */ "0x00000000000000000000000000000000000000000000000000000000000000004648d60ac877da2d22158abc7bc20966bbe551a1a703612aeb0c67ebb3a5b3c5000000000000000000000000000000000000000000000000000000000000000a".toBinary(),
            /* removed = */ false,
            /* topics = */ JavaConverters.collectionAsScalaIterable(listOf(
                Word.apply("0xc4109843e0b7d514e4c093114b863f8e7d8d9a458c372cd51bfe526b588006c9"),
                Word.apply("0x0000000000000000000000005ddbe22b6f861ce101d122a518601269ddc53b06"),
                Word.apply("0x00000000000000000000000095e4a09b554909badfd50044779896968ac3d7c9"),
                Word.apply("0xb4771912bfddf60f2d7afaf79821605a9782933ab226beea4e3fbe6e0c81c584"),
            )).toList(),
            /* type = */ "mined"
        )

        val transaction = Transaction(
            /* hash = */ Word.apply(/* bytes = */ "0x3859cb27661715d82eb0917f5b75eb7bcb328645d96dcaf23432a898928f332f"),
            /* nonce = */ BigInteger.valueOf(2),
            /* blockHash = */ Word.apply("0xd2a24c50832fa2a9d1e5909723266e53eba890c8d1c5aa7bada07bf521c5c32f"),
            /* blockNumber = */ BigInteger.valueOf(176),
            /* creates = */ null,
            /* transactionIndex = */ BigInteger.valueOf(0),
            /* from = */ Address.apply("0x95e4a09b554909badfd50044779896968ac3d7c9"),
            /* to = */ Address.apply("0x1e47a27e285d82b308c60328c6e0530d9c24f26d"),
            /* value = */ BigInteger.valueOf(0),
            /* gasPrice = */ BigInteger.valueOf(0),
            /* gas = */ BigInteger.valueOf(8000000),
            /* input = */ "0xab834bab0000000000000000000000001e47a27e285d82b308c60328c6e0530d9c24f26d00000000000000000000000095e4a09b554909badfd50044779896968ac3d7c90000000000000000000000005ddbe22b6f861ce101d122a518601269ddc53b060000000000000000000000000000000000000000000000000000000000000000000000000000000000000000009ec91297a15af0581f3af51c9ed8323a2e93c600000000000000000000000000000000000000000000000000000000000000000000000000000000000000007843377e895ed20c4356360e8f6f3a9f7c8e2b580000000000000000000000001e47a27e285d82b308c60328c6e0530d9c24f26d0000000000000000000000005ddbe22b6f861ce101d122a518601269ddc53b0600000000000000000000000000000000000000000000000000000000000000000000000000000000000000001e47a27e285d82b308c60328c6e0530d9c24f26d000000000000000000000000009ec91297a15af0581f3af51c9ed8323a2e93c600000000000000000000000000000000000000000000000000000000000000000000000000000000000000007843377e895ed20c4356360e8f6f3a9f7c8e2b5800000000000000000000000000000000000000000000000000000000000000fa0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000064000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000626b7f4800000000000000000000000000000000000000000000000000000000000000004558d88b5daf4dc303fc47ad6d07406a5787f779affd510a5d812a167cbb264100000000000000000000000000000000000000000000000000000000000000fa000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000626b7f3b0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000006a000000000000000000000000000000000000000000000000000000000000007c000000000000000000000000000000000000000000000000000000000000008e00000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000b200000000000000000000000000000000000000000000000000000000000000b40000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000001cb10f8712f294fd92a94e65524d8990fff8697188b68d3ad24eb1ec1a6e4201750e3cc47b9edfb8f865f13ef491b74ac08bd12e4df43b0f03d8bc059794a3a0c7b10f8712f294fd92a94e65524d8990fff8697188b68d3ad24eb1ec1a6e4201750e3cc47b9edfb8f865f13ef491b74ac08bd12e4df43b0f03d8bc059794a3a0c7b4771912bfddf60f2d7afaf79821605a9782933ab226beea4e3fbe6e0c81c58400000000000000000000000000000000000000000000000000000000000000e4fb16a595000000000000000000000000000000000000000000000000000000000000000000000000000000000000000095e4a09b554909badfd50044779896968ac3d7c90000000000000000000000000424cb476001d70b12861f65491feeebc26c25070000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e4fb16a5950000000000000000000000005ddbe22b6f861ce101d122a518601269ddc53b0600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000424cb476001d70b12861f65491feeebc26c25070000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e400000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e4000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".toBinary()
        )

        val date = Instant.now()
        val index = 0
        val totalLogs = 1

        val orders: List<OrderSideMatch> = descriptor.convert(
            log = log,
            transaction = transaction,
            timestamp = date.toEpochMilli(),
            index = index,
            totalLogs = totalLogs
        ).asFlow().toList()

        val buyOrder = orders[0]
        val sellOrder = orders[1]

        val expectedOriginsFees = listOf(Part(account = Address.apply("0x4c9d38c11c1c72bdcb71199b82e8ba869599e099"), value = EthUInt256.of(300)))
        assertEquals(Address.apply("0x4c9d38c11c1c72bdcb71199b82e8ba869599e099"), buyOrder.maker)
        assertEquals(expectedOriginsFees, buyOrder.originFees)
        assertEquals(expectedOriginsFees, sellOrder.originFees)
    }
}
