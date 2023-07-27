package com.rarible.protocol.nft.core.model

sealed class InconsistentItemFilter {
    abstract val sort: Sort

    enum class Sort {
        LAST_UPDATE_ASC,
    }
}

data class InconsistentItemFilterAll(
    override val sort: Sort = Sort.LAST_UPDATE_ASC,
) : InconsistentItemFilter()
