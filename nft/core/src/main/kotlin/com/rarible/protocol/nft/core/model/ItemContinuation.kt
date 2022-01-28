package com.rarible.protocol.nft.core.model

import java.time.Instant

data class ItemContinuation(
    val afterDate: Instant,
    val afterId: ItemId
) {
    override fun toString(): String {
        return "${afterDate.toEpochMilli()}_${afterId}"
    }

    companion object {
        fun parse(str: String?): ItemContinuation? {
            return if(str == null || str.isEmpty()) {
                null
            } else {
                if(str.contains('_')) {
                    val (dateStr, idStr) = str.split('_')
                    ItemContinuation(Instant.ofEpochMilli(dateStr.toLong()), ItemId.parseId(idStr))
                } else {
                    null
                }
            }
        }
    }
}
