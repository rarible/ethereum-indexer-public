package com.rarible.protocol.unlockable.api.client

import java.net.URI

class FixedUnlockableApiServiceUriProvider(private val fixedURI: URI) : UnlockableApiServiceUriProvider {

    override fun getUri(blockchain: String): URI {
        return fixedURI
    }

}
