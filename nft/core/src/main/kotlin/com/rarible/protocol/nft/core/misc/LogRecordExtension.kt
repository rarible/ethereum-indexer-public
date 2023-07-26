package com.rarible.protocol.nft.core.misc

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.model.LogRecord

fun LogRecord.asEthereumLogRecord() = this as ReversedEthereumLogRecord
