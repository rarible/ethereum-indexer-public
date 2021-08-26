package com.rarible.protocol.nftorder.core.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.util.spent
import com.rarible.protocol.unlockable.api.client.LockControllerApi
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LockService(
    private val lockControllerApi: LockControllerApi
) {

    private val logger = LoggerFactory.getLogger(LockService::class.java)

    suspend fun isUnlockable(itemId: ItemId): Boolean {
        val now = nowMillis()
        val result = lockControllerApi
            .isUnlockable(itemId.decimalStringValue)
            .awaitFirstOrDefault(false)
        logger.info("Fetched Unlockable marker for Item [{}]: [{}] ({}ms)", itemId, result, spent(now))
        return result
    }

}