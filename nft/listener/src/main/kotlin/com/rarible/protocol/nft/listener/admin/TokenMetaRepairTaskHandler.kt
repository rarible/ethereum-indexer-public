package com.rarible.protocol.nft.listener.admin

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.nft.core.model.FeatureFlags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TokenMetaRepairTaskHandler(
    private val ff: FeatureFlags
) : TaskHandler<String> {

    override val type = "TOKEN_META_REPAIR"

    override suspend fun isAbleToRun(param: String): Boolean {
        return ff.enableTokenMetaSelfRepair
    }

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(""))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> = flow {
        logger.error("TokenMetaRepairTaskHandler shouldn't be executed")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TokenMetaRepairTaskHandler::class.java)
    }
}