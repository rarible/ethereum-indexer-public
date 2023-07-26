package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.framework.model.TransactionRecord
import scalether.domain.Address

data class SetBaseUriRecord(
    val hash: String,
    val address: Address,
) : TransactionRecord {
    override fun getKey(): String = hash
}
