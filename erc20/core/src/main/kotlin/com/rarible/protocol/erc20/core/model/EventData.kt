package com.rarible.protocol.erc20.core.model

import com.rarible.blockchain.scanner.ethereum.model.EventData as ScannerEventData
import com.rarible.ethereum.listener.log.domain.EventData as LegacyEventData

interface EventData : ScannerEventData, LegacyEventData
