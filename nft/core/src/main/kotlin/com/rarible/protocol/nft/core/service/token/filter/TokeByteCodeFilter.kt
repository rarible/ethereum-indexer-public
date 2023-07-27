package com.rarible.protocol.nft.core.service.token.filter

import io.daonomic.rpc.domain.Binary

interface TokeByteCodeFilter {
    fun isValid(code: Binary): Boolean
}
