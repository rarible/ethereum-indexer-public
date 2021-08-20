package com.rarible.protocol.nftorder.core.service

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.test.IntegrationTest
import com.rarible.protocol.nftorder.core.test.data.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class OwnershipServiceIt {

    @Autowired
    private lateinit var ownershipService: OwnershipService

    @Test
    fun getTotalStock() = runBlocking<Unit> {
        val contract = randomAddress()
        val tokenId = randomEthUInt256()
        val itemId = randomItemId()

        val orderDto1 = randomOrderDto(itemId).copy(makeStock = 54.toBigInteger())
        val orderDto2 = randomOrderDto(itemId).copy(makeStock = 33.toBigInteger())
        val orderDto3 = randomOrderDto(itemId).copy(makeStock = 13.toBigInteger())

        val ownership1 = randomOwnership(itemId, randomPart()).copy(bestSellOrder = orderDto1)
        val ownership2 = randomOwnership(itemId, randomPart()).copy(bestSellOrder = orderDto2)
        val ownership3 = randomOwnership(itemId, randomPart()).copy(bestSellOrder = orderDto3)

        // should not be included into calculation
        val ownership4 = randomOwnership(itemId, randomPart())
        val ownership5 = randomOwnership(ItemId(randomAddress(), tokenId), randomPart())
            .copy(bestSellOrder = orderDto2)
        val ownership6 = randomOwnership(ItemId(contract, randomEthUInt256()), randomPart())
            .copy(bestSellOrder = orderDto3)

        ownershipService.save(ownership1)
        ownershipService.save(ownership2)
        ownershipService.save(ownership3)
        ownershipService.save(ownership4)
        ownershipService.save(ownership5)
        ownershipService.save(ownership6)

        val itemSellStats = ownershipService.getItemSellStats(itemId)

        assertEquals(100, itemSellStats.totalStock.toInt())
        assertEquals(3, itemSellStats.sellers)
    }
}