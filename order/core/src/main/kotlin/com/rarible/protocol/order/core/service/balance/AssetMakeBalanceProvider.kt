package com.rarible.protocol.order.core.service.balance

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.service.asset.AssetBalanceProvider
import org.springframework.stereotype.Component

@Component
class AssetMakeBalanceProvider(
    private val delegate: AssetBalanceProvider
) {
    suspend fun getMakeBalance(order: Order): EthUInt256 {
        val makeBalance = delegate.getAssetStock(order.maker, order.make.type) ?: EthUInt256.ZERO
        /*
            Handle the balance of CryptoPunk's bids separately.
            'make' ETC balance is at least as "order.make" and
            it is stored in the CryptoPunks contract (see 'punkBids' contract field).
            This is because the bidder was able to make the bid order.
            We must set the correct 'make' here to make sure that the bid order is considered "active" by OrderRepository.
         */
        if (order.type == OrderType.CRYPTO_PUNKS
            && order.make.type is EthAssetType
            && order.take.type is CryptoPunksAssetType
        ) {
            return order.make.value
        }
         //TODO[punk]: make stock of CryptoPunk SELL order must be updated: ask punks contract whether the seller gave access to our proxy to buy the punk.
        return makeBalance
    }
}