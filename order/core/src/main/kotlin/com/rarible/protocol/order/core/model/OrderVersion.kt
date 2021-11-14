package com.rarible.protocol.order.core.model

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Instant

@Document(collection = OrderVersionRepository.COLLECTION)
data class OrderVersion(
    val maker: Address,
    val taker: Address?,
    val make: Asset,
    val take: Asset,
    val makePriceUsd: BigDecimal?,
    val takePriceUsd: BigDecimal?,
    val makePrice: BigDecimal?,
    val takePrice: BigDecimal?,
    val makeUsd: BigDecimal?,
    val takeUsd: BigDecimal?,
    @Id
    val id: ObjectId = ObjectId(),
    val onChainOrderKey: LogEventKey? = null,
    val createdAt: Instant = nowMillis(),
    val platform: Platform = Platform.RARIBLE,
    // TODO: Default values here are needed only before the 1st migration ChangeLog00011AddAllFieldsFromOrderToOrderVersion is run
    // to read the old OrderVersions from the database. After that we should remove the default values.
    val type: OrderType = OrderType.RARIBLE_V2,
    val salt: EthUInt256 = EthUInt256.ZERO,
    val start: Long? = null,
    val end: Long? = null,
    val data: OrderData = OrderRaribleV2DataV1(emptyList(), emptyList()),
    val signature: Binary? = null,

    val hash: Word = Order.hashKey(maker, make.type, take.type, salt.value)
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

fun OrderVersion.toOrderExactFields() = Order(
    maker = maker,
    taker = taker,
    make = make,
    take = take,
    type = type,
    fill = EthUInt256.ZERO,
    cancelled = false,
    makeStock = make.value,
    salt = salt,
    start = start,
    end = end,
    data = data,
    signature = signature,
    createdAt = createdAt,
    lastUpdateAt = createdAt,
    pending = emptyList(),
    makePriceUsd = null,
    takePriceUsd = null,
    makeUsd = null,
    takeUsd = null,
    priceHistory = emptyList(),
    platform = platform
)

fun OrderVersion.toOnChainOrder() = OnChainOrder(
    maker = maker,
    taker = taker,
    make = make,
    take = take,
    createdAt = createdAt,
    platform = platform,
    orderType = type,
    salt = salt,
    start = start,
    end = end,
    data = data,
    signature = signature,
    hash = hash,
    priceUsd = makePriceUsd ?: takePriceUsd
)

fun OnChainOrder.toOrderVersion() =
    OrderVersion(
        maker = maker,
        taker = taker,
        make = make,
        take = take,
        makePriceUsd = null,
        takePriceUsd = null,
        makePrice = null,
        takePrice = null,
        makeUsd = null,
        takeUsd = null,
        createdAt = createdAt,
        platform = platform,
        type = orderType,
        salt = salt,
        start = start,
        end = end,
        data = data,
        signature = signature,
        hash = hash
    )

