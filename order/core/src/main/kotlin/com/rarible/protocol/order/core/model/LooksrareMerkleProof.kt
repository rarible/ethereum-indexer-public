package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary

data class LooksrareMerkleProof(
    val position: Long,
    val value: Binary
)
