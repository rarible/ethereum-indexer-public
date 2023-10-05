package com.rarible.protocol.erc20.core.repository

import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Balance
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
class Erc20BalanceRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun save(erc20Balance: Erc20Balance): Erc20Balance {
        return template.save(erc20Balance).awaitFirst()
    }

    suspend fun get(balanceId: BalanceId): Erc20Balance? {
        return template.findById<Erc20Balance>(balanceId).awaitFirstOrNull()
    }

    suspend fun getAll(ids: Collection<BalanceId>): List<Erc20Balance> {
        val criteria = Criteria.where("_id").`in`(ids)
        return template.find<Erc20Balance>(Query.query(criteria))
            .collectList()
            .awaitSingle()
    }

    suspend fun deleteByOwner(owner: Address): Long {
        val criteria = Erc20Balance::owner isEqualTo owner
        return template.remove<Erc20Balance>(Query.query(criteria)).awaitFirst().deletedCount
    }
}
