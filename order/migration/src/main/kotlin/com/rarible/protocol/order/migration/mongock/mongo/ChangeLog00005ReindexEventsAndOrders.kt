package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate
import com.rarible.protocol.contracts.exchange.v1.BuyEvent
import com.rarible.protocol.contracts.exchange.v2.events.MatchEvent
import com.rarible.protocol.contracts.exchange.v2.events.MatchEventDeprecated
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

@ChangeLog(order = "00005")
class ChangeLog00005ReindexEventsAndOrders {

    @ChangeSet(id = "ChangeLog00005ReindexEventsAndOrders.deleteHistory", order = "0", author = "protocol")
    fun deleteHistory(mongo: MongockTemplate) {
        val c = Criteria.where("topic").`in`(BuyEvent.id().toString(), MatchEvent.id().toString(), MatchEventDeprecated.id().toString())
        mongo.remove(Query(c), "exchange_history")
    }

    @ChangeSet(id = "ChangeLog00005ReindexEventsAndOrders.reindex", order = "1", author = "protocol")
    fun reindex(mongo: MongockTemplate) {
        val c = Criteria.where("running").`is`(false)
            .and("type").`is`("TOPIC")
            .and("param").`in`(BuyEvent.id().toString(), MatchEvent.id().toString(), MatchEventDeprecated.id().toString())
        restartTasks(mongo, c)
    }

    @ChangeSet(id = "ChangeLog00005ReindexEventsAndOrders.reduce", order = "2", author = "protocol")
    fun reduce(mongo: MongockTemplate) {
        val c = Criteria.where("running").`is`(false)
            .and("type").`is`("ORDER_REDUCE")
        restartTasks(mongo, c)
    }

    private fun restartTasks(mongo: MongockTemplate, c: Criteria) {
        mongo.updateMulti(Query(c), Update().set("state", null).set("lastStatus", "NONE"), "task")
    }
}
