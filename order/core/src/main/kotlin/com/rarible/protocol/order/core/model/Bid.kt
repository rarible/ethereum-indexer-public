package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import scala.Tuple3
import java.time.Instant

sealed class Bid {
    abstract val amount: EthUInt256
    abstract val data: BidData
    abstract val date: Instant

    fun forTx() = Tuple3(
        amount.value,
        data.getDataVersion(),
        data.toEthereum().bytes()
    )
}

data class BidV1(
    override val amount: EthUInt256,
    override val data: BidDataV1,
    override val date: Instant
) : Bid()



