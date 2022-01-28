package com.rarible.protocol.nft.core.repository.item

import com.rarible.protocol.nft.core.model.*
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.*
import scalether.domain.Address
import java.time.Instant

object ItemFilterCriteria {

    fun ItemFilter.toCriteria(
        continuation: ItemContinuation?,
        limit: Int
    ): Query {
        val (criteria, showDeleted) = when (this) {
            is ItemFilterAll -> all(lastUpdatedFrom) to showDeleted
            is ItemFilterByCollection -> byCollection(collection, owner) to false
            is ItemFilterByCreator -> byCreator(creator) to false
            is ItemFilterByOwner -> byOwner(owner) to false
        }
        return Query
            .query(criteria.showDeleted(showDeleted).scrollTo(sort, continuation))
            .with(this.sort.toMongoSort())
            .limit(limit)
    }

    private fun all(lastUpdatedFrom: Instant?): Criteria {
        return Criteria().run { if (lastUpdatedFrom != null) and(Item::date).gte(lastUpdatedFrom) else this }
    }

    private fun byOwner(user: Address): Criteria =
        Criteria(Item::owners.name).`is`(user)

    private fun byCreator(creator: Address) =
        Criteria("${Item::creators.name}.recipient").inValues(creator)

    private fun byCollection(collection: Address, owner: Address?): Criteria {
        val condition = Criteria(Item::token.name).`is`(collection)
        return when {
            null != owner -> condition.and(Item::owners.name).`is`(owner)
            else -> condition
        }
    }

    private fun Criteria.scrollTo(sort: ItemFilter.Sort, continuation: ItemContinuation?): Criteria =
        if (continuation == null) {
            this
        } else {
            when (sort) {
                ItemFilter.Sort.LAST_UPDATE_DESC ->
                    this.orOperator(
                        Item::date lt continuation.afterDate,
                        Criteria().andOperator(
                            Item::date isEqualTo continuation.afterDate,
                            Item::id lt continuation.afterId
                        )
                    )
                ItemFilter.Sort.LAST_UPDATE_ASC ->
                    this.orOperator(
                        Item::date gt continuation.afterDate,
                        Criteria().andOperator(
                            Item::date isEqualTo continuation.afterDate,
                            Item::id gt continuation.afterId
                        )
                    )
            }
        }

    private fun Criteria.showDeleted(showDeleted: Boolean): Criteria {
        return if (showDeleted) this else and(Item::deleted).ne(true)
    }

    private fun ItemFilter.Sort.toMongoSort() =
        when (this) {
            ItemFilter.Sort.LAST_UPDATE_DESC -> Sort.by(
                Sort.Order.desc(Item::date.name),
                Sort.Order.desc(Item::id.name)
            )
            ItemFilter.Sort.LAST_UPDATE_ASC -> Sort.by(
                Sort.Order.asc(Item::date.name),
                Sort.Order.asc(Item::id.name)
            )
        }
}
