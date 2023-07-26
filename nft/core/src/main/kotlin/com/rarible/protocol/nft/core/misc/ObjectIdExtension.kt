package com.rarible.protocol.nft.core.misc

import org.bson.types.ObjectId

fun String.safeQueryParam(): Any = if (ObjectId.isValid(this)) ObjectId(this) else this
