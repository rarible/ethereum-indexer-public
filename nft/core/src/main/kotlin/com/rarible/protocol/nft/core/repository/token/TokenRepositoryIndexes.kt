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
        .on("_id", Sort.Direction.ASC)

    private val INDEX_BY_OWNER_AND_STANDARD = Index()
        .on(Token::owner.name, Sort.Direction.ASC)
        .on(Token::standard.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)

    private val INDEX_STANDARD = Index()
        .on(Token::standard.name, Sort.Direction.ASC)
        .on(Token::standardRetries.name, Sort.Direction.ASC)

    val ALL_INDEXES = listOf(
        INDEX_BY_DB_UPDATE,
        INDEX_STANDARD,
        INDEX_BY_OWNER,
        INDEX_BY_OWNER_AND_STANDARD
    )
}