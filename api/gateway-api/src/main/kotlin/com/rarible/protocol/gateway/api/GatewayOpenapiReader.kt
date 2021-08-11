package com.rarible.protocol.gateway.api

import java.io.InputStream

object GatewayOpenapiReader {

    fun getOpenapi(): InputStream {
        return GatewayOpenapiReader::class.java.getResourceAsStream("/openapi.yaml")
    }

}