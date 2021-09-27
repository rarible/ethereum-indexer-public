package com.rarible.protocol.nftorder.core.event

import com.rarible.protocol.dto.NftOrderOwnershipDto
import com.rarible.protocol.nftorder.core.model.OwnershipId
import java.util.*

sealed class OwnershipEvent(
    val type: OwnershipEventType
) {
    val id: String = UUID.randomUUID().toString()
}

data class OwnershipEventUpdate(
    val ownership: NftOrderOwnershipDto
) : OwnershipEvent(OwnershipEventType.UPDATE)


data class OwnershipEventDelete(
    val ownershipId: OwnershipId
) : OwnershipEvent(OwnershipEventType.DELETE)

enum class OwnershipEventType {
    UPDATE, DELETE
}