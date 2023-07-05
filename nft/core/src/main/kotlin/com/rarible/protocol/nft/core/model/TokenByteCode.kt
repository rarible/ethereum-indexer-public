package com.rarible.protocol.nft.core.model

import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant


@Document(collection = TokenByteCode.COLLECTION)
data class TokenByteCode(
    @Id
    val hash: Word,
    val code: Binary,
    val createdAt: Instant = Instant.now(),
) {
    companion object {
        const val COLLECTION = "token_byte_code"
    }
}