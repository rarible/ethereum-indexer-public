package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component

@Component
class OrderVersionCleanSignatureService(
    private val orderVersionRepository: OrderVersionRepository,
) {
    suspend fun cleanSignature(id: Word) {
        val versions = orderVersionRepository.findAllByHash(id).toList()
        versions
            .filter { it.signature != null }
            .forEach { orderVersionRepository.save(it.copy(signature = null)).awaitSingle() }
    }
}
