package com.rarible.protocol.order.core.service

import com.rarible.protocol.contracts.Exchange
import com.rarible.protocol.order.core.model.Order
import scala.Tuple2
import scalether.util.Hash
import scalether.util.Hex

object MessageHelper {

    fun prepareBuyerFeeExchangeMessage(order: Order, fee: Int): String {
        val data = Tuple2(
            order.forV1Tx(),
            fee.toBigInteger()
        )
        val encoded = Exchange.buyerFeeMessageType().encode(data)
        val hash = Hash.sha3(encoded.bytes())
        return Hex.to(hash)
    }
}
