package com.rarible.protocol.order.core.service.balance

import com.rarible.protocol.order.core.model.StockType
import com.rarible.protocol.order.core.service.approve.Erc20Service
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger

@Component
class StockService(
    private val erc20Service: Erc20Service
) {
    suspend fun getOnChainStock(maker: Address, token: Address, stockType: StockType): BigInteger {
        return when (stockType) {
            StockType.BALANCE -> erc20Service.getOnChainBalance(maker, token)
            StockType.ALLOWANCE -> erc20Service.getOnChainTransferProxyAllowance(maker, token)
        }
    }
}
