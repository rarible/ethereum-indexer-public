package com.rarible.protocol.order.core.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.order.core.model.CollectionOrderStat
import com.rarible.protocol.order.core.repository.CollectionOrderStatRepository
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class CollectionOrderStatService(
    private val collectionStatRepository: CollectionOrderStatRepository,
    private val exchangeHistoryRepository: ExchangeHistoryRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getOrSchedule(token: Address): CollectionOrderStat = optimisticLock {
        val stat = collectionStatRepository.get(token)
        stat ?: collectionStatRepository.save(CollectionOrderStat.empty(token))
    }

    suspend fun updateStat(token: Address): CollectionOrderStat {        // TODO add

    }

    private fun query(token: Address) {
        exchangeHistoryRepository.searchActivity()
    }

}