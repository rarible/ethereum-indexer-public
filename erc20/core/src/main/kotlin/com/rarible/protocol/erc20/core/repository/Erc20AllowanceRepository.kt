package com.rarible.protocol.erc20.core.repository

import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Allowance
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class Erc20AllowanceRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun save(erc20Allowance: Erc20Allowance): Erc20Allowance {
        return template.save(erc20Allowance).awaitFirst()
    }

    suspend fun get(balanceId: BalanceId): Erc20Allowance? {
        return template.findById<Erc20Allowance>(balanceId).awaitFirstOrNull()
    }

    suspend fun getAll(ids: Collection<BalanceId>): List<Erc20Allowance> {
        val criteria = Criteria.where("_id").`in`(ids)
        return template.find<Erc20Allowance>(Query.query(criteria))
            .collectList()
            .awaitSingle()
    }

    suspend fun deleteByOwner(owner: Address): Long {
        val criteria = Erc20Allowance::owner isEqualTo owner
        return template.remove<Erc20Allowance>(Query.query(criteria)).awaitFirst().deletedCount
    }
}
