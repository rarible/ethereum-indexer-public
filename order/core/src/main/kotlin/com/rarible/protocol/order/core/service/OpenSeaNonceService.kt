package com.rarible.protocol.order.core.service

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.model.MakerNonce
import com.rarible.protocol.order.core.repository.nonce.NonceHistoryRepository
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.time.Instant

@Component
class OpenSeaNonceService(
    private val nonceHistoryRepository: NonceHistoryRepository
) {
    suspend fun getLatestMakerNonce(maker: Address): MakerNonce {
        return nonceHistoryRepository.findLatestNonceHistoryByMaker(maker)
            ?.let { logEvent ->
                val data = logEvent.data as ChangeNonceHistory
                MakerNonce(
                    historyId = logEvent.id.toHexString(),
                    nonce = data.newNonce,
                    timestamp = data.date)
            }
            ?: DEFAULT_MAKER_NONCE
    }

    companion object {
        private val DEFAULT_MAKER_NONCE: MakerNonce = MakerNonce(
            historyId = "",
            nonce = EthUInt256.ZERO,
            timestamp = Instant.EPOCH
        )
    }
}