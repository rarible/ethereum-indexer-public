package com.rarible.protocol.order.listener.misc

import org.slf4j.Logger

const val SEAPORT_LOG = "[Seaport]"
const val LOOKSRARE_LOG = "[Looksrare]"
const val X2Y2_LOG = "[X2Y2]"
const val SUDOSWAP_LOG = "[SudoSwap]"

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

fun Logger.x2y2Info(message: String) = prefixedInfo(X2Y2_LOG, message)
fun Logger.x2y2Error(message: String) = prefixedError(X2Y2_LOG, message)
fun Logger.x2y2Error(message: String, ex: Throwable) = prefixedError(X2Y2_LOG, message, ex)

fun Logger.sudoswapInfo(message: String) = prefixedInfo(SUDOSWAP_LOG, message)
fun Logger.sudoswapError(message: String) = prefixedError(SUDOSWAP_LOG, message)
fun Logger.sudoswapError(message: String, ex: Throwable) = prefixedError(SUDOSWAP_LOG, message, ex)
