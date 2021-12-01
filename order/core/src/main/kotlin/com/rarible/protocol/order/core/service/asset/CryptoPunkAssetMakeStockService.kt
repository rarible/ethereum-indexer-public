package com.rarible.protocol.order.core.service.asset

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.CryptoPunksMarket
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger

@Component
@CaptureSpan(type = SpanType.EXT)
class CryptoPunkAssetMakeStockService(
    private val ethereumSender: MonoTransactionSender,
    private val orderIndexerProperties: OrderIndexerProperties
) {
    /**
     * Returns the approved and actual make stock of address [owner] to sell punk [punkIndex] via Rarible V2 contract.
     * The Rarible V2 requires approval to buy punk from the owner to the punk transfer proxy for 0 ETH at any moment.
     * We must maintain the correct value of 'Order.makeStock' and 'Order.status' when the ownership or approval changes
     */
    suspend fun getRaribleCryptoPunkSellMakeStock(owner: Address, punkIndex: Int): EthUInt256 {
        if (orderIndexerProperties.exchangeContractAddresses.cryptoPunks == Address.ZERO()) {
            return EthUInt256.ZERO
        }
        val cryptoPunksMarket = CryptoPunksMarket(
            orderIndexerProperties.exchangeContractAddresses.cryptoPunks,
            ethereumSender
        )
        /*
        https://github.com/larvalabs/cryptopunks/blob/11532167fa705ced569fc3206df0484f9027e1ee/contracts/CryptoPunksMarket.sol#L26
        struct Offer {
            bool isForSale;
            uint punkIndex;
            address seller;
            uint minValue;
            address onlySellTo;
        }
         */
        val sellTuple = cryptoPunksMarket.punksOfferedForSale(punkIndex.toBigInteger()).awaitFirst()
        val isForSale = sellTuple._1()
        val seller = sellTuple._3()
        val minValue = sellTuple._4()
        val onlySellTo = sellTuple._5()
        if (isForSale
            && owner == seller
            && minValue == BigInteger.ZERO
            && onlySellTo == orderIndexerProperties.transferProxyAddresses.cryptoPunksTransferProxy
        ) {
            return EthUInt256.ONE
        }
        return EthUInt256.ZERO
    }
}
