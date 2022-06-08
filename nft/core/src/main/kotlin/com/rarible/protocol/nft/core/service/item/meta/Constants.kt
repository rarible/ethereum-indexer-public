package com.rarible.protocol.nft.core.service.item.meta

const val ITEM_META_CAPTURE_SPAN_TYPE = "item_meta"
const val TOKEN_META_CAPTURE_SPAN_TYPE = "token_meta"
const val IPFS_CAPTURE_SPAN_TYPE = "ipfs"

fun base64MimeToBytes(data: String): ByteArray = java.util.Base64.getMimeDecoder().decode(data.toByteArray()) // TODO Change
