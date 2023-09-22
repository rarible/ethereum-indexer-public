package com.rarible.protocol.nft.core.service.token.filter

import com.rarible.protocol.nft.core.model.FeatureFlags
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ScamByteCodeHashFilter(
    private val featureFlags: FeatureFlags,
    private val scamByteCodeHashCache: ScamByteCodeHashCache
) : TokeByteCodeFilter {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun isValid(code: Binary, hash: Word): Boolean {
        if (
            featureFlags.filterScamToken.not() ||
            scamByteCodeHashCache.markers().isEmpty() ||
            code.bytes().isEmpty()
        ) return true

        val scam = scamByteCodeHashCache.markers().contains(Word.apply(hash))
        if (scam) {
            logger.warn("Found scam collection by hash: $hash")
        }
        return !scam
    }
}
