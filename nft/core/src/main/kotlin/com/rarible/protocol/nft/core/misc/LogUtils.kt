package com.rarible.protocol.nft.core.misc

import com.rarible.core.logging.RaribleMDCContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import org.slf4j.MDC

//TODO: Put in some common place for other indexers
object LogUtils {
    suspend fun <T> addToMdc(
        vararg values: Pair<String, String>,
        block: suspend CoroutineScope.() -> T
    ): T {
        return addToMdc(values.toList(), block)
    }

    private suspend fun <T> addToMdc(
        values: List<Pair<String, String>>,
        block: suspend CoroutineScope.() -> T
    ): T {
        val current = MDC.getCopyOfContextMap()
        val newValues = values.associateBy({ it.first }, { it.second })

        val resultMap = current?.let { newValues + it } ?: newValues

        return withContext(RaribleMDCContext(resultMap), block)
    }
}
