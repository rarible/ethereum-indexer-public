package com.rarible.protocol.nftorder.api.test.mock

import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.unlockable.api.client.LockControllerApi
import io.mockk.every
import reactor.core.publisher.Mono

class LockControllerApiMock(
    private val lockControllerApi: LockControllerApi
) {

    fun mockIsUnlockable(itemId: ItemId, returnIsUnlockable: Boolean) {
        every {
            lockControllerApi.isUnlockable(itemId.decimalStringValue)
        } returns Mono.just(returnIsUnlockable)
    }

}