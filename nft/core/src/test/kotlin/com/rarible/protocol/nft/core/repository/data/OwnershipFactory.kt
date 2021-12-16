package com.rarible.protocol.nft.core.repository.data

import com.rarible.protocol.nft.core.data.createRandomOwnership
import com.rarible.protocol.nft.core.model.Ownership

@Deprecated("", ReplaceWith("createRandomOwnership()", "com.rarible.protocol.nft.core.data.createRandomOwnership"))
fun createOwnership(): Ownership = createRandomOwnership()
