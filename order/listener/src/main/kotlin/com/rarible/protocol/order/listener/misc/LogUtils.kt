package com.rarible.protocol.order.listener.misc

import com.rarible.protocol.order.core.misc.prefixedError
import com.rarible.protocol.order.core.misc.prefixedInfo
import org.slf4j.Logger

const val SEAPORT_LOG = "[Seaport]"
const val X2Y2_LOG = "[X2Y2]"
const val SUDOSWAP_LOG = "[SudoSwap]"

fun Logger.seaportInfo(message: String) = prefixedInfo(SEAPORT_LOG, message)
fun Logger.seaportError(message: String) = prefixedError(SEAPORT_LOG, message)
fun Logger.seaportError(message: String, ex: Throwable) = prefixedError(SEAPORT_LOG, message, ex)

fun Logger.x2y2Info(message: String) = prefixedInfo(X2Y2_LOG, message)
fun Logger.x2y2Error(message: String) = prefixedError(X2Y2_LOG, message)
fun Logger.x2y2Error(message: String, ex: Throwable) = prefixedError(X2Y2_LOG, message, ex)

fun Logger.sudoswapInfo(message: String) = prefixedInfo(SUDOSWAP_LOG, message)
fun Logger.sudoswapError(message: String) = prefixedError(SUDOSWAP_LOG, message)
fun Logger.sudoswapError(message: String, ex: Throwable) = prefixedError(SUDOSWAP_LOG, message, ex)
