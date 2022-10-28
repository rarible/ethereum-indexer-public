package com.rarible.protocol.nft.core.model

import scalether.domain.Address
import java.time.Instant

sealed class InconsistentItemFilter {
    abstract val sort: Sort

    enum class Sort {
        LAST_UPDATE_ASC,
    }
}

data class InconsistentItemFilterAll(
    override val sort: Sort = Sort.LAST_UPDATE_ASC,
) : InconsistentItemFilter()

