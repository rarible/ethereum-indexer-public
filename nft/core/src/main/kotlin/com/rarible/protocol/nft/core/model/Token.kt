package com.rarible.protocol.nft.core.model

import com.rarible.core.entity.reducer.model.Entity
import io.daonomic.rpc.domain.Word
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address
import java.time.Instant

@Document(collection = Token.COLLECTION)
data class Token(
    @Id
    override val id: Address,
    val owner: Address? = null,
    val name: String,
    val symbol: String? = null,
    val status: ContractStatus = ContractStatus.CONFIRMED,
    val features: Set<TokenFeature> = emptySet(),
    val dbUpdatedAt: Instant? = null, // TODO Can't be null after migration

    val lastEventId: String? = null,

    @Indexed(background = true)
    val standard: TokenStandard,
    val standardRetries: Int? = 0,
    @Version
    override val version: Long? = null,

    // Better off would be to introduce a TokenVersion enum (RPN-1264).
    val isRaribleContract: Boolean = false,
    val deleted: Boolean = false,
    override val revertableEvents: List<TokenEvent> = emptyList(),
    val scam: Boolean = false,
    val byteCodeHash: Word? = null
) : Entity<Address, TokenEvent, Token> {

    override fun withRevertableEvents(events: List<TokenEvent>): Token {
        return copy(revertableEvents = events)
    }

    fun withByteCodeHash(hash: Word): Token {
        return copy(byteCodeHash = hash)
    }

    fun withDbUpdatedAt(): Token {
        return copy(dbUpdatedAt = Instant.now())
    }

    fun hasChanges(token: Token): Boolean {
        return standard != token.standard
                || name != token.name
                || symbol != token.symbol
                || features != token.features
                || owner != token.owner
                || scam != token.scam
    }

    companion object {
        fun empty() = Token(
            id = Address.ZERO(),
            name = "",
            standard = TokenStandard.NONE,
            status = ContractStatus.PENDING
        )
        const val COLLECTION = "token"
    }
}
