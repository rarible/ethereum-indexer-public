package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking

@ChangeLog(order = "00002")
class ChangeLog00002HistoryIndexes {
    @ChangeSet(id = "ChangeLog00003ItemIndexes.createItemHistoryRepositoryIndexes", order = "1", author = "protocol", runAlways = true)
    fun createIndexForAll(@NonLockGuarded itemHistoryRepository: NftItemHistoryRepository) = runBlocking {
        itemHistoryRepository.createIndexes()
    }

    @ChangeSet(id = "ChangeLog00003ItemIndexes.dropUnneededItemHistoryRepositoryIndexes", order = "2", author = "protocol", runAlways = true)
    fun dropUnneededIndexForAll(@NonLockGuarded itemHistoryRepository: NftItemHistoryRepository) = runBlocking {
        itemHistoryRepository.dropIndexes()
    }
}
