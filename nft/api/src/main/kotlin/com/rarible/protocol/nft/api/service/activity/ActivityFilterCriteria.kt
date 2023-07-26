package com.rarible.protocol.nft.api.service.activity

// import com.rarible.core.model.type.EthUInt256
// import com.rarible.protocol.dto.activity.ActivityContinuationDto
// import com.rarible.protocol.nft.core.model.ItemType
// import org.springframework.data.domain.Sort
// import org.springframework.data.mongodb.core.query.Criteria
// import org.springframework.data.mongodb.core.query.Query
// import org.springframework.data.mongodb.core.query.isEqualTo
// import scalether.domain.Address
//
// object ActivityFilterCriteria {
//    private const val DEFAULT_LIMIT = 50
//
//    private val sort = Sort.by(
//        Sort.Order.desc("data.date"),
//        Sort.Order.desc("id")
//    )
//
//    fun NftActivityRequestDto.toCriteria(continuation: ActivityContinuationDto?, limit: Int? = null): Query {
//        val criteria = this.filter.run {
//            (when (this) {
//                is ActivityFilter.All -> all()
//                is ActivityFilter.ByCollection -> byCollection(contract)
//                is ActivityFilter.ByItem -> byItem(contract, EthUInt256(tokenId))
//                is ActivityFilter.ByUser -> byUser(user)
//            } scrollTo continuation)
//        }
//
//        return Query.query(criteria withType type)
//            .with(sort)
//            .limit(limit ?: DEFAULT_LIMIT)
//    }
//
//    private fun all() = Criteria("data.type").`is`(ItemType.TRANSFER)
//
//    private fun byUser(user: Address): Criteria =
//        all().and("data.owner").`is`(user)
//
//    private fun byItem(token: Address, tokenId: EthUInt256) =
//        all().and("data.token").`is`(token).and("data.tokenId").`is`(tokenId)
//
//    private fun byCollection(collection: Address) =
//        Criteria("data.token").isEqualTo(collection)
//
//    private fun typeFilter(type: NftActivityType): Criteria {
//        return when (type) {
//            NftActivityType.BURN -> {
//                Criteria("data.owner").`is`(Address.ZERO())
//            }
//            NftActivityType.MINT -> {
//                Criteria("data.from").`is`(Address.ZERO())
//            }
//            NftActivityType.TRANSFER -> {
//                Criteria()
//                    .and("data.from").ne(Address.ZERO())
//                    .and("data.owner").ne(Address.ZERO())
//            }
//        }
//    }
//
//    private infix fun Criteria.withType(type: NftActivityType) =
//        this.andOperator(typeFilter(type))
//
//    private infix fun Criteria.scrollTo(continuation: ActivityContinuationDto?): Criteria =
//        if (continuation == null) {
//            this
//        } else {
//            this.orOperator(
//                Criteria("data.date").lt(continuation.afterDate),
//                Criteria("data.date").isEqualTo(continuation.afterDate)
//                    .and("id").lt(continuation.afterId)
//            )
//        }
// }
