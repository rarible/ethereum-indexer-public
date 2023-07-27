package com.rarible.protocol.nft.core.model

import java.time.Instant

data class InconsistentItemContinuation(
    val afterDate: Instant,
    val afterId: ItemId,
) {
    override fun toString(): String {
        return "${afterDate.toEpochMilli()}_$afterId"
    }

    companion object {
        fun parse(str: String?): InconsistentItemContinuation? {
            return if (str.isNullOrEmpty()) {
                null
            } else {
                if (str.contains('_')) {
                    val (dateStr, idStr) = str.split('_')
                    InconsistentItemContinuation(
                        Instant.ofEpochMilli(dateStr.toLong()),
                        ItemId.parseId(idStr)
                    )
                } else {
                    null
                }
            }
        }

        fun InconsistentItem.fromInconsistentItem(): InconsistentItemContinuation {
            return InconsistentItemContinuation(
                afterDate = this.lastUpdatedAt ?: Instant.EPOCH,
                afterId = this.id,
            )
        }
    }
}
