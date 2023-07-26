package com.rarible.protocol.nft.listener.service.suspicios

import com.rarible.protocol.nft.core.model.UpdateSuspiciousItemsState
import com.rarible.protocol.nft.core.repository.JobStateRepository
import com.rarible.protocol.nft.listener.service.resolver.BluechipTokenResolver
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class UpdateSuspiciousItemsStateService(
    private val bluechipTokenResolver: BluechipTokenResolver,
    private val stateRepository: JobStateRepository
) {
    suspend fun save(state: UpdateSuspiciousItemsState) {
        stateRepository.save(UpdateSuspiciousItemsState.STATE_ID, state.withLastUpdatedAt())
    }

    suspend fun getState(): UpdateSuspiciousItemsState? {
        return stateRepository.get(
            UpdateSuspiciousItemsState.STATE_ID,
            UpdateSuspiciousItemsState::class.java
        )
    }

    fun getInitState(statedAt: Instant): UpdateSuspiciousItemsState {
        val contracts = bluechipTokenResolver.resolve()
        return UpdateSuspiciousItemsState(
            statedAt = statedAt,
            assets = contracts.map { UpdateSuspiciousItemsState.Asset(it) }
        )
    }
}
