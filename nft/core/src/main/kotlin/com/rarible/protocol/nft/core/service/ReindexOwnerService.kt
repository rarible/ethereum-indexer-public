package com.rarible.protocol.nft.core.service

import com.rarible.core.task.Task
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ReindexOwnerService(
    private val taskSchedulingService: TaskSchedulingService,
) {

    suspend fun createReduceOwnerItemsTask(owner: Address, force: Boolean): Task =
        taskSchedulingService.saveTask(
            param = owner.toString(),
            type = ADMIN_REDUCE_OWNER_ITEMS,
            state = null,
            force = force,
        )

    companion object {
        const val ADMIN_REDUCE_OWNER_ITEMS = "ADMIN_REDUCE_OWNER_ITEMS"
    }
}
