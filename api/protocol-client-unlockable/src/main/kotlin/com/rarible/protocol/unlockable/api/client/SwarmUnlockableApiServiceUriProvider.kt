package com.rarible.protocol.unlockable.api.client

import java.net.URI

class SwarmUnlockableApiServiceUriProvider(
    private val environment: String
) : UnlockableApiServiceUriProvider {

    override fun getUri(blockchain: String): URI {
        return URI.create(String.format("http://%s-%s-unlockable-api:8080", environment, blockchain))
    }

}
