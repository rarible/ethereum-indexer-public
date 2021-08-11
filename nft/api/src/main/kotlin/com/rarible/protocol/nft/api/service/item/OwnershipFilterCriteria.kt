package com.rarible.protocol.nft.api.service.item

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.nft.api.domain.OwnershipContinuation
import com.rarible.protocol.nft.core.model.Ownership
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.*
import scalether.domain.Address

object OwnershipFilterCriteria {
    private const val DEFAULT_LIMIT = 1_000

    fun NftOwnershipFilterDto.toCriteria(continuation: OwnershipContinuation?, limit: Int?): Query {
        val criteria = when (this) {
            is NftOwnershipFilterAllDto -> all()
            is NftOwnershipFilterByCollectionDto -> byCollection(collection)
            is NftOwnershipFilterByCreatorDto -> byCreator(creator)
            is NftOwnershipFilterByOwnerDto -> byOwner(owner)
            is NftOwnershipFilterByItemDto -> byItem(contract, EthUInt256(tokenId))
        } scrollTo continuation

        return Query.query(criteria).with(
            this.sort.toMongoSort() ?: Sort.by(
                Sort.Order.desc(Ownership::date.name),
                Sort.Order.desc(Ownership::id.name)
            )
        ).limit(limit ?: DEFAULT_LIMIT)
    }

    private fun all() = Criteria()

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

    private fun NftOwnershipFilterDto.Sort.toMongoSort() =
        when (this) {
            NftOwnershipFilterDto.Sort.LAST_UPDATE -> Sort.by(
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
}
