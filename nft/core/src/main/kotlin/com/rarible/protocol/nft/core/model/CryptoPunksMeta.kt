package com.rarible.protocol.nft.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigInteger

@Document("cryptopunks_meta")
data class CryptoPunksMeta(
    @Id
    val id: BigInteger,
    val image: String?,
    val attributes: List<ItemAttribute>)
