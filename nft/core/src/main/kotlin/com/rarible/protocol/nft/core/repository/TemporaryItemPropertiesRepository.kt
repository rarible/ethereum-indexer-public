package com.rarible.protocol.nft.core.repository

import com.rarible.protocol.nft.core.model.TemporaryItemProperties
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface TemporaryItemPropertiesRepository: ReactiveCrudRepository<TemporaryItemProperties, String>
