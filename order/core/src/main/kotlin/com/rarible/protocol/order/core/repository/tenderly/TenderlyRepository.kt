package com.rarible.protocol.order.core.repository.tenderly

import com.rarible.protocol.order.core.model.tenderly.TenderlyStat
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class TenderlyRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun getById(id: LocalDate): TenderlyStat? {
        return template.findById(id, TenderlyStat::class.java).awaitFirstOrNull()
    }

    suspend fun save(stat: TenderlyStat): TenderlyStat {
        return template.save(stat).awaitSingle()
    }

    suspend fun requestsByMonth(): Long {
        val startDate = LocalDate.now().minusMonths(1)
        val aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("_id").gt(startDate)),
            Aggregation.group().sum(TenderlyStat::requests.name).`as`(SumResult::totalSum.name)
        )
        val aggregationResults = template.aggregate(aggregation, TenderlyStat::class.java, SumResult::class.java).awaitFirstOrNull()
        return aggregationResults?.totalSum ?: 0L
    }

    data class SumResult(
        val totalSum: Long
    )
}
