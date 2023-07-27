package com.rarible.protocol.order.core.provider

import com.rarible.ethereum.domain.EthUInt256
import java.util.function.Supplier

class ProtocolCommissionProvider(
    private val protocolCommission: EthUInt256
) : Supplier<EthUInt256> {

    override fun get(): EthUInt256 {
        return protocolCommission
    }
}
