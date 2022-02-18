package com.rarible.protocol.order.core.service

import com.rarible.ethereum.sign.domain.EIP712Domain
import io.daonomic.rpc.domain.Word
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class OpenSeaSigner(
    private val commonSigner: CommonSigner,
    @Qualifier("openseaExchange") private val eip712Domain: EIP712Domain
) {
    fun openSeaHashToSign(hash: Word, eip712: Boolean) =
        if (eip712)
            eip712Domain.hashToSign(hash)
        else
            commonSigner.ethSignHashToSign(hash)
}