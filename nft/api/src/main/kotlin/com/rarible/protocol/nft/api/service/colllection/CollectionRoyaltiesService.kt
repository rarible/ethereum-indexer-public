package com.rarible.protocol.nft.api.service.colllection

import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.RoyaltiesEventType
import com.rarible.protocol.nft.core.model.SetRoyaltiesForContract
import com.rarible.protocol.nft.core.repository.history.RoyaltiesHistoryRepository
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class CollectionRoyaltiesService(
    private val royaltiesHistoryRepository: RoyaltiesHistoryRepository
) {
    suspend fun getRoyaltiesHistory(token: Address): List<Part>? {
        val history = royaltiesHistoryRepository.findLastByCollection(token, RoyaltiesEventType.SET_CONTRACT_ROYALTIES)
        return (history?.data as? SetRoyaltiesForContract)?.parts
    }
}
