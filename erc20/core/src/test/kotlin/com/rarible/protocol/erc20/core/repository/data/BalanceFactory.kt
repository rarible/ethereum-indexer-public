package com.rarible.protocol.erc20.core.repository.data

import com.rarible.core.common.nowMillis
import com.rarible.protocol.erc20.core.model.Erc20Balance

fun createBalance(): Erc20Balance {
    val token = createAddress()
    val owner = createAddress()
    return Erc20Balance(
        token = token,
        owner = owner,
        balance = createEthUInt256(),
        createdAt = nowMillis(),
        lastUpdatedAt = nowMillis()
    )
}

