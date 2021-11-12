package com.rarible.protocol.erc20.core.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Balance
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = "db", subtype = "erc20-balance")
class Erc20BalanceRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun save(erc20Balance: Erc20Balance): Erc20Balance {
        return template.save(erc20Balance).awaitFirst()
    }

    suspend fun get(balanceId: BalanceId): Erc20Balance? {
        return template.findById<Erc20Balance>(balanceId).awaitFirstOrNull()
    }
}
