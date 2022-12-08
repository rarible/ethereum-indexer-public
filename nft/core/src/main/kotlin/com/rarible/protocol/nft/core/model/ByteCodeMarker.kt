package com.rarible.protocol.nft.core.model

import io.daonomic.rpc.domain.Binary

data class ByteCodeMarker(
    val payloads: List<ByteCodeFragment>,
) {
    init {
        require(payloads.isNotEmpty()) {
            "payloads must not be empty list"
        }
        require(payloads.all { it.fragment.bytes().isNotEmpty() }) {
            "payload can't be empty code"
        }
    }
}

class ByteCodeFragment(
    val offset: Int,
    fragment: String
) {
    val fragment: Binary = Binary.apply(fragment)

    fun matchFragment(code: Binary): Boolean {
        return code.length() >= (offset + fragment.length()) &&
               code.slice(offset, offset + fragment.length()) == fragment
    }
}
