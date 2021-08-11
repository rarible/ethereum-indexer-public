package com.rarible.protocol.client.exception

class ProtocolApiResponseException(
    message: String?,
    cause: Throwable?,
    val responseObject: Any
) : RuntimeException(message, cause)
