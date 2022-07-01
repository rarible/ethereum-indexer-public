package com.rarible.protocol.nft.core.misc

import io.daonomic.rpc.domain.Binary

fun Binary.methodSignatureId(): Binary? = if (length() >= 4) slice(0, 4) else null
