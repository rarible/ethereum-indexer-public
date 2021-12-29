package com.rarible.protocol.nft.core.model

import io.daonomic.rpc.domain.Word

typealias HistoryCollection = String
typealias Topic = Word

data class HistoryTopics(
    private val delegate: Map<Topic, HistoryCollection>
): Map<Topic, HistoryCollection> by delegate