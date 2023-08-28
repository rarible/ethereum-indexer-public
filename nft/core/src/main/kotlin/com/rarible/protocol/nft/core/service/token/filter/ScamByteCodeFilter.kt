package com.rarible.protocol.nft.core.service.token.filter

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.FeatureFlags
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component

@Component
class ScamByteCodeFilter(
    private val featureFlags: FeatureFlags,
    scamByteCodeProperties: NftIndexerProperties.ScamByteCodeProperties
) : TokeByteCodeFilter {
    private val markers = scamByteCodeProperties.markers

    override fun isValid(code: Binary, hash: Word): Boolean {
        if (
            featureFlags.filterScamToken.not() ||
            markers.isEmpty() ||
            code.bytes().isEmpty()
        ) return true

        for (marker in markers) {
            if (marker.payloads.all { it.matchFragment(code) }) return false
        }
        return true
    }
}
