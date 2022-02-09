package com.rarible.protocol.order.core.service.balance

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.asset.AssetBalanceProvider
import com.rarible.protocol.order.core.service.asset.CryptoPunkAssetMakeStockService
import org.springframework.stereotype.Component

@Component
class AssetMakeBalanceProvider(
    private val delegate: AssetBalanceProvider,
    private val cryptoPunkAssetMakeStockService: CryptoPunkAssetMakeStockService
) {

    suspend fun getMakeBalance(order: Order): MakeBalanceState = when {
        order.type == OrderType.CRYPTO_PUNKS -> handleCryptoPunkMarketOrder(order)
        order.platform == Platform.RARIBLE -> handleRaribleOrder(order)
        else -> delegate.getAssetStock(order.maker, order.make)
    } ?: MakeBalanceState(EthUInt256.ZERO)

    /**
     * Handle the balance of CryptoPunk's bids separately.
     * The ETH 'make' balance is stored in the CryptoPunksMarket contract, not in the punk owner's address.
     * We must set the correct 'make' here to make sure that the bid order is considered "active" by OrderRepository.
     */
    private suspend fun handleCryptoPunkMarketOrder(order: Order): MakeBalanceState? {
        if (
            order.make.type is EthAssetType
            && order.take.type is CryptoPunksAssetType
        ) {
            return MakeBalanceState(order.make.value)
        }
        return delegate.getAssetStock(order.maker, order.make)
    }

    private suspend fun handleRaribleOrder(order: Order): MakeBalanceState? {
        if (order.make.type is EthAssetType) {
            /*
             * Handle the balance of the on-chain ETH bids separately.
             * ExchangeV2 locks ETH balance in the contract.
             * The maker has at least 'make.value' amount of ETH available
             * because he was able to put the bid to the contract.
             */
            return MakeBalanceState(order.make.value)
        }
        if (order.make.type is CryptoPunksAssetType) {
            val balance = cryptoPunkAssetMakeStockService.getRaribleCryptoPunkSellMakeStock(
                owner = order.maker,
                punkIndex = order.make.type.tokenId.value.toInt()
            )
            return MakeBalanceState(balance)
        }
        return delegate.getAssetStock(order.maker, order.make)
    }
}
