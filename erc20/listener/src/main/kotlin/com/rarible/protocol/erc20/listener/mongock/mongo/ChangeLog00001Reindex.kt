package com.rarible.protocol.erc20.listener.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.core.common.subscribeAndLog
import com.rarible.protocol.erc20.listener.service.balance.Erc20BalanceReduceService
import io.changock.migration.api.annotations.NonLockGuarded
import org.slf4j.LoggerFactory

@ChangeLog(order = "00001")
class ChangeLog00001Reindex {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @ChangeSet(id = "ChangeLog00001Reindex.reindex001", order = "1", author = "protocol")
    fun createHistoryIndexes(@NonLockGuarded balanceReduceService: Erc20BalanceReduceService) {
        balanceReduceService.update(null, Long.MIN_VALUE).then().subscribeAndLog("reindex001", logger)
    }
}