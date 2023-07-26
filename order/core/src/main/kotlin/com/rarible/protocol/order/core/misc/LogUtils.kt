package com.rarible.protocol.order.core.misc

import org.slf4j.Logger

const val LOOKSRARE_LOG = "[Looksrare]"

fun Logger.prefixedInfo(prefix: String, message: String) {
    info("{} {}", prefix, message)
}

fun Logger.prefixedError(prefix: String, message: String) {
    error("{} {}", prefix, message)
}

fun Logger.prefixedError(prefix: String, message: String, ex: Throwable) {
    error("$prefix $message", ex)
}

fun Logger.looksrareInfo(message: String) = prefixedInfo(LOOKSRARE_LOG, message)
fun Logger.looksrareError(message: String) = prefixedError(LOOKSRARE_LOG, message)
fun Logger.looksrareError(message: String, ex: Throwable) = prefixedError(LOOKSRARE_LOG, message, ex)
