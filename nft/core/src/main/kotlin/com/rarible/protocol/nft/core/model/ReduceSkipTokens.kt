package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.EthUInt256
import org.slf4j.LoggerFactory
import scalether.domain.Address
import java.util.concurrent.atomic.AtomicLong

class ReduceSkipTokens(tokens: Collection<ItemId>) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val skipCounters = tokens.associateWith { AtomicLong(ALLOWING_PERIOD - 1) }

    fun allowReducing(token: Address, tokenId: EthUInt256): Boolean {
        val itemId = ItemId(token, tokenId)
        val skipCounter = skipCounters[itemId]?.getAndIncrement() ?: 0L
        val allow = skipCounter / ALLOWING_PERIOD == 0L

        if (allow.not()) {
            logger.info("Token ${itemId.decimalStringValue} not allowed to be reduced" )
        }
        return allow
    }

    companion object {
        const val ALLOWING_PERIOD = 100L
        val NO_SKIP_TOKENS: ReduceSkipTokens = ReduceSkipTokens(emptyList())
    }
}