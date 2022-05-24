package com.rarible.protocol.order.api.controller

import com.rarible.protocol.dto.AuctionActivitiesSyncTypesDto
import com.rarible.protocol.dto.AuctionActivityDto
import com.rarible.protocol.dto.SyncSortDto
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.data.createAuctionLogEvent
import com.rarible.protocol.order.core.data.createOffchainHistoryEvent
import com.rarible.protocol.order.core.data.randomAuction
import com.rarible.protocol.order.core.data.randomAuctionCreated
import com.rarible.protocol.order.core.model.AuctionHistoryType
import com.rarible.protocol.order.core.model.AuctionOffchainHistory
import com.rarible.protocol.order.core.model.OnChainAuction
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class AuctionActivityControllerIt : AbstractIntegrationTest()  {

    @Autowired
    private lateinit var controller: AuctionActivityController

    @Test
    fun `should get all order activities using pagination desc`() = runBlocking<Unit> {
        val activityQuantities = 30 //must be even
        val ordersChunk = 5

        fillRepositories(activityQuantities)

        var continuation : String? = null
        var pageCounter = 0
        val receivedOrders = mutableListOf<AuctionActivityDto>()

        do {
            val dto = controller.getAuctionActivitiesSync(continuation, ordersChunk, SyncSortDto.DB_UPDATE_DESC, null)
            continuation = dto.body?.continuation
            dto.body?.let { receivedOrders.addAll(it.items) }
            pageCounter += 1
        } while (continuation != null)

        Assertions.assertThat(pageCounter).isEqualTo(activityQuantities/ordersChunk + 1)
        Assertions.assertThat(receivedOrders).hasSize(activityQuantities)
        Assertions.assertThat(receivedOrders)
            .isSortedAccordingTo { o1, o2 -> compareValues(o2.lastUpdatedAt, o1.lastUpdatedAt) }
    }

    @Test
    fun `should get all order activities using pagination asc`() = runBlocking<Unit> {
        val activityQuantities = 40 //must be even
        val activitiesChunk = 6

        fillRepositories(activityQuantities)

        var continuation : String? = null
        var pageCounter = 0
        val receivedOrders = mutableListOf<AuctionActivityDto>()

        do {
            val dto = controller.getAuctionActivitiesSync(continuation, activitiesChunk, SyncSortDto.DB_UPDATE_ASC, null)
            continuation = dto.body?.continuation
            dto.body?.let { receivedOrders.addAll(it.items) }
            pageCounter += 1
        } while (continuation != null)

        Assertions.assertThat(pageCounter).isEqualTo(activityQuantities/activitiesChunk + 1)
        Assertions.assertThat(receivedOrders).hasSize(activityQuantities)
        Assertions.assertThat(receivedOrders)
            .isSortedAccordingTo { o1, o2 -> compareValues(o1.lastUpdatedAt, o2.lastUpdatedAt) }
    }

    @Test
    fun `should get all order activities with filter using pagination asc`() = runBlocking<Unit> {
        val activityQuantities = 40 //must be even
        val activitiesChunk = 6

        fillRepositories(activityQuantities)

        var continuation : String? = null
        var pageCounter = 0
        val receivedOrders = mutableListOf<AuctionActivityDto>()

        do {
            val dto = controller.getAuctionActivitiesSync(
                continuation,
                activitiesChunk,
                SyncSortDto.DB_UPDATE_ASC,
                listOf(AuctionActivitiesSyncTypesDto.STARTED, AuctionActivitiesSyncTypesDto.CREATED)
            )
            continuation = dto.body?.continuation
            dto.body?.let { receivedOrders.addAll(it.items) }
            pageCounter += 1
        } while (continuation != null)

        Assertions.assertThat(pageCounter).isEqualTo(activityQuantities/activitiesChunk + 1)
        Assertions.assertThat(receivedOrders).hasSize(activityQuantities)
        Assertions.assertThat(receivedOrders)
            .isSortedAccordingTo { o1, o2 -> compareValues(o1.lastUpdatedAt, o2.lastUpdatedAt) }
    }

    private suspend fun fillRepositories(
        activitiesQuantity: Int,
        offChainType: AuctionOffchainHistory.Type = AuctionOffchainHistory.Type.STARTED,
        onChainType: AuctionHistoryType = AuctionHistoryType.ON_CHAIN_AUCTION
    ) {
        repeat(activitiesQuantity / 2) {
            val auction = randomAuction()
            auctionRepository.save(auction)
            offchainHistoryRepository.save(createOffchainHistoryEvent(auction, offChainType))

            val auctionHistoryLogEvent = createAuctionLogEvent(randomAuctionCreated())
            val data = auctionHistoryLogEvent.data as OnChainAuction
            data.type = onChainType
            auctionRepository.save(randomAuction().copy(auctionId = data.auctionId, contract = data.contract))
            val historySave = auctionHistoryRepository.save(auctionHistoryLogEvent)
            historySave.awaitFirst()
        }
    }

}