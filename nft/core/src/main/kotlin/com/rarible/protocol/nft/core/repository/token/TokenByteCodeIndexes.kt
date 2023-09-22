package com.rarible.protocol.nft.core.repository.token

import com.rarible.protocol.nft.core.model.TokenByteCode
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.PartialIndexFilter
import org.springframework.data.mongodb.core.query.isEqualTo

object TokenByteCodeIndexes {
    private val INDEX_BY_SCAM = Index()
        .on("_id", Sort.Direction.ASC)
        .partial(PartialIndexFilter.of(TokenByteCode::scam isEqualTo true))
        .background()

    val ALL_INDEXES = listOf(INDEX_BY_SCAM)
}
