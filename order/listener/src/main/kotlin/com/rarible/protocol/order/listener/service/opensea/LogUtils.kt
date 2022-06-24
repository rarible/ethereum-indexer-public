package com.rarible.protocol.order.listener.service.opensea

import org.slf4j.Logger

fun Logger.seaportInfo(message: String) {
    info("[Seaport] {}", message)
}

fun Logger.seaportError(message: String) {
    error("[Seaport] {}", message)
}

fun Logger.seaportError(message: String, ex: Throwable) {
    error("[Seaport] $message", ex)
}

