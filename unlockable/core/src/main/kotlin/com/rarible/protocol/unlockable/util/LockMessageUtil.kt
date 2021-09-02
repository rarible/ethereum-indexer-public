package com.rarible.protocol.unlockable.util

import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address

object LockMessageUtil {

    fun getLockMessage(token: Address, tokenId: EthUInt256, content: String) =
        "I would like to set lock for $token:$tokenId. content is $content"

    fun getUnlockMessage(token: Address, tokenId: EthUInt256) =
        "I would like to unlock content for $token:$tokenId"

}