package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.listener.log.domain.LogEvent

data class HistoryLog(
    val item: ItemHistory,
    val log: LogEvent
)
