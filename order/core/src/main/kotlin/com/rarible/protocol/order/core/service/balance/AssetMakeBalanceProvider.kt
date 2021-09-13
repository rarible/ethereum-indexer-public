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
    suspend fun getMakeBalance(order: Order): EthUInt256 =
        handleCryptoPunksOrder(order)
            ?: delegate.getAssetStock(order.maker, order.make.type)
            ?: EthUInt256.ZERO

    private fun handleCryptoPunksOrder(order: Order): EthUInt256? {
        /*
            Handle the balance of CryptoPunk's bids separately.
            The ETH 'make' balance is stored in the CryptoPunksMarket contract, not in the punk owner's address.
            We must set the correct 'make' here to make sure that the bid order is considered "active" by OrderRepository.
         */
        if (order.type == OrderType.CRYPTO_PUNKS
            && order.make.type is EthAssetType
            && order.take.type is CryptoPunksAssetType
        ) {
            return order.make.value
        }
        return null
    }
}