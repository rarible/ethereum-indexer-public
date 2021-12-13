package com.rarible.protocol.nft.api.service.item

import com.rarible.protocol.nft.api.domain.ItemContinuation
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemFilter
import com.rarible.protocol.nft.core.model.ItemFilterAll
import com.rarible.protocol.nft.core.model.ItemFilterByCollection
import com.rarible.protocol.nft.core.model.ItemFilterByCreator
import com.rarible.protocol.nft.core.model.ItemFilterByOwner
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
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
            .query(criteria showDeleted(showDeleted) scrollTo continuation)
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


    private fun ItemFilter.Sort.toMongoSort() =
        when (this) {
            ItemFilter.Sort.LAST_UPDATE -> Sort.by(
                Sort.Order.desc(Item::date.name),
                Sort.Order.desc(Item::id.name)
            )
        }

    private infix fun Criteria.showDeleted(showDeleted: Boolean): Criteria {
        return if (showDeleted) this else and(Item::deleted).ne(true)
    }

    private infix fun Criteria.scrollTo(continuation: ItemContinuation?): Criteria =
        if (continuation == null) {
            this
        } else {
            this.orOperator(
                Item::date lt continuation.afterDate,
                Criteria().andOperator(
                    Item::date isEqualTo continuation.afterDate,
                    Item::id lt continuation.afterId
                )
            )
        }
}
