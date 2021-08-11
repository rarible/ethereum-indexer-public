package com.rarible.protocol.nftorder.core.service

import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.unlockable.api.client.LockControllerApi
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import org.springframework.stereotype.Component

@Component
class LockService(
    private val lockControllerApi: LockControllerApi
) {

    suspend fun isUnlockable(itemId: ItemId): Boolean {
        return lockControllerApi
            .isUnlockable(itemId.decimalStringValue)
            .awaitFirstOrDefault(false)
    }

}