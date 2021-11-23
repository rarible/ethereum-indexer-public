package com.rarible.protocol.nft.api.service.item

import com.rarible.protocol.dto.*
import com.rarible.protocol.nft.api.domain.ItemContinuation
import com.rarible.protocol.nft.core.model.Item
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.*
import scalether.domain.Address
import java.time.Instant

object ItemFilterCriteria {

    const val DEFAULT_LIMIT = 1_000

    fun NftItemFilterDto.toCriteria(
        continuation: ItemContinuation?,
        limit: Int? = null
    ): Query {
        val (criteria, showDeleted) = when (this) {
            is NftItemFilterAllDto -> all(lastUpdatedFrom) to showDeleted
            is NftItemFilterByCollectionDto -> byCollection(collection, owner) to false
            is NftItemFilterByCreatorDto -> byCreator(creator) to false
            is NftItemFilterByOwnerDto -> byOwner(owner) to false
        }
        return Query
            .query(criteria showDeleted(showDeleted) scrollTo continuation)
            .with(this.sort.toMongoSort())
            .limit(limit ?: DEFAULT_LIMIT)
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


    private fun NftItemFilterDto.Sort.toMongoSort() =
        when (this) {
            NftItemFilterDto.Sort.LAST_UPDATE -> Sort.by(
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
