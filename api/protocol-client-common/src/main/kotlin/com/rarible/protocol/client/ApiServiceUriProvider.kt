package com.rarible.protocol.client

import java.net.URI

interface ApiServiceUriProvider {
    fun getUri(blockchain: String): URI
}
