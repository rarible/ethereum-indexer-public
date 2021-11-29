package com.rarible.protocol.order.core.service.balance

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.asset.AssetBalanceProvider
import org.springframework.stereotype.Component

@Component
class AssetMakeBalanceProvider(
    private val delegate: AssetBalanceProvider
) {
    suspend fun getMakeBalance(order: Order): EthUInt256 = when {
        order.type == OrderType.CRYPTO_PUNKS -> handleCryptoPunksOrder(order)
        order.platform == Platform.RARIBLE -> handleRaribleOnChainOrder(order)
        else -> null
    }
        ?: delegate.getAssetStock(order.maker, order.make)
        ?: EthUInt256.ZERO

    /**
     * Handle the balance of CryptoPunk's bids separately.
     * The ETH 'make' balance is stored in the CryptoPunksMarket contract, not in the punk owner's address.
     * We must set the correct 'make' here to make sure that the bid order is considered "active" by OrderRepository.
     */
    private fun handleCryptoPunksOrder(order: Order): EthUInt256? {
        if (order.type == OrderType.CRYPTO_PUNKS
            && order.make.type is EthAssetType
            && order.take.type is CryptoPunksAssetType
        ) {
            return order.make.value
        }
        return null
    }

    /**
     * Handle the balance of the on-chain ETH bids separately.
     * ExchangeV2 locks ETH balance in the contract.
     * The maker has at least 'make.value' amount of ETH available
     * because he was able to put the bid to the contract.
     */
    private fun handleRaribleOnChainOrder(order: Order): EthUInt256? {
        if (order.type == OrderType.RARIBLE_V2 && order.make.type is EthAssetType) {
            return order.make.value
        }
        return null
    }
}
