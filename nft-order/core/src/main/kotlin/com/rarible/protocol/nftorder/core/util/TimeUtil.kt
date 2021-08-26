package com.rarible.protocol.nftorder.core.util

import com.rarible.core.common.nowMillis
import java.time.Instant

fun spent(from: Instant): Long {
    return nowMillis().toEpochMilli() - from.toEpochMilli()
}