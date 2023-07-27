package com.rarible.protocol.order.core.service.floor

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.order.core.data.randomErc20
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.data.randomEth
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

@IntegrationTest
internal class FloorSellOrderProviderIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var floorSellOrderProvider: FloorSellOrderProvider

    @Test
    fun `should get all floor orders by currencies`() = runBlocking<Unit> {
        val token = randomAddress()
        val currency1 = randomAddress()
        val currency2 = randomAddress()
        val floorOrder1 = randomOrder().copy(
            make = randomErc721(token),
            take = randomErc20(currency1),
            makePrice = BigDecimal.valueOf(1)
        )
        val floorOrder2 = randomOrder().copy(
            make = randomErc721(token),
            take = randomErc20(currency2),
            makePrice = BigDecimal.valueOf(2)
        )
        val floorOrder3 = randomOrder().copy(
            make = randomErc721(token),
            take = randomEth(),
            makePrice = BigDecimal.valueOf(3)
        )
        val other1 = randomOrder().copy(
            make = randomErc721(token),
            take = randomErc20(currency1),
            makePrice = BigDecimal.valueOf(10)
        )
        val other2 = randomOrder().copy(
            make = randomErc721(token),
            take = randomErc20(currency2),
            makePrice = BigDecimal.valueOf(10)
        )
        val other3 = randomOrder().copy(
            make = randomErc721(token),
            take = randomEth(),
            makePrice = BigDecimal.valueOf(10)
        )
        listOf(floorOrder1, floorOrder2, floorOrder3, other1, other2, other3).shuffled().forEach {
            orderRepository.save(it)
        }
        val floorOrders = floorSellOrderProvider.getCurrencyFloorSells(token)
        assertThat(floorOrders.map { it.hash }).containsExactlyInAnyOrder(floorOrder1.hash, floorOrder2.hash, floorOrder3.hash)
    }
}
