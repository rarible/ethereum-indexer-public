package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.model.ItemId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("item-meta-loading")

internal fun logMetaLoading(itemId: ItemId, message: String, warn: Boolean = false) {
    val logMessage = "Meta of ${itemId.decimalStringValue}: $message"
    if (warn) {
        logger.warn(logMessage)
    } else {
        logger.info(logMessage)
    }
}
