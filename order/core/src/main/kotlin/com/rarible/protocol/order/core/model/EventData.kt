package com.rarible.protocol.order.core.model

import com.rarible.ethereum.listener.log.domain.EventData as LegacyEventData
import com.rarible.blockchain.scanner.ethereum.model.EventData as ScannerEventData

interface EventData : ScannerEventData, LegacyEventData
