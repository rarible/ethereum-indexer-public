package com.rarible.protocol.order.core.repository.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.token
import org.bson.types.ObjectId
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.elemMatch
import org.springframework.data.mongodb.core.query.exists
import org.springframework.data.mongodb.core.query.gte
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import org.springframework.data.mongodb.core.query.lte
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Instant

sealed class PriceOrderVersionFilter : OrderVersionFilter() {
    // TODO this continuation will NOT work in right way for order_version repository,
    // TODO since it contains order hash, but _id in entities is random Object()
    abstract val continuation: Continuation.Price?
    abstract override val limit: Int

    abstract fun withContinuation(continuation: InternalPriceContinuation?): PriceOrderVersionFilter
    abstract fun Criteria.scrollToContinuation(): Criteria

    data class BidByItem(
        private val contract: Address,
        private val tokenId: EthUInt256,
        private val makers: List<Address>?,
        private val origin: Address?,
        private val platforms: List<Platform>,
        val currencyId: Address?,
        private val startDate: Instant?,
        private val endDate: Instant?,
        private val size: Int,
        override val continuation: Continuation.Price?,
        private val internalContinuation: InternalPriceContinuation? = null
    ) : PriceOrderVersionFilter() {
        override val sort = getSort(currencyId)
        override val limit = size

        override fun getCriteria(): Criteria {
            val makersCriteria = if (makers.isNullOrEmpty()) null else OrderVersion::maker inValues makers
            val criteria = listOfNotNull(
                tokenCondition(),
                makersCriteria,
                origin?.let { (OrderVersion::data / OrderRaribleV2DataV1::originFees).elemMatch(Part::account isEqualTo origin) },
                if (platforms.isNotEmpty()) platforms.let { OrderVersion::platform inValues it } else null,
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
            return Criteria().andOperator(*criteria.toTypedArray()).scrollToContinuation()
        }

        private fun tokenCondition(): Criteria {
            val forToken = listOfNotNull(
                takeNftContractKey isEqualTo contract,
                takeNftTokenIdKey isEqualTo tokenId
            )
            val forCollection = listOfNotNull(
                takeNftContractKey isEqualTo contract,
                takeNftTokenIdKey exists false
            )
            return Criteria().orOperator(
                Criteria().andOperator(*forToken.toTypedArray()),
                Criteria().andOperator(*forCollection.toTypedArray())
            )
        }

        override fun withContinuation(continuation: InternalPriceContinuation?): BidByItem {
            return copy(internalContinuation = continuation)
        }

        override fun Criteria.scrollToContinuation(): Criteria {
            // TODO
            // Internal continuation used for sub-requests only, otherwise - legacy logic
            // (which are not correct, originally - order hash can't be used in filter)
            val afterPrice = internalContinuation?.afterPrice ?: continuation?.afterPrice
            val afterId = internalContinuation?.afterId ?: continuation?.afterId

            // Both of them are null or not null
            return if (afterPrice == null || afterId == null) {
                this
            } else {
                if (currencyId == null) {
                    this.orOperator(
                        OrderVersion::takePriceUsd lt afterPrice,
                        (OrderVersion::takePriceUsd isEqualTo afterPrice).and("_id")
                            .lt(afterId)
                    )
                } else {
                    this.orOperator(
                        OrderVersion::takePrice lt afterPrice,
                        (OrderVersion::takePrice isEqualTo afterPrice).and("_id")
                            .lt(afterId)
                    )
                }
            }
        }
    }

    data class BidByMaker(
        private val maker: Address?,
        private val origin: Address?,
        private val platforms: List<Platform>,
        private val startDate: Instant?,
        private val endDate: Instant?,
        private val size: Int,
        override val continuation: Continuation.Price?,
        val internalContinuation: InternalPriceContinuation? = null
    ) : PriceOrderVersionFilter() {
        override val sort = getSort(null)
        override val limit = size
        override val hint = OrderVersionRepositoryIndexes.MAKER_TAKE_PRICE_USD_BID_DEFINITION.indexKeys

        override fun getCriteria(): Criteria {
            val criteria = listOfNotNull(
                takeNftKey isEqualTo true,
                maker?.let { OrderVersion::maker isEqualTo it },
                origin?.let { (OrderVersion::data / OrderRaribleV2DataV1::originFees).elemMatch(Part::account isEqualTo origin) },
                if (platforms.isNotEmpty()) platforms.let { OrderVersion::platform inValues it } else null,
                startDate?.let { OrderVersion::createdAt gte it },
                endDate?.let { OrderVersion::createdAt lte it }
            )
            return Criteria().andOperator(*criteria.toTypedArray()).scrollToContinuation()
        }

        override fun withContinuation(continuation: InternalPriceContinuation?): BidByMaker {
            return copy(internalContinuation = continuation)
        }

        override fun Criteria.scrollToContinuation(): Criteria {
            // TODO
            // Internal continuation used for sub-requests only, otherwise - legacy logic
            // (which are not correct, originally - order hash can't be used in filter)
            val afterPrice = internalContinuation?.afterPrice ?: continuation?.afterPrice
            val afterId = internalContinuation?.afterId ?: continuation?.afterId

            // Both of them are null or not null
            return if (afterPrice == null || afterId == null) {
                this
            } else {
                this.orOperator(
                    OrderVersion::takePriceUsd lt afterPrice,
                    (OrderVersion::takePriceUsd isEqualTo afterPrice).and("_id").lt(afterId)
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

// Workaround for case, when we need to request order versions several times in single request.
// Continuation with afterId as order hash will NOT work here since _id in order_versions is mongoId
// and when we use hash of order as part of continuation, it doesn't work
data class InternalPriceContinuation(
    val afterPrice: BigDecimal,
    val afterId: ObjectId
)
