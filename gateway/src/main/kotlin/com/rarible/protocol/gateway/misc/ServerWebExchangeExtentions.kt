package com.rarible.protocol.gateway.misc

import org.springframework.http.server.reactive.ServerHttpRequest

fun ServerHttpRequest.setHeader(headerName: String, headerValue: String?): ServerHttpRequest {
    return this.mutate().header(headerName, headerValue).build()
}