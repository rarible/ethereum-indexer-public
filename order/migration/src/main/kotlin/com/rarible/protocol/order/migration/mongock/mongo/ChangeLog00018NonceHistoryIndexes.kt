package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.repository.nonce.NonceHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking

@ChangeLog(order = "000018")
class ChangeLog00018NonceHistoryIndexes {

    @ChangeSet(
        id = "ChangeLog00018NonceHistoryIndexes.createIndexForNonceHistory",
        order = "1",
        author = "protocol",
        runAlways = true
    )
    fun createIndexForNonceHistory(@NonLockGuarded nonceHistoryRepository: NonceHistoryRepository) = runBlocking {
        nonceHistoryRepository.createIndexes()
    }
}
