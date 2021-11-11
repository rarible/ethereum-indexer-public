package com.rarible.protocol.nft.core.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.TemporaryItemProperties
import org.springframework.data.repository.reactive.ReactiveCrudRepository

@CaptureSpan(type = "db", subtype = "temporary-item-properties")
interface TemporaryItemPropertiesRepository: ReactiveCrudRepository<TemporaryItemProperties, String>
