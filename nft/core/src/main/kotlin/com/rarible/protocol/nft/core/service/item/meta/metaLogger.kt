package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.model.ItemId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("item-meta-loading")

internal fun logMetaLoading(itemId: ItemId, message: String, warn: Boolean = false) {
    logMetaLoading(itemId.decimalStringValue, message, warn)
}

fun logMetaLoading(id: String, message: String, warn: Boolean = false) = logger.logMetaLoading(id, message, warn)

fun Logger.logMetaLoading(id: String, message: String, warn: Boolean = false) {
    val logMessage = "Meta of $id: $message"
    if (warn) {
        this.warn(logMessage)
    } else {
        this.info(logMessage)
    }
}
