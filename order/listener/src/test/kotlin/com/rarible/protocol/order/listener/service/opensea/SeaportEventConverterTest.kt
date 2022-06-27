package com.rarible.protocol.order.listener.service.opensea

import com.rarible.protocol.contracts.seaport.v1.events.OrderFulfilledEvent
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.java.Lists
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

internal class SeaportEventConverterTest {
    private val priceUpdateService = mockk<PriceUpdateService>()
    private val prizeNormalizer = mockk<PriceNormalizer>()

    private val converter = SeaportEventConverter(
        priceUpdateService,
        prizeNormalizer
    )

    @Test
    fun `should convert basic sell OrderFulfilledEvent`() = runBlocking<Unit> {
        val log = log(
            "0xef032f06329eb259260d7760927fe547ad69e32223cf67901e1782d5d63ff1420000000000000000000000003c4e47b1be41100b8fd839334fe840469c722ab900000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000120000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000020000000000000000000000001bb08f4c63e891049fbb716fe4636392e32b4f7c0000000000000000000000000000000000000000000000000000000000000302000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000030000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000019945ca2620000000000000000000000000000d0df1aa764f1650184ffd549648dd84964ba00970000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000b5e620f480000000000000000000000000008de9c5a032463c561423387a9648c5c7bcc5bc90000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000221b262dd800000000000000000000000000062f5b75b11b38a008a1642e6434c91df734170be",
            listOf(
                Word.apply("0x9d9af8e38d66c62e2c12f0225249fd9d721c54b83f48d9352c97c6cacdcb6f31"),
                Word.apply("0x000000000000000000000000d0df1aa764f1650184ffd549648dd84964ba0097"),
                Word.apply("0x000000000000000000000000004c00500000ad104d7dbd00e3ae0a5c00560c00")
            )
        )
        coEvery { priceUpdateService.getAssetsUsdValue(any(), any(), any()) } returns null
        coEvery { prizeNormalizer.normalize(any()) } returns BigDecimal.ONE
        val event = OrderFulfilledEvent.apply(log)
        val matches = converter.convert(event, Instant.now())
        assertThat(matches).hasSize(2)
    }

    private fun log(data: String, topics: kotlin.collections.List<Word>) = Log(
        BigInteger.ONE, // logIndex
        BigInteger.TEN, // transactionIndex
        Word.apply(ByteArray(32)), // transactionHash
        Word.apply(ByteArray(32)), // blockHash
        BigInteger.ZERO, // blockNumber
        Address.ZERO(), // address
        Binary.apply( // data
            data
        ),
        false, // removed
        Lists.toScala( // topics
            topics
        ),
        "" // type
    )
}