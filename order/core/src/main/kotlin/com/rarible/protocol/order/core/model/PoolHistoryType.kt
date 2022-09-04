package com.rarible.protocol.order.core.model

import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.NFTDepositEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.NewPairEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.DeltaUpdateEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.FeeUpdateEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.NFTWithdrawalEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SpotPriceUpdateEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SwapNFTInPairEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SwapNFTOutPairEvent
import io.daonomic.rpc.domain.Word

enum class PoolHistoryType(
    val topic: Set<Word>
) {
    POOL_CREAT(
        topic = setOf(
            NewPairEvent.id(),
        )
    ),
    POOL_NFT_OUT(
        topic = setOf(
            SwapNFTOutPairEvent.id(),
        )
    ),
    POOL_NFT_IN(
        topic = setOf(
            SwapNFTInPairEvent.id(),
        )
    ),
    POOL_NFT_WITHDRAW(
        topic = setOf(
            NFTWithdrawalEvent.id(),
        )
    ),
    POOL_NFT_DEPOSIT(
        topic = setOf(
            NFTDepositEvent.id(),
        )
    ),
    POOL_SPOT_PRICE_UPDATE(
        topic = setOf(
            SpotPriceUpdateEvent.id(),
        )
    ),
    POOL_DELTA_UPDATE(
        topic = setOf(
            DeltaUpdateEvent.id(),
        )
    ),
    POOL_FEE_UPDATE(
        topic = setOf(
            FeeUpdateEvent.id(),
        )
    ),
}
