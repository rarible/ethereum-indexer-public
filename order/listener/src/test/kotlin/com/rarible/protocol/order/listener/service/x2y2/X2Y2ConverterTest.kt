package com.rarible.protocol.order.listener.service.x2y2

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderX2Y2DataV1
import com.rarible.protocol.order.core.model.token
import com.rarible.x2y2.client.model.ApiListResponse
import com.rarible.x2y2.client.model.Order
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class X21Y2ConverterTest {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private val orders = ClassPathResource("json/x2y2/orders.json").inputStream.use {
        mapper.readValue(it, object : TypeReference<ApiListResponse<Order>>() {})
    }.data

    private val converter = X2Y2OrderConverter(
        mockk {
            coEvery { withUpdatedPrices(orderVersion = any()) } returnsArgument 0
        }
    )

    @Test
    internal fun `should convert order`() {
        runBlocking {
            val order = orders.shuffled()[Random.nextInt(0, 19)]
            val converted = converter.convertOrder(order)
            assertThat(converted.hash).isEqualTo(order.itemHash)
            assertThat(converted.maker).isEqualTo(order.maker)
            assertThat(converted.taker).isNull()
            assertThat(converted.make.type).isExactlyInstanceOf(Erc721AssetType::class.java)
            assertThat((converted.make.type as Erc721AssetType).token).isEqualTo(order.token?.contract)
            assertThat((converted.make.type as Erc721AssetType).tokenId.value).isEqualTo(order.token?.tokenId)
            assertThat(converted.take.value.value).isEqualTo(order.price)
            assertThat(converted.take.type.token).isEqualTo(order.currency)
            assertThat(converted.type).isEqualTo(OrderType.X2Y2)
            assertThat(converted.start).isEqualTo(order.createdAt.epochSecond)
            assertThat(converted.end).isEqualTo(order.endAt.epochSecond)
            assertThat(converted.createdAt).isEqualTo(order.createdAt)
            assertThat(converted.data).isExactlyInstanceOf(OrderX2Y2DataV1::class.java)
            val d = converted.data as OrderX2Y2DataV1
            assertThat(d.itemHash).isEqualTo(order.itemHash)
            assertThat(d.isCollectionOffer).isEqualTo(order.isCollectionOffer)
            assertThat(d.isBundle).isEqualTo(order.isBundle)
            assertThat(d.side).isEqualTo(order.side)
            assertThat(d.orderId).isEqualTo(order.id)
        }
    }
}
