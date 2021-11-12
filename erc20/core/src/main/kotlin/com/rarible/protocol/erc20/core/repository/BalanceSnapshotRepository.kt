package com.rarible.protocol.erc20.core.repository

import com.rarible.core.reduce.repository.SnapshotRepository
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.BalanceReduceSnapshot
import com.rarible.protocol.erc20.core.model.Erc20Balance
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Component

@Component
class BalanceSnapshotRepository(
    private val template: ReactiveMongoTemplate
) : SnapshotRepository<BalanceReduceSnapshot, Erc20Balance, Long, BalanceId> {

    override suspend fun get(key: BalanceId): BalanceReduceSnapshot? {
        return template.findById<BalanceReduceSnapshot>(key).awaitFirstOrNull()
    }

    override suspend fun save(snapshot: BalanceReduceSnapshot): BalanceReduceSnapshot {
        return template.save(snapshot).awaitFirst()
    }
}
