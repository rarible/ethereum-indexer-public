package com.rarible.protocol.nftorder.core.migration

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.dto.LegacyOrderDto
import com.rarible.protocol.dto.RaribleV2OrderDto
import com.rarible.protocol.nftorder.core.model.Item
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo

@ChangeLog(order = "00002")
class ChangeLog00002UpdateOrderDtoType() {

    @ChangeSet(
        id = "ChangeLog00001UpdateOrderDtoType.updateOrderDtoType",
        order = "2",
        author = "protocol",
        runAlways = false
    )
    fun updateOrderDtoType(@NonLockGuarded mongoTemplate: ReactiveMongoTemplate) = runBlocking {
        updateOrders(mongoTemplate, Item::bestBidOrder.name, "RARIBLE_V1", LegacyOrderDto::class.java.name)
        updateOrders(mongoTemplate, Item::bestSellOrder.name, "RARIBLE_V1", LegacyOrderDto::class.java.name)
        updateOrders(mongoTemplate, Item::bestBidOrder.name, "RARIBLE_V2", RaribleV2OrderDto::class.java.name)
        updateOrders(mongoTemplate, Item::bestSellOrder.name, "RARIBLE_V2", RaribleV2OrderDto::class.java.name)
    }

    private fun updateOrders(
        mongoTemplate: ReactiveMongoTemplate,
        orderField: String,
        orderVersion: String,
        newClassName: String
    ) {
        // We are looking for orders with specified type - RARIBLE_V1 or RARIBLE_V2
        val criteria = Criteria.where("$orderField.type").isEqualTo(orderVersion)

        val update = Update()
            .set("$orderField._class", newClassName) // OrderDto now is abstract class, we have children now
            .unset("$orderField.data._class") // OrderData now is strictly set for each Order subtype
            .unset("$orderField.type") // This field was removed, we are using _class now
        mongoTemplate.updateMulti(Query(criteria), update, Item::class.java).block()
    }

}