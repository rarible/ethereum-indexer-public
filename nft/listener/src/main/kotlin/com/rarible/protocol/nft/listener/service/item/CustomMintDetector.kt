package com.rarible.protocol.nft.listener.service.item

import com.rarible.contracts.erc1155.TransferSingleEvent
import com.rarible.protocol.nft.core.misc.methodSignatureId
import io.daonomic.rpc.domain.Binary
import org.springframework.stereotype.Component
import scalether.domain.response.Transaction

@Component
class CustomMintDetector {
    fun isMint(event: TransferSingleEvent, transaction: Transaction): Boolean {
        return event._from() == event._to() && transaction.input().methodSignatureId() == MINT_METHOD_ID_SIGNATURE
    }

    companion object {
        internal val MINT_METHOD_ID_SIGNATURE: Binary = Binary.apply("0x731133e9")
    }
}