package com.rarible.protocol.order.core.repository.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.*
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.*
import scalether.domain.Address
import java.time.Instant

sealed class PriceOrderVersionFilter : OrderVersionFilter() {
    abstract val continuation: Continuation.Price?
    abstract override val limit: Int

    abstract fun withContinuation(continuation: Continuation.Price?): PriceOrderVersionFilter
    abstract infix fun Criteria.scrollTo(continuation: Continuation.Price?): Criteria

    data class BidByItem(
        private val contract: Address,
        private val tokenId: EthUInt256,
        private val maker: Address?,
        private val origin: Address?,
        private val platform: Platform?,
        val currencyId: Address?,
        private val startDate: Instant?,
        private val endDate: Instant?,
        private val size: Int,
        override val continuation: Continuation.Price?
    ) : PriceOrderVersionFilter() {
        override val sort = getSort(currencyId)
        override val limit = size

        override fun getCriteria(): Criteria {
            val criteria = listOfNotNull(
                takeNftContractKey isEqualTo contract,
                takeNftTokenIdKey isEqualTo tokenId,
                maker?.let { OrderVersion::maker isEqualTo it },
                origin?.let { (OrderVersion::data / OrderRaribleV2DataV1::originFees).elemMatch(Part::account isEqualTo origin) },
                platform?.let { OrderVersion::platform isEqualTo it },
                currencyId?.let {
                    if (it == Address.ZERO()) { // zero means ETH
                        OrderVersion::make / Asset::type / AssetType::token exists false
                    } else {
                        OrderVersion::make / Asset::type / AssetType::token isEqualTo it
                    }
                },
                startDate?.let { OrderVersion::createdAt gte it },
                endDate?.let { OrderVersion::createdAt lte it }
            )
            return Criteria().andOperator(*criteria.toTypedArray()) scrollTo continuation
        }

        override fun withContinuation(continuation: Continuation.Price?): BidByItem {
            return copy(continuation = continuation)
        }

        override fun Criteria.scrollTo(continuation: Continuation.Price?): Criteria {
            return if (continuation == null) {
                this
            } else {
                if (currencyId == null) {
                    this.orOperator(
                        OrderVersion::takePriceUsd lt continuation.afterPrice,
                        (OrderVersion::takePriceUsd isEqualTo continuation.afterPrice).and("_id")
                            .lt(continuation.afterId)
                    )
                } else {
                    this.orOperator(
                        OrderVersion::takePrice lt continuation.afterPrice,
                        (OrderVersion::takePrice isEqualTo continuation.afterPrice).and("_id")
                            .lt(continuation.afterId)
                    )
                }
            }
        }
    }

    data class BidByMaker(
        private val maker: Address?,
        private val origin: Address?,
        private val platform: Platform?,
        private val startDate: Instant?,
        private val endDate: Instant?,
        private val size: Int,
        override val continuation: Continuation.Price?
    ) : PriceOrderVersionFilter() {
        override val sort = getSort(null)
        override val limit = size

        override fun getCriteria(): Criteria {
            val criteria = listOfNotNull(
                maker?.let { OrderVersion::maker isEqualTo it },
                origin?.let { (OrderVersion::data / OrderRaribleV2DataV1::originFees).elemMatch(Part::account isEqualTo origin) },
                platform?.let { OrderVersion::platform isEqualTo it },
                startDate?.let { OrderVersion::createdAt gte it },
                endDate?.let { OrderVersion::createdAt lte it }
            )
            return Criteria().andOperator(*criteria.toTypedArray()) scrollTo continuation
        }

        override fun withContinuation(continuation: Continuation.Price?): BidByMaker {
            return copy(continuation = continuation)
        }

        override fun Criteria.scrollTo(continuation: Continuation.Price?): Criteria {
            return if (continuation == null) {
                this
            } else {
                this.orOperator(
                    OrderVersion::takePriceUsd lt continuation.afterPrice,
                    (OrderVersion::takePriceUsd isEqualTo continuation.afterPrice).and("_id").lt(continuation.afterId)
                )
            }
        }
    }

    internal companion object {
        fun getSort(currencyId: Address?): Sort {
            return if (currencyId == null) {
                Sort.by(Sort.Order.desc(OrderVersion::takePriceUsd.name), Sort.Order.desc("_id"))
            } else {
                Sort.by(Sort.Order.desc(OrderVersion::takePrice.name), Sort.Order.desc("_id"))
            }
        }
    }
}
