package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

@ChangeLog(order = "000019")
class ChangeLog00019AddOriginFeesToSideMatch {
    @ChangeSet(id = "ChangeLog00019AddOriginFeesToSideMatch.addOriginFeesToSideMatch", order = "1", author = "protocol")
    fun addOriginFeesToSideMatch(
        @NonLockGuarded template: ReactiveMongoTemplate
    ) = runBlocking<Unit> {
        val originFeesField = "${ReversedEthereumLogRecord::data.name}.${OrderSideMatch::originFees.name}"
        val dataOriginFeesField = "${ReversedEthereumLogRecord::data.name}.${OrderSideMatch::data.name}.${OrderRaribleV2DataV1::originFees.name}"

        val criteria = Criteria().andOperator(
            Criteria.where(dataOriginFeesField).exists(true),
            Criteria.where(originFeesField).exists(false)
        )
        val multiUpdate = AggregationUpdate
            .update()
            .set(originFeesField).toValue("\$$dataOriginFeesField")

        template.updateMulti(Query(criteria), multiUpdate, ExchangeHistoryRepository.COLLECTION).awaitFirst()
    }
}
