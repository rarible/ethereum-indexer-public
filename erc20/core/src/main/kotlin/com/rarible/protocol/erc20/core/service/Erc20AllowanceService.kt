package com.rarible.protocol.erc20.core.service

import com.rarible.contracts.erc20.IERC20
import com.rarible.core.common.optimisticLock
import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.core.configuration.Erc20IndexerProperties
import com.rarible.protocol.erc20.core.event.Erc20BalanceEventListener
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Allowance
import com.rarible.protocol.erc20.core.model.Erc20AllowanceEvent
import com.rarible.protocol.erc20.core.model.Erc20MarkedEvent
import com.rarible.protocol.erc20.core.repository.Erc20AllowanceRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger
import java.time.Instant

@Service
class Erc20AllowanceService(
    private val sender: MonoTransactionSender,
    private val erc20AllowanceRepository: Erc20AllowanceRepository,
    private val erc20BalanceEventListeners: List<Erc20BalanceEventListener>,
    erc20IndexerProperties: Erc20IndexerProperties,
) : EntityService<BalanceId, Erc20Allowance, Erc20MarkedEvent> {
    private val transferProxyAddress = erc20IndexerProperties.erc20TransferProxy

    override suspend fun get(id: BalanceId): Erc20Allowance? = erc20AllowanceRepository.get(id)

    override suspend fun update(entity: Erc20Allowance, event: Erc20MarkedEvent?): Erc20Allowance {
        val result = erc20AllowanceRepository.save(entity)
        erc20BalanceEventListeners.forEach {
            it.onUpdate(Erc20AllowanceEvent(event?.event, event?.eventTimeMarks, entity))
        }
        return result
    }

    suspend fun getBlockchainAllowance(id: BalanceId): EthUInt256? {
        return IERC20(id.token, sender).allowance(id.owner, transferProxyAddress).awaitSingleOrNull()
            ?.let { EthUInt256(it) }
    }

    suspend fun onChainUpdate(balanceId: BalanceId, event: Erc20MarkedEvent?) {
        optimisticLock {
            val erc20Allowance = get(balanceId) ?: Erc20Allowance(
                token = balanceId.token,
                owner = balanceId.owner,
                allowance = EthUInt256(BigInteger.ZERO),
                createdAt = Instant.now(),
                lastUpdatedAt = Instant.now(),
            )
            val allowance = getBlockchainAllowance(balanceId) ?: run {
                logger.error("Can't get allowance $balanceId from blockchain")
                throw IllegalStateException("Can't get allowance $balanceId from blockchain")
            }
            update(entity = erc20Allowance.withAllowance(allowance), event = event)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Erc20AllowanceService::class.java)
    }
}