package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.EthUInt256
import org.web3j.crypto.Sign

data class SignedTokenId(
    val tokenId: EthUInt256,
    val sign: Sign.SignatureData
)