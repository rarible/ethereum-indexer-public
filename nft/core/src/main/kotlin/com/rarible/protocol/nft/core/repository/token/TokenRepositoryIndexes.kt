package com.rarible.protocol.nft.core.repository.token

import com.rarible.protocol.nft.core.model.Token
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index

object TokenRepositoryIndexes {
    private val INDEX_BY_DB_UPDATE = Index()
        .on(Token::dbUpdatedAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val INDEX_BY_OWNER = Index()
    .on(Token::owner.name, Sort.Direction.ASC)
    .on(Token::standard.name, Sort.Direction.ASC)
    .on("_id", Sort.Direction.ASC)

    val ALL_INDEXES = listOf(
        INDEX_BY_DB_UPDATE,
        INDEX_BY_OWNER
    )
}