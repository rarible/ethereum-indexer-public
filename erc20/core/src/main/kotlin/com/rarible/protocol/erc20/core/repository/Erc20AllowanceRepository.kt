package com.rarible.protocol.erc20.core.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Allowance
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
@CaptureSpan(type = "db", subtype = "erc20-allowance")
class Erc20AllowanceRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun save(erc20Allowance: Erc20Allowance): Erc20Allowance {
        return template.save(erc20Allowance).awaitFirst()
    }

    suspend fun get(balanceId: BalanceId): Erc20Allowance? {
        return template.findById<Erc20Allowance>(balanceId).awaitFirstOrNull()
    }

    suspend fun deleteByOwner(owner: Address): Long {
        val criteria = Erc20Allowance::owner isEqualTo owner
        return template.remove<Erc20Allowance>(Query.query(criteria)).awaitFirst().deletedCount
    }
}
