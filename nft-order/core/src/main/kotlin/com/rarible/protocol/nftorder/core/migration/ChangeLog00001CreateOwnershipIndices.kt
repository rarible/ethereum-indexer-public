package com.rarible.protocol.nftorder.core.migration

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nftorder.core.repository.OwnershipRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking

@ChangeLog(order = "00001")
class ChangeLog00001CreateOwnershipIndices() {

    @ChangeSet(
        id = "ChangeLog00001CreateOwnershipIndices.createIndicesForAllCollections",
        order = "1",
        author = "protocol",
        runAlways = true
    )
    fun createIndicesForOwnership(@NonLockGuarded ownershipRepository: OwnershipRepository) = runBlocking {
        ownershipRepository.createIndices()
    }
}