package com.rarible.protocol.order.listener.misc

import org.slf4j.Logger

const val SEAPORT_LOG = "[Seaport]"
const val LOOKSRARE_LOG = "[Looksrare]"

internal fun Logger.prefixedInfo(prefix: String, message: String) {
    info("{} {}", prefix, message)
}
internal fun Logger.prefixedError(prefix: String, message: String) {
    error("{} {}", prefix, message)
}
internal fun Logger.prefixedError(prefix: String, message: String, ex: Throwable) {
    error("$prefix $message", ex)
}

fun Logger.seaportInfo(message: String) = prefixedInfo(SEAPORT_LOG, message)
fun Logger.seaportError(message: String) = prefixedError(SEAPORT_LOG, message)
fun Logger.seaportError(message: String, ex: Throwable) = prefixedError(SEAPORT_LOG, message, ex)

fun Logger.looksrareInfo(message: String) = prefixedInfo(LOOKSRARE_LOG, message)
fun Logger.looksrareError(message: String) = prefixedError(LOOKSRARE_LOG, message)
fun Logger.looksrareError(message: String, ex: Throwable) = prefixedError(LOOKSRARE_LOG, message, ex)
