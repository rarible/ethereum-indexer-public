 package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking

@ChangeLog(order = "00016")
class ChangeLog00016NftHistoryIndexes {
    @ChangeSet(
        id = "ChangeLog00016NftHistoryIndexes.createIndexes",
        order = "1",
        author = "protocol",
        runAlways = true
    )
    fun createIndexes(@NonLockGuarded exchangeHistoryRepository: NftHistoryRepository) = runBlocking {
        exchangeHistoryRepository.createIndexes()
    }
}
