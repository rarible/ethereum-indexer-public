package com.rarible.protocol.nft.core.service.item.meta

fun base64MimeToBytes(data: String): ByteArray = java.util.Base64.getMimeDecoder().decode(data.toByteArray()) // TODO Change
