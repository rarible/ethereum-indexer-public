package com.rarible.protocol.order.core.service.approve

import com.rarible.contracts.erc20.IERC20
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.transaction.ReadOnlyMonoTransactionSender
import java.math.BigInteger

@Component
class Erc20Service(
    private val sender: ReadOnlyMonoTransactionSender,
    transferProxyAddresses: OrderIndexerProperties.TransferProxyAddresses,
) {
    private val raribleTransferProxy = transferProxyAddresses.erc20TransferProxy

    suspend fun getOnChainTransferProxyAllowance(maker: Address, token: Address): BigInteger {
        val contract = IERC20(token, sender)
        return contract.allowance(maker, raribleTransferProxy).awaitFirst()
    }

    suspend fun getOnChainBalance(maker: Address, token: Address): BigInteger {
        val contract = IERC20(token, sender)
        return contract.balanceOf(maker).awaitFirst()
    }
}
