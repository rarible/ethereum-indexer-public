package com.rarible.protocol.nftorder.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.*
import com.rarible.protocol.dto.mapper.ContinuationMapper
import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.nftorder.api.client.NftOrderActivityControllerApi
import com.rarible.protocol.nftorder.api.test.AbstractFunctionalTest
import com.rarible.protocol.nftorder.api.test.FunctionalTest
import com.rarible.protocol.nftorder.listener.test.mock.data.randomOrderBidActivityDto
import com.rarible.protocol.nftorder.listener.test.mock.data.randomTransferDto
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Mono
import scalether.domain.Address
import java.time.Instant
import java.util.*

@FunctionalTest
internal class ActivityControllerFt : AbstractFunctionalTest() {

    @Autowired
    private lateinit var nftActivityControllerApi: NftActivityControllerApi

    @Autowired
    private lateinit var orderActivityControllerApi: OrderActivityControllerApi

    @Autowired
    private lateinit var nftOrderActivityControllerApi: NftOrderActivityControllerApi

    private val nftActivity1 = randomTransferDto().copy(date = Instant.ofEpochMilli(115))
    private val nftActivity2 = randomTransferDto().copy(date = Instant.ofEpochMilli(110))
    private val nftActivity3 = randomTransferDto().copy(date = Instant.ofEpochMilli(105))
    private val nftActivity4 = randomTransferDto().copy(date = Instant.ofEpochMilli(100))

    private val orderActivity1 = randomOrderBidActivityDto().copy(date = Instant.ofEpochMilli(118))
    private val orderActivity2 = randomOrderBidActivityDto().copy(date = Instant.ofEpochMilli(112))
    private val orderActivity3 = randomOrderBidActivityDto().copy(date = Instant.ofEpochMilli(108))
    private val orderActivity4 = randomOrderBidActivityDto().copy(date = Instant.ofEpochMilli(102))

    @Test
    fun `should aggregate all activities from nft and orders`() = runBlocking<Unit> {
        val filterContinuation = UUID.randomUUID().toString()
        val filterSize = 8
        val sort = ActivitySortDto.LATEST_FIRST

        val types = listOf(
            ActivityFilterAllTypeDto.TRANSFER,
            ActivityFilterAllTypeDto.MINT,
            ActivityFilterAllTypeDto.BURN,
            ActivityFilterAllTypeDto.SELL,
            ActivityFilterAllTypeDto.BID,
            ActivityFilterAllTypeDto.LIST
        )
        val nftFilter = NftActivityFilterAllDto(
            listOf(
                NftActivityFilterAllDto.Types.TRANSFER,
                NftActivityFilterAllDto.Types.MINT,
                NftActivityFilterAllDto.Types.BURN
            )
        )
        val orderFilter = OrderActivityFilterAllDto(
            listOf(
                OrderActivityFilterAllDto.Types.MATCH,
                OrderActivityFilterAllDto.Types.BID,
                OrderActivityFilterAllDto.Types.LIST
            )
        )

        coEvery {
            nftActivityControllerApi.getNftActivities(
                eq(nftFilter),
                eq(filterContinuation),
                eq(filterSize),
                eq(sort)
            )
        } returns Mono.just(
            NftActivitiesDto(
                continuation = UUID.randomUUID().toString(),
                items = listOf(nftActivity1, nftActivity2, nftActivity3, nftActivity4)
            )
        )
        coEvery {
            orderActivityControllerApi.getOrderActivities(
                eq(orderFilter),
                eq(filterContinuation),
                eq(filterSize),
                eq(sort)
            )
        } returns Mono.just(
            OrderActivitiesDto(
                continuation = UUID.randomUUID().toString(),
                items = listOf(orderActivity1, orderActivity2, orderActivity3, orderActivity4)
            )
        )

        val result =
            nftOrderActivityControllerApi
                .getNftOrderAllActivities(types, filterContinuation, filterSize, sort).awaitFirst()
        assertThat(result.items).hasSize(8)
        assertThat(result.continuation).isEqualTo(ContinuationMapper.toString(nftActivity4))
        assertThat(result.items).containsExactly(
            orderActivity1,
            nftActivity1,
            orderActivity2,
            nftActivity2,
            orderActivity3,
            nftActivity3,
            orderActivity4,
            nftActivity4
        )
    }

    @Test
    fun `should aggregate by item activities from nft and orders`() = runBlocking<Unit> {
        val filterContinuation = UUID.randomUUID().toString()
        val filterSize = 8
        val sort = ActivitySortDto.LATEST_FIRST

        val token = Address.FOUR()
        val tokenId = (1..1000).random().toBigInteger()

        val types = listOf(
            ActivityFilterByItemTypeDto.TRANSFER,
            ActivityFilterByItemTypeDto.MINT,
            ActivityFilterByItemTypeDto.BURN,
            ActivityFilterByItemTypeDto.MATCH,
            ActivityFilterByItemTypeDto.BID,
            ActivityFilterByItemTypeDto.LIST
        )
        val nftFilter = NftActivityFilterByItemDto(
            token,
            tokenId,
            listOf(
                NftActivityFilterByItemDto.Types.TRANSFER,
                NftActivityFilterByItemDto.Types.MINT,
                NftActivityFilterByItemDto.Types.BURN
            )
        )
        val orderFilter = OrderActivityFilterByItemDto(
            token,
            tokenId,
            listOf(
                OrderActivityFilterByItemDto.Types.MATCH,
                OrderActivityFilterByItemDto.Types.BID,
                OrderActivityFilterByItemDto.Types.LIST
            )
        )
        coEvery {
            nftActivityControllerApi.getNftActivities(
                eq(nftFilter),
                eq(filterContinuation),
                eq(filterSize),
                eq(sort)
            )
        } returns Mono.just(
            NftActivitiesDto(
                continuation = UUID.randomUUID().toString(),
                items = listOf(nftActivity1, nftActivity2, nftActivity3, nftActivity4)
            )
        )
        coEvery {
            orderActivityControllerApi.getOrderActivities(
                eq(orderFilter),
                eq(filterContinuation),
                eq(filterSize),
                eq(sort)
            )
        } returns Mono.just(
            OrderActivitiesDto(
                continuation = UUID.randomUUID().toString(),
                items = listOf(orderActivity1, orderActivity2, orderActivity3, orderActivity4)
            )
        )

        val result =
            nftOrderActivityControllerApi.getNftOrderActivitiesByItem(
                types,
                token.hex(),
                tokenId.toString(),
                filterContinuation,
                filterSize,
                sort
            ).awaitFirst()
        assertThat(result.items).hasSize(8)
        assertThat(result.continuation).isEqualTo(ContinuationMapper.toString(nftActivity4))
        assertThat(result.items).containsExactly(
            orderActivity1,
            nftActivity1,
            orderActivity2,
            nftActivity2,
            orderActivity3,
            nftActivity3,
            orderActivity4,
            nftActivity4
        )
    }

    @Test
    fun `should aggregate by collection activities from nft and orders`() = runBlocking<Unit> {
        val filterContinuation = UUID.randomUUID().toString()
        val filterSize = 8
        val sort = ActivitySortDto.LATEST_FIRST

        val token = Address.FOUR()

        val types = listOf(
            ActivityFilterByCollectionTypeDto.TRANSFER,
            ActivityFilterByCollectionTypeDto.MINT,
            ActivityFilterByCollectionTypeDto.BURN,
            ActivityFilterByCollectionTypeDto.MATCH,
            ActivityFilterByCollectionTypeDto.BID,
            ActivityFilterByCollectionTypeDto.LIST
        )
        val nftFilter = NftActivityFilterByCollectionDto(
            token,
            listOf(
                NftActivityFilterByCollectionDto.Types.TRANSFER,
                NftActivityFilterByCollectionDto.Types.MINT,
                NftActivityFilterByCollectionDto.Types.BURN
            )
        )
        val orderFilter = OrderActivityFilterByCollectionDto(
            token,
            listOf(
                OrderActivityFilterByCollectionDto.Types.MATCH,
                OrderActivityFilterByCollectionDto.Types.BID,
                OrderActivityFilterByCollectionDto.Types.LIST
            )
        )
        coEvery {
            nftActivityControllerApi.getNftActivities(
                eq(nftFilter),
                eq(filterContinuation),
                eq(filterSize),
                eq(sort)
            )
        } returns Mono.just(
            NftActivitiesDto(
                continuation = UUID.randomUUID().toString(),
                items = listOf(nftActivity1, nftActivity2, nftActivity3, nftActivity4)
            )
        )
        coEvery {
            orderActivityControllerApi.getOrderActivities(
                eq(orderFilter),
                eq(filterContinuation),
                eq(filterSize),
                eq(sort)
            )
        } returns Mono.just(
            OrderActivitiesDto(
                continuation = UUID.randomUUID().toString(),
                items = listOf(orderActivity1, orderActivity2, orderActivity3, orderActivity4)
            )
        )

        val result =
            nftOrderActivityControllerApi.getNftOrderActivitiesByCollection(
                types,
                token.hex(),
                filterContinuation,
                filterSize,
                sort
            )
                .awaitFirst()
        assertThat(result.items).hasSize(8)
        assertThat(result.continuation).isEqualTo(ContinuationMapper.toString(nftActivity4))
        assertThat(result.items).containsExactly(
            orderActivity1,
            nftActivity1,
            orderActivity2,
            nftActivity2,
            orderActivity3,
            nftActivity3,
            orderActivity4,
            nftActivity4
        )
    }

    @Test
    fun `should aggregate by user activities from nft and orders`() = runBlocking<Unit> {
        val filterContinuation = UUID.randomUUID().toString()
        val filterSize = 8
        val sort = ActivitySortDto.LATEST_FIRST
        val from = nowMillis().epochSecond
        val to = nowMillis().epochSecond + 1

        val user = listOf(Address.FOUR(), Address.ONE())

        val types = listOf(
            ActivityFilterByUserTypeDto.TRANSFER_TO,
            ActivityFilterByUserTypeDto.TRANSFER_FROM,
            ActivityFilterByUserTypeDto.MINT,
            ActivityFilterByUserTypeDto.BURN,
            ActivityFilterByUserTypeDto.GET_BID,
            ActivityFilterByUserTypeDto.MAKE_BID,
            ActivityFilterByUserTypeDto.SELL,
            ActivityFilterByUserTypeDto.BUY,
            ActivityFilterByUserTypeDto.LIST
        )
        val nftFilter = NftActivityFilterByUserDto(
            user,
            listOf(
                NftActivityFilterByUserDto.Types.TRANSFER_TO,
                NftActivityFilterByUserDto.Types.TRANSFER_FROM,
                NftActivityFilterByUserDto.Types.MINT,
                NftActivityFilterByUserDto.Types.BURN
            ),
            Instant.ofEpochSecond(from),
            Instant.ofEpochSecond(to)
        )
        val orderFilter = OrderActivityFilterByUserDto(
            user,
            listOf(
                OrderActivityFilterByUserDto.Types.GET_BID,
                OrderActivityFilterByUserDto.Types.MAKE_BID,
                OrderActivityFilterByUserDto.Types.SELL,
                OrderActivityFilterByUserDto.Types.BUY,
                OrderActivityFilterByUserDto.Types.LIST
            ),
            Instant.ofEpochSecond(from),
            Instant.ofEpochSecond(to)
        )
        coEvery {
            nftActivityControllerApi.getNftActivities(
                eq(nftFilter),
                eq(filterContinuation),
                eq(filterSize),
                eq(sort)
            )
        } returns Mono.just(
            NftActivitiesDto(
                continuation = UUID.randomUUID().toString(),
                items = listOf(nftActivity1, nftActivity2, nftActivity3, nftActivity4)
            )
        )
        coEvery {
            orderActivityControllerApi.getOrderActivities(
                eq(orderFilter),
                eq(filterContinuation),
                eq(filterSize),
                eq(sort)
            )
        } returns Mono.just(
            OrderActivitiesDto(
                continuation = UUID.randomUUID().toString(),
                items = listOf(orderActivity1, orderActivity2, orderActivity3, orderActivity4)
            )
        )

        val result =
            nftOrderActivityControllerApi.getNftOrderActivitiesByUser(
                types,
                user,
                from,
                to,
                filterContinuation,
                filterSize,
                sort
            ).awaitFirst()
        assertThat(result.items).hasSize(8)
        assertThat(result.continuation).isEqualTo(ContinuationMapper.toString(nftActivity4))
        assertThat(result.items).containsExactly(
            orderActivity1,
            nftActivity1,
            orderActivity2,
            nftActivity2,
            orderActivity3,
            nftActivity3,
            orderActivity4,
            nftActivity4
        )
    }
}
