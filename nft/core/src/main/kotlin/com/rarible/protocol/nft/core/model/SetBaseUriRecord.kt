package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.framework.model.LogRecord
import com.rarible.blockchain.scanner.framework.model.TransactionRecord
import scalether.domain.Address

data class SetBaseUriRecord(
    val hash: String,
    val address: Address,
) : TransactionRecord, LogRecord {
    override fun getBlock(): Long? = null

    override fun getKey(): String = hash
}
