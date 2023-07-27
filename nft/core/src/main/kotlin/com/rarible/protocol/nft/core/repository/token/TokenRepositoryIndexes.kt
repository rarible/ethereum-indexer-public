package com.rarible.protocol.nft.core.repository.token

import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.PartialIndexFilter
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.isEqualTo

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

    private val INDEX_STANDARD_RETRIES = Index()
        .partial(PartialIndexFilter.of(Criteria.where(Token::standard.name).isEqualTo(TokenStandard.NONE)))
        .on(Token::standardRetries.name, Sort.Direction.ASC)

    private val INDEX_BYTE_CODE_HASH = Index()
        .on(Token::byteCodeHash.name, Sort.Direction.ASC)

    val ALL_INDEXES = listOf(
        INDEX_BY_DB_UPDATE,
        INDEX_STANDARD,
        INDEX_STANDARD_RETRIES,
        INDEX_BY_OWNER,
        INDEX_BY_OWNER_AND_STANDARD,
        INDEX_BYTE_CODE_HASH,
    )
}
