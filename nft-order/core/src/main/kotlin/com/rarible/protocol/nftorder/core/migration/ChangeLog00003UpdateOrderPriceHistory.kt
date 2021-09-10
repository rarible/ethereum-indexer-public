package com.rarible.protocol.nftorder.core.migration

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.dto.OrderPriceHistoryRecordDto
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.Ownership
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

@ChangeLog(order = "00003")
class ChangeLog00003UpdateOrderPriceHistory() {

    @ChangeSet(
        id = "ChangeLog00003UpdateOrderPriceHistory.setDefaultPriceHistory",
        order = "2",
        author = "protocol",
        runAlways = false
    )
    fun setDefaultPriceHistory(@NonLockGuarded mongoTemplate: ReactiveMongoTemplate) = runBlocking {
        updateOrders(mongoTemplate, Item::bestBidOrder.name, Item::class.java)
        updateOrders(mongoTemplate, Item::bestSellOrder.name, Item::class.java)
        updateOrders(mongoTemplate, Ownership::bestSellOrder.name, Ownership::class.java)
    }

    private fun updateOrders(
        mongoTemplate: ReactiveMongoTemplate,
        orderField: String,
        collectionClass: Class<*>
    ) {

        val criteria = Criteria
            .where(orderField).exists(true)
            .and("$orderField.priceHistory").exists(false)

        val update = Update()
            .set("$orderField.priceHistory", emptyList<OrderPriceHistoryRecordDto>())
        mongoTemplate.updateMulti(Query(criteria), update, collectionClass).block()
    }

}