package com.rarible.protocol.nft.core.model

import java.time.Instant

data class OwnershipContinuation(
    val afterDate: Instant,
    val afterId: OwnershipId
) {
    override fun toString(): String {
        return "${afterDate.toEpochMilli()}_${afterId}"
    }

    companion object {
        fun parse(str: String?): OwnershipContinuation? {
            return if(str == null || str.isEmpty()) {
                null
            } else {
                if(str.contains('_')) {
                    val (dateStr, idStr) = str.split('_')
                    OwnershipContinuation(Instant.ofEpochMilli(dateStr.toLong()), OwnershipId.parseId(idStr))
                } else {
                    null
                }
            }
        }
    }
}
