package com.rarible.protocol.nft.core.service.token.filter

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.FeatureFlags
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.util.Hash

@Component
class ScamByteCodeHashFilter(
    private val featureFlags: FeatureFlags,
    scamByteCodeProperties: NftIndexerProperties.ScamByteCodeProperties
) : TokeByteCodeFilter {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val markers = scamByteCodeProperties.hashCodes.map { Word.apply(it) }

    override fun isValid(code: Binary): Boolean {
        val hash = Word.apply(Hash.sha3(code.bytes()))
        if (
            featureFlags.filterScamToken.not() ||
            markers.isEmpty() ||
            code.bytes().isEmpty()
        ) return true

        val scam = markers.contains(Word.apply(hash))
        if (scam) {
            logger.warn("Found scam collection by hash: $hash")
        }
        return !scam
    }
}
