package com.rarible.protocol.nft.core.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.PendingLogItemProperties
import org.springframework.data.repository.reactive.ReactiveCrudRepository

@CaptureSpan(type = "db", subtype = "pending-log-item-properties")
interface PendingLogItemPropertiesRepository: ReactiveCrudRepository<PendingLogItemProperties, String>
