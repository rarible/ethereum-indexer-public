package com.rarible.protocol.nft.listener.misc

import java.math.BigDecimal

fun BigDecimal?.isEqual(value: BigDecimal?): Boolean =
    if (this == null || value == null) {
        this == value
    } else {
        this.compareTo(value) == 0
    }