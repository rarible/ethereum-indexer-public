package com.rarible.protocol.nft.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address

@Document(collection = "token")
data class Token(
    @Id
    val id: Address,
    val owner: Address? = null,
    val name: String,
    val symbol: String? = null,
    val status: ContractStatus = ContractStatus.CONFIRMED,
    val features: Set<TokenFeature> = emptySet(),

    val lastEventId: String? = null,

    @Indexed(background = true)
    val standard: TokenStandard,
    @Version
    val version: Long? = null
) {
    companion object {
        fun empty() = Token(
            id = Address.ZERO(),
            name = "",
            standard = TokenStandard.NONE,
            status = ContractStatus.PENDING
        )
    }
}
