package com.rarible.protocol.nftorder.core.migration

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nftorder.core.repository.OwnershipRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking

@ChangeLog(order = "99999")
class ChangeLog99999CreateOwnershipIndices() {

    @ChangeSet(
        id = "ChangeLog00001CreateOwnershipIndices.createIndicesForAllCollections",
        order = "99999",
        author = "protocol",
        runAlways = true
    )
    fun createIndicesForOwnership(@NonLockGuarded ownershipRepository: OwnershipRepository) = runBlocking {
        ownershipRepository.createIndices()
    }
}