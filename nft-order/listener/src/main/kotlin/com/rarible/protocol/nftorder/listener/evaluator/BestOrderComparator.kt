package com.rarible.protocol.nftorder.listener.evaluator

import com.rarible.protocol.nftorder.core.model.ShortOrder

interface BestOrderComparator {

    val name: String

    fun compare(current: ShortOrder, updated: ShortOrder): ShortOrder


}