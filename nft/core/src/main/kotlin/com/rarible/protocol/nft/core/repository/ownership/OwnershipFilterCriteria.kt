package com.rarible.protocol.nft.core.repository.ownership

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.OwnershipContinuation
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
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import scalether.domain.Address

object OwnershipFilterCriteria {
    private const val DEFAULT_LIMIT = 1_000

    fun OwnershipFilter.toCriteria(continuation: OwnershipContinuation?, limit: Int?): Query {
        val criteria = when (this) {
            is OwnershipFilterAll -> all()
            is OwnershipFilterByCollection -> byCollection(collection)
            is OwnershipFilterByCreator -> byCreator(creator)
            is OwnershipFilterByOwner -> byOwner(owner, collection)
            is OwnershipFilterByItem -> byItem(contract, EthUInt256(tokenId))
        }.showDeleted(showDeleted).scrollTo(continuation, this.sort)

        return Query.query(criteria).with(
            this.sort.toMongoSort() ?: Sort.by(
                Sort.Order.desc(Ownership::date.name),
                Sort.Order.desc(Ownership::id.name)
            )
        ).limit(limit ?: DEFAULT_LIMIT)
    }

    private fun all() = Criteria()

    private fun byOwner(user: Address, collection: Address?): Criteria {
        return Criteria().andOperator(
            listOfNotNull(
                Ownership::owner isEqualTo user,
                collection?.let { Ownership::token isEqualTo collection }
            )
        )
    }

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
            OwnershipFilter.Sort.LAST_UPDATE_ASC -> Sort.by(
                Sort.Order.asc(Ownership::date.name),
                Sort.Order.asc(Ownership::id.name)
            )
        }

    private fun Criteria.scrollTo(continuation: OwnershipContinuation?, sort: OwnershipFilter.Sort): Criteria =
        if (continuation == null) {
            this
        } else {
            when (sort) {
                OwnershipFilter.Sort.LAST_UPDATE_ASC -> {
                    this.orOperator(
                        Ownership::date gt continuation.afterDate,
                        Criteria().andOperator(
                            Ownership::date isEqualTo continuation.afterDate,
                            Ownership::id gt continuation.afterId
                        )
                    )
                }
                OwnershipFilter.Sort.LAST_UPDATE -> {
                    this.orOperator(
                        Ownership::date lt continuation.afterDate,
                        Criteria().andOperator(
                            Ownership::date isEqualTo continuation.afterDate,
                            Ownership::id lt continuation.afterId
                        )
                    )
                }
            }
        }

    private infix fun Criteria.showDeleted(showDeleted: Boolean): Criteria {
        return if (showDeleted) this else and(Ownership::deleted).ne(true)
    }
}
