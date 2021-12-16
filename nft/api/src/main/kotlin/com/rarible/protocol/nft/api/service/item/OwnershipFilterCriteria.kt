package com.rarible.protocol.nft.api.service.item

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.api.domain.OwnershipContinuation
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipFilter
import com.rarible.protocol.nft.core.model.OwnershipFilterAll
import com.rarible.protocol.nft.core.model.OwnershipFilterByCollection
import com.rarible.protocol.nft.core.model.OwnershipFilterByCreator
import com.rarible.protocol.nft.core.model.OwnershipFilterByItem
import com.rarible.protocol.nft.core.model.OwnershipFilterByOwner
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import scalether.domain.Address

object OwnershipFilterCriteria {
    private const val DEFAULT_LIMIT = 1_000

    fun OwnershipFilter.toCriteria(continuation: OwnershipContinuation?, limit: Int?): Query {
        val criteria = when (this) {
            is OwnershipFilterAll -> all(showDeleted)
            is OwnershipFilterByCollection -> byCollection(collection)
            is OwnershipFilterByCreator -> byCreator(creator)
            is OwnershipFilterByOwner -> byOwner(owner)
            is OwnershipFilterByItem -> byItem(contract, EthUInt256(tokenId))
        } scrollTo continuation

        return Query.query(criteria).with(
            this.sort.toMongoSort() ?: Sort.by(
                Sort.Order.desc(Ownership::date.name),
                Sort.Order.desc(Ownership::id.name)
            )
        ).limit(limit ?: DEFAULT_LIMIT)
    }

    private fun all(showDeleted: Boolean) = Criteria() showDeleted showDeleted

    private fun byOwner(user: Address): Criteria =
        Criteria(Ownership::owner.name).`is`(user)

    private fun byItem(token: Address, tokenId: EthUInt256): Criteria =
        Criteria().andOperator(
            Ownership::token isEqualTo token,
            Ownership::tokenId isEqualTo tokenId
        )

    private fun byCreator(creator: Address) =
        Criteria("${Ownership::creators.name}.recipient").inValues(creator)

    private fun byCollection(collection: Address) =
        Criteria(Ownership::token.name).isEqualTo(collection)

    private fun OwnershipFilter.Sort.toMongoSort() =
        when (this) {
            OwnershipFilter.Sort.LAST_UPDATE -> Sort.by(
                Sort.Order.desc(Ownership::date.name),
                Sort.Order.desc(Ownership::id.name)
            )
        }

    private infix fun Criteria.scrollTo(continuation: OwnershipContinuation?): Criteria =
        if (continuation == null) {
            this
        } else {
            this.orOperator(
                Ownership::date lt continuation.afterDate,
                Criteria().andOperator(
                    Ownership::date isEqualTo continuation.afterDate,
                    Ownership::id lt continuation.afterId
                )
            )
        }

    private infix fun Criteria.showDeleted(showDeleted: Boolean): Criteria {
        return if (showDeleted) this else and(Ownership::deleted).ne(true)
    }
}
