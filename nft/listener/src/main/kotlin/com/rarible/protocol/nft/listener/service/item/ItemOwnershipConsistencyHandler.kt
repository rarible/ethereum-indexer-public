package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.daemon.job.JobHandler
import org.springframework.stereotype.Component

@Component
class ItemOwnershipConsistencyHandler : JobHandler {

    override suspend fun handle() {
        try {
            // get state

            // while continuation is less than (now - 5 minutes)
                // get batch of items
                // for each item
                    // get all ownerships
                    // if ownerships amount doesn't match with item.supply
                        // run reduce on item
                        // check consistency again
                        // if still not consistent
                            // save item to inconsistent_item repo
                // save state
        } finally {
            // save state
        }
    }
}