package com.rarible.protocol.order.core.model

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Instant

@Document(collection = "order_version")
data class OrderVersion(
    val hash: Word,
    val maker: Address,
    val taker: Address?,
    val make: Asset,
    val take: Asset,
    val makePriceUsd: BigDecimal?,
    val takePriceUsd: BigDecimal?,
    val makeUsd: BigDecimal?,
    val takeUsd: BigDecimal?,
    @Id
    val id: ObjectId = ObjectId(),
    val createdAt: Instant = nowMillis(),
    val platform: Platform = Platform.RARIBLE,

    // RPN-1135: add pre-migration of OrderVersions.
    val type: OrderType = OrderType.RARIBLE_V2,
    val salt: EthUInt256 = EthUInt256.ZERO,
    val start: Long? = null,
    val end: Long? = null,
    val data: OrderData = OrderRaribleV2DataV1(emptyList(), emptyList()),
    val signature: Binary? = null
) {
    fun isBid(): Boolean = take.type.nft

    fun withOrderUsdValue(usdValue: OrderUsdValue): OrderVersion {
        return copy(
            makeUsd = usdValue.makeUsd,
            takeUsd = usdValue.takeUsd,
            makePriceUsd = usdValue.makePriceUsd,
            takePriceUsd = usdValue.takePriceUsd
        )
    }
}

