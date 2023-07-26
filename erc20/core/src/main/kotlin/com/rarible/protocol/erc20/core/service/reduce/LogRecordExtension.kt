package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.model.LogRecord

fun LogRecord.asEthereumLogRecord() = this as ReversedEthereumLogRecord
